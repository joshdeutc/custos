"use client";

import React, { useEffect, useMemo, useRef, useState } from "react";
import { motion } from "framer-motion";
import {
  ArrowLeft,
  BatteryCharging,
  CalendarClock,
  Clock,
  Flame,
  Lock,
  MoonStar,
  Scissors,
  Wifi,
} from "lucide-react";

type NodeId = "delay" | "limits" | "curfew" | "nuclear" | "partial" | "schedule";

interface NodeConfig {
  id: NodeId;
  title: string;
  subtitle: string;
  angleDeg: number;
  isHub?: boolean;
  summaryColorClass: string;
  detailHeadline: string;
  detailSub: string;
  ctaLabel: string;
  ctaStyle: "black" | "red";
  rows: { title: string; meta: string; status: string }[];
}

interface PositionedNode extends NodeConfig {
  cx: number;
  cy: number;
  size: number;
}

const EASE_CURVE = [0.22, 1, 0.36, 1] as const;

const NODE_ICON_COLORS: Record<NodeId, string> = {
  delay: "text-indigo-600 dark:text-indigo-400",
  limits: "text-indigo-600 dark:text-indigo-400",
  curfew: "text-violet-600 dark:text-violet-400",
  nuclear: "text-rose-700 dark:text-rose-400",
  partial: "text-sky-700 dark:text-sky-400",
  schedule: "text-emerald-700 dark:text-emerald-400",
};

function NodeIcon({
  id,
  className,
}: {
  id: NodeId;
  className: string;
}) {
  const iconClassName = `shrink-0 ${NODE_ICON_COLORS[id]} ${className}`;
  switch (id) {
    case "delay":
      return <Lock className={iconClassName} aria-hidden="true" />;
    case "limits":
      return <Clock className={iconClassName} aria-hidden="true" />;
    case "curfew":
      return <MoonStar className={iconClassName} aria-hidden="true" />;
    case "nuclear":
      return <Flame className={iconClassName} aria-hidden="true" />;
    case "partial":
      return <Scissors className={iconClassName} aria-hidden="true" />;
    case "schedule":
      return <CalendarClock className={iconClassName} aria-hidden="true" />;
  }
}

