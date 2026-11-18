"use client";

import React from "react";
import Link from "next/link";
import { motion } from "framer-motion";
import {
  ArrowRight,
  Cpu,
  Database,
  EyeOff,
  FileCheck2,
  LockKeyhole,
  ServerOff,
} from "lucide-react";

const TRUST_PILLARS = [
  {
    icon: ServerOff,
    title: "Zero cloud, zero telemetry",
    description:
      "Custos includes no Firebase Analytics, no ad networks, and no third-party trackers. No usage statistics or app habits are ever sent to external servers.",
  },
  {
    icon: Database,
    title: "100% on-device storage",
    description:
      "Your quotas, curfew rules, sessions, and Nuclear presets are stored exclusively inside the app’s private internal storage (filesDir) on your Android phone.",
  },
  {
    icon: EyeOff,
    title: "Transparent AccessibilityService",
    description:
      "The accessibility service only detects the foreground app and view IDs targeted by Partial Access on the fly in RAM: no keystrokes, passwords, or personal content are ever recorded.",
  },
];

export function SecuritySection() {
  return (
    <section
      id="security"
      className="scroll-mt-24 border-b border-slate-200/80 bg-[#FAFAFA] py-20 sm:py-24 dark:border-zinc-800/80 dark:bg-[#0C0C10]"
    >
      <div className="mx-auto max-w-6xl px-4 sm:px-6 lg:px-8">
        <div className="grid items-start gap-12 lg:grid-cols-12">
          {/* Left Narrative */}
          <div className="lg:col-span-5">
            <div className="inline-flex items-center gap-2 rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1 text-xs font-medium text-emerald-700 dark:border-emerald-500/30 dark:bg-emerald-500/10 dark:text-emerald-300">
              <LockKeyhole className="h-3.5 w-3.5" />
              <span>Trust &amp; Privacy</span>
            </div>

            <h2 className="mt-4 text-3xl font-bold tracking-tight text-slate-950 sm:text-4xl dark:text-white">
              Your digital habits belong to you. Period.
            </h2>

            <p className="mt-4 text-sm leading-relaxed text-slate-600 sm:text-base dark:text-zinc-400">
              A tool that knows which apps you restrict deserves uncompromising
              privacy standards. Custos was architected from day one to run 100%
              of its logic locally, with no account creation and zero server
              dependency.
            </p>

            {/* Local Pipeline Diagram */}
            <div className="mt-8 rounded-2xl border border-slate-200 bg-white p-5 shadow-soft dark:border-zinc-800 dark:bg-[#111116]">
              <p className="font-mono text-[11px] font-semibold uppercase tracking-wider text-indigo-600 dark:text-indigo-400">
                Local pipeline on your Android
              </p>
              <div className="mt-4 space-y-3 text-xs">
                <div className="flex items-center justify-between gap-3 rounded-lg border border-slate-200/80 bg-slate-50 px-3 py-2.5 dark:border-zinc-800 dark:bg-zinc-900/70">
                  <span className="flex min-w-0 items-center gap-2 font-medium text-slate-800 dark:text-zinc-200">
                    <Cpu className="h-4 w-4 shrink-0 text-indigo-600 dark:text-indigo-400" />
                    1. Local read (UsageStats + A11y RAM)
                  </span>
                  <span className="shrink-0 whitespace-nowrap font-mono text-[10px] font-semibold text-emerald-700 dark:text-emerald-400">
                    On-device
                  </span>
                </div>
                <div className="flex items-center justify-between gap-3 rounded-lg border border-slate-200/80 bg-slate-50 px-3 py-2.5 dark:border-zinc-800 dark:bg-zinc-900/70">
                  <span className="flex min-w-0 items-center gap-2 font-medium text-slate-800 dark:text-zinc-200">
                    <FileCheck2 className="h-4 w-4 shrink-0 text-violet-600 dark:text-violet-400" />
                    2. Rule evaluation (limits.json)
                  </span>
                  <span className="shrink-0 whitespace-nowrap font-mono text-[10px] font-semibold text-emerald-700 dark:text-emerald-400">
                    1 sec tick
                  </span>
                </div>
                <div className="flex items-center justify-between gap-3 rounded-lg border border-slate-200/80 bg-slate-50 px-3 py-2.5 dark:border-zinc-800 dark:bg-zinc-900/70">
                  <span className="flex min-w-0 items-center gap-2 font-medium text-slate-800 dark:text-zinc-200">
                    <ServerOff className="h-4 w-4 shrink-0 text-emerald-700 dark:text-emerald-400" />
                    3. Cloud transmission or resale
                  </span>
                  <span className="shrink-0 whitespace-nowrap font-mono text-[10px] font-semibold text-emerald-700 dark:text-emerald-400">
                    0 bytes
                  </span>
                </div>
              </div>
            </div>

            <div className="mt-6 flex flex-wrap gap-4 text-sm font-semibold">
              <Link
                href="/privacy"
                className="inline-flex items-center gap-1.5 text-indigo-600 hover:underline dark:text-indigo-400"
              >
                <span>Read the Privacy Policy</span>
                <ArrowRight className="h-4 w-4" />
              </Link>
            </div>
          </div>

          {/* Right Cards */}
          <div className="space-y-5 lg:col-span-7">
            {TRUST_PILLARS.map((pillar, idx) => {
              const Icon = pillar.icon;
              return (
                <motion.div
                  key={pillar.title}
                  initial={{ opacity: 0, x: 16 }}
                  whileInView={{ opacity: 1, x: 0 }}
                  viewport={{ once: true, amount: 0.25 }}
                  transition={{
                    duration: 0.4,
                    delay: idx * 0.08,
                    ease: [0.22, 1, 0.36, 1] as const,
                  }}
                  className="rounded-2xl border border-slate-200/90 bg-white p-6 shadow-soft dark:border-zinc-800 dark:bg-[#111116]"
                >
                  <div className="flex items-start gap-4">
                    <div className="inline-flex h-11 w-11 shrink-0 items-center justify-center rounded-xl border border-slate-200 bg-slate-50 text-indigo-600 dark:border-zinc-700 dark:bg-zinc-900 dark:text-indigo-400">
                      <Icon className="h-5 w-5" />
                    </div>
                    <div>
                      <h3 className="text-lg font-semibold text-slate-950 dark:text-white">
                        {pillar.title}
                      </h3>
                      <p className="mt-2 text-sm leading-relaxed text-slate-600 dark:text-zinc-400">
                        {pillar.description}
                      </p>
                    </div>
                  </div>
                </motion.div>
              );
            })}
          </div>
        </div>
      </div>
    </section>
  );
}
