// Regenerates the raster images of the site from the HTML sources in this folder:
//   assets/og-fr.png, assets/og-en.png (1200x630, Open Graph / Twitter cards)
//   assets/icon-180.png               (180x180, apple-touch-icon and PNG favicon)
//
// Usage (from the repo root): node website/tools/render-images.mjs
// Needs Node 22+ (built-in WebSocket) and a local Chrome or Chromium. No npm dependency.
// Set CHROME_PATH if Chrome is not found automatically.

import { spawn } from "node:child_process";
import { existsSync, mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const assets = resolve(here, "..", "assets");

const jobs = [
  { src: "og.html?lang=fr", out: "og-fr.png", width: 1200, height: 630 },
  { src: "og.html?lang=en", out: "og-en.png", width: 1200, height: 630 },
  { src: "icon.html", out: "icon-180.png", width: 180, height: 180 },
];

function findChrome() {
  const candidates = [
    process.env.CHROME_PATH,
    "C:/Program Files/Google/Chrome/Application/chrome.exe",
    "C:/Program Files (x86)/Google/Chrome/Application/chrome.exe",
    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
    "/usr/bin/google-chrome",
    "/usr/bin/chromium",
    "/usr/bin/chromium-browser",
  ].filter(Boolean);
  const found = candidates.find((p) => existsSync(p));
  if (!found) throw new Error("Chrome not found. Set CHROME_PATH to your Chrome or Chromium binary.");
  return found;
}

function launch(chromePath, profileDir) {
  return new Promise((resolveLaunch, reject) => {
    const proc = spawn(chromePath, [
      "--headless=new",
      "--remote-debugging-port=0",
      `--user-data-dir=${profileDir}`,
      "--hide-scrollbars",
      "--no-first-run",
      "--no-default-browser-check",
      "about:blank",
    ]);
    let buffer = "";
    proc.stderr.on("data", (chunk) => {
      buffer += chunk.toString();
      const match = buffer.match(/DevTools listening on (ws:\/\/\S+)/);
      if (match) resolveLaunch({ proc, wsUrl: match[1] });
    });
    proc.on("error", reject);
    proc.on("exit", (code) => reject(new Error(`Chrome exited early (code ${code})`)));
  });
}

function connect(wsUrl) {
  return new Promise((resolveConnect, reject) => {
    const ws = new WebSocket(wsUrl);
    let nextId = 0;
    const pending = new Map();
    const listeners = [];
    ws.onmessage = (event) => {
      const msg = JSON.parse(event.data);
      if (msg.id !== undefined && pending.has(msg.id)) {
        const { resolveCall, rejectCall } = pending.get(msg.id);
        pending.delete(msg.id);
        if (msg.error) rejectCall(new Error(msg.error.message));
        else resolveCall(msg.result);
      } else if (msg.method) {
        listeners.forEach((fn) => fn(msg));
      }
    };
    ws.onerror = reject;
    ws.onopen = () =>
      resolveConnect({
        send(method, params = {}, sessionId) {
          const id = ++nextId;
          ws.send(JSON.stringify({ id, method, params, sessionId }));
          return new Promise((resolveCall, rejectCall) => pending.set(id, { resolveCall, rejectCall }));
        },
        waitFor(method, sessionId) {
          return new Promise((resolveWait) => {
            const fn = (msg) => {
              if (msg.method === method && msg.sessionId === sessionId) {
                listeners.splice(listeners.indexOf(fn), 1);
                resolveWait(msg.params);
              }
            };
            listeners.push(fn);
          });
        },
        close: () => ws.close(),
      });
  });
}

async function render(cdp, job) {
  const { targetId } = await cdp.send("Target.createTarget", { url: "about:blank" });
  const { sessionId } = await cdp.send("Target.attachToTarget", { targetId, flatten: true });
  await cdp.send("Page.enable", {}, sessionId);
  await cdp.send(
    "Emulation.setDeviceMetricsOverride",
    { width: job.width, height: job.height, deviceScaleFactor: 1, mobile: false },
    sessionId,
  );
  const loaded = cdp.waitFor("Page.loadEventFired", sessionId);
  const [file, query = ""] = job.src.split("?");
  const url = pathToFileURL(join(here, file)).href + (query ? `?${query}` : "");
  await cdp.send("Page.navigate", { url }, sessionId);
  await loaded;
  await new Promise((r) => setTimeout(r, 300));
  const { data } = await cdp.send(
    "Page.captureScreenshot",
    { format: "png", clip: { x: 0, y: 0, width: job.width, height: job.height, scale: 1 } },
    sessionId,
  );
  writeFileSync(join(assets, job.out), Buffer.from(data, "base64"));
  await cdp.send("Target.closeTarget", { targetId });
  console.log(`wrote assets/${job.out} (${job.width}x${job.height})`);
}

const profileDir = mkdtempSync(join(tmpdir(), "custos-render-"));
const { proc, wsUrl } = await launch(findChrome(), profileDir);
try {
  const cdp = await connect(wsUrl);
  for (const job of jobs) await render(cdp, job);
  cdp.close();
} finally {
  proc.removeAllListeners("exit");
  proc.kill();
  setTimeout(() => rmSync(profileDir, { recursive: true, force: true }), 500);
}