const NODE_CONFIGS: NodeConfig[] = [
  {
    id: "delay",
    title: "Delay & Limits",
    subtitle: "Delay active",
    angleDeg: 0,
    isHub: true,
    summaryColorClass: "text-emerald-700 dark:text-emerald-400",
    detailHeadline: "Security & Delay",
    detailSub:
      "Tightening a rule takes effect immediately. Relaxing it requires waiting for your chosen delay.",
    ctaLabel: "Global delay: 12h 00",
    ctaStyle: "black",
    rows: [
      { title: "Tightening a limit", meta: "Applied directly", status: "Instant" },
      { title: "Relaxing / Deleting", meta: "Cooling-off lock active", status: "11h 42m" },
      { title: "Settings Protection", meta: "Anti-quick-disable", status: "Active" },
    ],
  },
  {
    id: "limits",
    title: "App Limits",
    subtitle: "Quotas active",
    angleDeg: 270,
    summaryColorClass: "text-slate-600 dark:text-zinc-400",
    detailHeadline: "App Limits",
    detailSub:
      "Daily foreground budget (resets at 2:00 AM) + per-session limits.",
    ctaLabel: "+ Add an application",
    ctaStyle: "black",
    rows: [
      { title: "Instagram", meta: "Quota: 30 min / day", status: "24m / 30m" },
      { title: "TikTok", meta: "Daily quota reached", status: "Blocked" },
      { title: "Discord", meta: "5m session · 4h cooldown", status: "2 / 3 sess." },
    ],
  },
  {
    id: "curfew",
    title: "Curfew",
    subtitle: "22:00 → 07:00",
    angleDeg: 342,
    summaryColorClass: "text-indigo-600 dark:text-indigo-400",
    detailHeadline: "Curfew",
    detailSub:
      "Strict blocking during your sleep or study hours, with notifications muted.",
    ctaLabel: "+ Add a curfew",
    ctaStyle: "black",
    rows: [
      { title: "Social Media · Night", meta: "Mon–Sun · Notifs muted", status: "22:00 → 07:00" },
      { title: "Deep Study · Morning", meta: "Mon–Fri · YouTube, Reddit", status: "09:00 → 12:00" },
    ],
  },
  {
    id: "nuclear",
    title: "Nuclear",
    subtitle: "Total block",
    angleDeg: 54,
    summaryColorClass: "text-rose-700 dark:text-rose-400",
    detailHeadline: "Nuclear Mode",
    detailSub:
      "Firm, irreversible lockdown for a fixed duration with named presets and Do Not Disturb.",
    ctaLabel: "Activate Nuclear Mode",
    ctaStyle: "red",
    rows: [
      { title: "Preset \"Deep Work\"", meta: "6 apps + Do Not Disturb", status: "2h 00" },
      { title: "Preset \"Exams\"", meta: "All social networks", status: "4h 00" },
      { title: "Early cancellation", meta: "Impossible before timer ends", status: "Locked" },
    ],
  },
  {
    id: "partial",
    title: "Partial Access",
    subtitle: "In-app",
    angleDeg: 126,
    summaryColorClass: "text-sky-700 dark:text-sky-400",
    detailHeadline: "Partial Access",
    detailSub:
      "Instantly redirects away from addictive tabs without closing the useful app.",
    ctaLabel: "+ Learn a screen",
    ctaStyle: "black",
    rows: [
      { title: "YouTube Shorts", meta: "Redirect → YouTube Home", status: "Blocked" },
      { title: "WhatsApp · Channels", meta: "Chats & calls kept", status: "Blocked" },
    ],
  },
  {
    id: "schedule",
    title: "Schedule",
    subtitle: "Days & hrs",
    angleDeg: 198,
    summaryColorClass: "text-slate-600 dark:text-zinc-400",
    detailHeadline: "Allowed Days & Hours",
    detailSub:
      "Allow specific apps only on selected days or time windows.",
    ctaLabel: "Configure schedules",
    ctaStyle: "black",
    rows: [
      { title: "Games & Streaming", meta: "Fri 18:00 → Sun 22:00 only", status: "Restricted" },
      { title: "Browsers", meta: "Daytime hours only", status: "08:00 → 22:00" },
    ],
  },
];

function computeEdgeLine(
  satCx: number,
  satCy: number,
  hubCx: number,
  hubCy: number,
  hubR: number,
  satR: number
) {
  const dx = satCx - hubCx;
  const dy = satCy - hubCy;
  const dist = Math.hypot(dx, dy);
  if (dist === 0) {
    return { x1: hubCx, y1: hubCy, x2: satCx, y2: satCy };
  }
  const ux = dx / dist;
  const uy = dy / dist;
  return {
    x1: hubCx + ux * hubR,
    y1: hubCy + uy * hubR,
    x2: satCx - ux * satR,
    y2: satCy - uy * satR,
  };
}

