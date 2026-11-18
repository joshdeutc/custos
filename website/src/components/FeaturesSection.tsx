"use client";

import React from "react";
import { motion } from "framer-motion";
import {
  Clock,
  Flame,
  Hourglass,
  Layers,
  Scissors,
  ShieldAlert,
} from "lucide-react";

interface FeatureCardItem {
  id: string;
  tag: string;
  title: string;
  description: string;
  highlights: string[];
  icon: "delay" | "partial" | "quotas" | "nuclear";
  accentBg: string;
  accentText: string;
}

const FEATURES: FeatureCardItem[] = [
  {
    id: "delay",
    tag: "Anti-impulse asymmetry",
    title: "Anti-Bypass Security Delay",
    description:
      "Tightening a limit or adding a blocked app takes effect immediately. However, raising a quota or deleting a rule triggers a mandatory cooling-off delay (e.g., 30 min to 24h) before taking effect.",
    highlights: [
      "Customizable protection duration per individual rule",
      "Protection of sensitive Android Settings pages",
      "Delay preserved even after a device reboot",
    ],
    icon: "delay",
    accentBg: "bg-indigo-500/10 dark:bg-indigo-500/15",
    accentText: "text-indigo-600 dark:text-indigo-400",
  },
  {
    id: "partial",
    tag: "Surgical in-app filtering",
    title: "Partial Access (Tabs & Feeds)",
    description:
      "Stop sacrificing an entire useful app because of a single addictive tab. Custos locally identifies the targeted screen (e.g., YouTube Shorts or news feeds) and redirects you straight back to the allowed screen.",
    highlights: [
      "2-tap differential learning right on your phone",
      "Keeps messaging, calls, and useful search intact",
      "Evaluated strictly in RAM — no screenshots or screen recording",
    ],
    icon: "partial",
    accentBg: "bg-sky-500/10 dark:bg-sky-500/15",
    accentText: "text-sky-700 dark:text-sky-400",
  },
  {
    id: "quotas",
    tag: "1 Hz time engine",
    title: "Quotas, Sessions & Curfews",
    description:
      "Combine daily time budgets (logical 2:00 AM reset), priority night curfews, and per-session limits (max duration per unlock plus mandatory cooldown before reopening the app).",
    highlights: [
      "Automatic notification muting while an app is blocked",
      "Accurate measurement powered by UsageStats events",
      "Automatic restart after phone reboot",
    ],
    icon: "quotas",
    accentBg: "bg-violet-500/10 dark:bg-violet-500/15",
    accentText: "text-violet-600 dark:text-violet-400",
  },
  {
    id: "nuclear",
    tag: "Deep focus & Presets",
    title: "Nuclear Mode & Named Presets",
    description:
      "Need total focus for an exam, a Deep Work sprint, or a screen-free evening? Trigger a firm lockdown for a fixed duration (30 min, 1h, 2h, 4h, or custom) with zero early cancellation.",
    highlights: [
      "One-tap reusable named presets (Deep Work, Exams)",
      "Optional automatic Do Not Disturb activation",
      "Emergency calls and critical system apps always preserved",
    ],
    icon: "nuclear",
    accentBg: "bg-rose-500/10 dark:bg-rose-500/15",
    accentText: "text-rose-700 dark:text-rose-400",
  },
];

function renderFeatureIcon(icon: FeatureCardItem["icon"], className: string) {
  switch (icon) {
    case "delay":
      return <Hourglass className={className} />;
    case "partial":
      return <Scissors className={className} />;
    case "quotas":
      return <Clock className={className} />;
    case "nuclear":
      return <Flame className={className} />;
  }
}

export function FeaturesSection() {
  return (
    <section
      id="features"
      className="scroll-mt-24 border-b border-slate-200/80 bg-white py-20 sm:py-24 dark:border-zinc-800/80 dark:bg-[#09090B]"
    >
      <div className="mx-auto max-w-6xl px-4 sm:px-6 lg:px-8">
        <div className="max-w-2xl">
          <div className="inline-flex items-center gap-2 rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-xs font-medium text-slate-700 dark:border-zinc-800 dark:bg-zinc-900 dark:text-zinc-300">
            <Layers className="h-3.5 w-3.5 text-indigo-600 dark:text-indigo-400" />
            <span>Core features</span>
          </div>
          <h2 className="mt-4 text-3xl font-bold tracking-tight text-slate-950 sm:text-4xl dark:text-white">
            Built to break automatic habits, not your productivity.
          </h2>
          <p className="mt-4 text-base leading-relaxed text-slate-600 dark:text-zinc-400">
            Every Custos module solves a classic flaw of traditional screen-time
            limiters: either too easy to disable in a split second, or too blunt
            for the tools you genuinely rely on every day.
          </p>
        </div>

        <div className="mt-12 grid gap-6 md:grid-cols-2">
          {FEATURES.map((feature, index) => (
            <motion.article
              key={feature.id}
              initial={{ opacity: 0, y: 18 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, amount: 0.2 }}
              transition={{
                duration: 0.4,
                delay: index * 0.06,
                ease: [0.22, 1, 0.36, 1] as const,
              }}
              className="group relative flex flex-col justify-between rounded-2xl border border-slate-200/90 bg-[#FAFAFA] p-6 shadow-soft transition-all hover:border-indigo-300 sm:p-8 dark:border-zinc-800 dark:bg-[#111116] dark:hover:border-indigo-500/50"
            >
              <div>
                <div className="flex items-center justify-between gap-4">
                  <div
                    className={`inline-flex h-11 w-11 items-center justify-center rounded-xl ${feature.accentBg}`}
                  >
                    {renderFeatureIcon(
                      feature.icon,
                      `h-5 w-5 ${feature.accentText}`
                    )}
                  </div>
                  <span className="rounded-full border border-slate-200 bg-white px-2.5 py-1 text-[11px] font-medium text-slate-600 dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-300">
                    {feature.tag}
                  </span>
                </div>

                <h3 className="mt-5 text-xl font-semibold tracking-tight text-slate-950 dark:text-white">
                  {feature.title}
                </h3>
                <p className="mt-3 text-sm leading-relaxed text-slate-600 dark:text-zinc-400">
                  {feature.description}
                </p>
              </div>

              <ul className="mt-6 space-y-2.5 border-t border-slate-200/80 pt-5 dark:border-zinc-800/80">
                {feature.highlights.map((item) => (
                  <li
                    key={item}
                    className="flex items-start gap-2.5 text-xs font-medium text-slate-700 dark:text-zinc-300"
                  >
                    <ShieldAlert className="mt-0.5 h-3.5 w-3.5 shrink-0 text-indigo-600 dark:text-indigo-400" />
                    <span>{item}</span>
                  </li>
                ))}
              </ul>
            </motion.article>
          ))}
        </div>
      </div>
    </section>
  );
}