export function AndroidPhoneMockup() {
  const [zoomedId, setZoomedId] = useState<NodeId | null>(null);
  const stageRef = useRef<HTMLDivElement | null>(null);
  const [stageSize, setStageSize] = useState<{ width: number; height: number }>({
    width: 300,
    height: 430,
  });

  useEffect(() => {
    const el = stageRef.current;
    if (!el) return;

    const updateSize = () => {
      const width = Math.round(el.clientWidth);
      const height = Math.round(el.clientHeight);
      if (width > 0 && height > 0) {
        setStageSize((prev) =>
          prev.width === width && prev.height === height
            ? prev
            : { width, height }
        );
      }
    };

    updateSize();

    if (typeof ResizeObserver !== "undefined") {
      const observer = new ResizeObserver(updateSize);
      observer.observe(el);
      return () => observer.disconnect();
    }

    window.addEventListener("resize", updateSize);
    return () => window.removeEventListener("resize", updateSize);
  }, []);

  const geometry = useMemo(() => {
    const stageW = stageSize.width;
    const stageH = stageSize.height;
    const hubCx = stageW / 2;
    const hubCy = stageH / 2 + 6;
    const radius = Math.min(stageW, stageH) * 0.342;
    const hubSize = Math.round((stageW * 0.345) / 2) * 2;
    const satSize = Math.round((stageW * 0.262) / 2) * 2;
    const hubR = hubSize / 2;
    const satR = satSize / 2;

    const nodes: PositionedNode[] = NODE_CONFIGS.map((cfg) => {
      if (cfg.isHub) {
        return { ...cfg, cx: hubCx, cy: hubCy, size: hubSize };
      }
      const rad = (cfg.angleDeg * Math.PI) / 180;
      return {
        ...cfg,
        cx: hubCx + radius * Math.cos(rad),
        cy: hubCy + radius * Math.sin(rad),
        size: satSize,
      };
    });

    return {
      stageW,
      stageH,
      hubCx,
      hubCy,
      hubR,
      satR,
      nodes,
      satellites: nodes.filter((n) => !n.isHub),
    };
  }, [stageSize.width, stageSize.height]);

  return (
    <div className="relative mx-auto w-full max-w-[330px] select-none">
      {/* Subtle Ambient Glow */}
      <div
        aria-hidden="true"
        className="pointer-events-none absolute -inset-6 rounded-[3rem] bg-gradient-to-tr from-indigo-500/20 via-violet-500/15 to-sky-500/10 blur-2xl dark:from-indigo-500/25 dark:via-purple-500/20 dark:to-transparent"
      />

      {/* Android Smartphone Shell */}
      <div className="relative overflow-hidden rounded-[2.6rem] border-[7px] border-slate-900 bg-white shadow-2xl dark:border-zinc-800 dark:bg-[#111116]">
        {/* Top Android Camera Punch-Hole + Status Bar */}
        <div className="relative z-10 flex items-center justify-between bg-white px-6 pb-1.5 pt-3 text-[11px] font-medium text-slate-600 dark:bg-[#111116] dark:text-zinc-400">
          <span className="font-mono">09:41</span>
          <div
            aria-hidden="true"
            className="h-3.5 w-3.5 rounded-full bg-slate-900 ring-2 ring-slate-800/30 dark:bg-black dark:ring-zinc-700/60"
          />
          <div className="flex items-center gap-1.5">
            <Wifi className="h-3 w-3" />
            <BatteryCharging className="h-3.5 w-3.5" />
          </div>
        </div>

        {/* Zoomable Canvas Stage (1:1 responsive mirror of ZoomableCanvasView) */}
        <div
          ref={stageRef}
          className="relative h-[430px] w-full overflow-hidden bg-white sm:h-[448px] dark:bg-[#111116]"
        >
          {/* Subtle hint pill when overview is visible */}
          <motion.div
            animate={{ opacity: zoomedId ? 0 : 1 }}
            transition={{ duration: 0.2 }}
            className="pointer-events-none absolute inset-x-0 top-2.5 z-20 flex justify-center px-2"
          >
            <span className="inline-flex items-center gap-1.5 rounded-full border border-slate-200 bg-slate-50 px-2.5 py-0.5 text-[10px] font-medium text-slate-600 dark:border-zinc-800 dark:bg-zinc-900 dark:text-zinc-300">
              <Lock className="h-2.5 w-2.5 shrink-0 text-emerald-700 dark:text-emerald-400" />
              <span>Custos · Tap a circle to zoom</span>
            </span>
          </motion.div>

          {/* SVG Connector Lines + Endpoint Dots (fade out on zoomInto) */}
          <motion.svg
            viewBox={`0 0 ${geometry.stageW} ${geometry.stageH}`}
            className="pointer-events-none absolute inset-0 h-full w-full"
            animate={{ opacity: zoomedId ? 0 : 1 }}
            transition={{ duration: 0.22 }}
            aria-hidden="true"
          >
            {geometry.satellites.map((sat) => {
              const line = computeEdgeLine(
                sat.cx,
                sat.cy,
                geometry.hubCx,
                geometry.hubCy,
                geometry.hubR,
                geometry.satR
              );
              return (
                <g key={sat.id}>
                  <line
                    x1={line.x1}
                    y1={line.y1}
                    x2={line.x2}
                    y2={line.y2}
                    className="stroke-slate-300 dark:stroke-zinc-700"
                    strokeWidth="1.5"
                    strokeLinecap="round"
                  />
                  <circle
                    cx={line.x1}
                    cy={line.y1}
                    r="2.4"
                    className="fill-slate-400 dark:fill-zinc-500"
                  />
                  <circle
                    cx={line.x2}
                    cy={line.y2}
                    r="2.4"
                    className="fill-slate-400 dark:fill-zinc-500"
                  />
                </g>
              );
            })}
          </motion.svg>

          {/* Single-Element Morphing Feature Circles (morphProgress: 0 -> 1) */}
          {geometry.nodes.map((node) => {
            const isZoomed = zoomedId === node.id;
            const isOtherZoomed = zoomedId !== null && !isZoomed;
            const baseLeft = node.cx - node.size / 2;
            const baseTop = node.cy - node.size / 2;

            return (
              <motion.div
                key={node.id}
                initial={false}
                animate={{
                  left: isZoomed ? 8 : baseLeft,
                  top: isZoomed ? 8 : baseTop,
                  width: isZoomed ? Math.max(geometry.stageW - 16, 220) : node.size,
                  height: isZoomed ? Math.max(geometry.stageH - 16, 360) : node.size,
                  borderRadius: isZoomed ? 24 : 999,
                  opacity: isOtherZoomed ? 0 : 1,
                  scale: isOtherZoomed ? 0.92 : 1,
                }}
                transition={{
                  duration: 0.44,
                  ease: EASE_CURVE,
                }}
                style={{
                  zIndex: isZoomed ? 30 : 10,
                  pointerEvents: isOtherZoomed ? "none" : "auto",
                }}
                aria-hidden={isOtherZoomed}
                className="absolute overflow-hidden border-[1.75px] border-slate-900 bg-white shadow-sm dark:border-zinc-300 dark:bg-[#14141A]"
              >
                {!isZoomed ? (
                  <button
                    type="button"
                    onClick={() => setZoomedId(node.id)}
                    tabIndex={isOtherZoomed ? -1 : 0}
                    aria-label={`Open ${node.title}`}
                    className="flex h-full w-full flex-col items-center justify-center gap-[1px] px-2 text-center transition-colors hover:bg-slate-50/70 sm:gap-0.5 dark:hover:bg-zinc-900/50"
                  >
                    <NodeIcon
                      id={node.id}
                      className={
                        node.isHub
                          ? "h-3.5 w-3.5 sm:h-[18px] sm:w-[18px]"
                          : "h-3 w-3 sm:h-4 sm:w-4"
                      }
                    />
                    <span
                      className={`font-bold leading-none text-slate-950 sm:leading-tight dark:text-white ${
                        node.isHub ? "text-[10px] sm:text-xs" : "text-[9px] sm:text-[10px]"
                      }`}
                    >
                      {node.title}
                    </span>
                    <span
                      className={`font-medium leading-none sm:leading-tight ${
                        node.isHub ? "text-[9px] sm:text-[10px]" : "text-[8px] sm:text-[9px]"
                      } ${node.summaryColorClass}`}
                    >
                      {node.subtitle}
                    </span>
                  </button>
                ) : (
                  <motion.div
                    initial={{ opacity: 0, y: 6 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.24, delay: 0.16 }}
                    className="flex h-full w-full flex-col p-3 sm:p-4"
                  >
                    {/* Sticky Top Header */}
                    <div className="flex shrink-0 items-center justify-between gap-2 border-b border-slate-100 pb-2 sm:pb-2.5 dark:border-zinc-800">
                      <button
                        type="button"
                        onClick={() => setZoomedId(null)}
                        className="inline-flex shrink-0 items-center gap-1 rounded-lg border border-slate-200 bg-slate-50 px-2.5 py-1 text-[10px] font-semibold text-slate-700 transition-colors hover:bg-slate-100 dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-200 dark:hover:bg-zinc-800"
                      >
                        <ArrowLeft className="h-3 w-3" />
                        <span>Pentagon</span>
                      </button>
                      <div className="flex min-w-0 items-center gap-1.5">
                        <NodeIcon id={node.id} className="h-3.5 w-3.5" />
                        <span className="truncate text-xs font-bold text-slate-900 dark:text-white">
                          {node.title}
                        </span>
                      </div>
                    </div>

                    {/* Scrollable Body */}
                    <div className="mt-2.5 flex-1 overflow-y-auto pr-0.5 sm:mt-3">
                      <h3 className="text-center text-sm font-bold text-slate-950 dark:text-white">
                        {node.detailHeadline}
                      </h3>
                      <p className="mt-1.5 text-center text-[11px] leading-relaxed text-slate-600 dark:text-zinc-400">
                        {node.detailSub}
                      </p>

                      <button
                        type="button"
                        onClick={() => setZoomedId(null)}
                        className={`mt-3 w-full rounded-xl px-3 py-1.5 text-[11px] font-semibold text-white transition-opacity hover:opacity-90 sm:mt-3.5 sm:py-2 ${
                          node.ctaStyle === "red"
                            ? "bg-rose-600"
                            : "bg-slate-950 dark:bg-indigo-600"
                        }`}
                      >
                        {node.ctaLabel}
                      </button>

                      <div className="mt-2.5 space-y-1.5 sm:mt-3.5 sm:space-y-2">
                        {node.rows.map((row) => (
                          <div
                            key={row.title}
                            className="flex items-center justify-between gap-2 rounded-xl border border-slate-200/80 bg-slate-50/80 px-2.5 py-1.5 sm:py-2 dark:border-zinc-800 dark:bg-zinc-900/70"
                          >
                            <div className="min-w-0">
                              <p className="text-[11px] font-semibold leading-snug text-slate-900 dark:text-zinc-100">
                                {row.title}
                              </p>
                              <p className="text-[10px] leading-snug text-slate-600 dark:text-zinc-400">
                                {row.meta}
                              </p>
                            </div>
                            <span className="shrink-0 rounded-md bg-white px-2 py-0.5 font-mono text-[10px] font-semibold text-slate-800 shadow-sm dark:bg-zinc-800 dark:text-zinc-200">
                              {row.status}
                            </span>
                          </div>
                        ))}
                      </div>
                    </div>

                    {/* Bottom Hint */}
                    <p className="mt-2 shrink-0 border-t border-slate-100 pt-1.5 text-center text-[10px] text-slate-600 sm:mt-2.5 sm:pt-2 dark:border-zinc-800/80 dark:text-zinc-400">
                      Tap “Pentagon” to zoom out
                    </p>
                  </motion.div>
                )}
              </motion.div>
            );
          })}
        </div>

        {/* Bottom Android Gesture Pill */}
        <div className="flex justify-center bg-white pb-2.5 pt-1 dark:bg-[#111116]">
          <div className="h-1 w-24 rounded-full bg-slate-300 dark:bg-zinc-700" />
        </div>
      </div>
    </div>
  );
}
