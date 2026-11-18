"use client";

import React, { useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { ChevronDown, HelpCircle } from "lucide-react";

interface FaqItem {
  question: string;
  answer: string;
}

const FAQ_ITEMS: FaqItem[] = [
  {
    question:
      "Why does Custos request the Accessibility Service permission?",
    answer:
      "On Android, the accessibility service is essential to detect the active foreground app in real time, return you automatically to the home screen when a quota or curfew expires, and recognize tabs blocked by Partial Access (such as YouTube Shorts). No keystrokes, passwords, or message contents are ever read or stored.",
  },
  {
    question:
      "Does my usage data ever leave my smartphone?",
    answer:
      "No. Custos operates without any remote server or account creation. Your rules (limits.json, screen_rules.json), daily counters, and preferences remain strictly inside your device’s private local storage.",
  },
  {
    question:
      "What happens if I try to relax a limit or cancel Nuclear Mode?",
    answer:
      "That is the core of Custos’s anti-impulse mechanism: tightening a limit is instant, while relaxing it requires waiting for your configured security delay (from 30 minutes to several hours) to elapse. Nuclear Mode remains locked until the selected timer ends.",
  },
  {
    question: "Does the app require a rooted phone or special privileges?",
    answer:
      "No. Custos runs on any Android 8.0+ smartphone using standard Android permissions (Accessibility and Usage Access) — with no root, no device admin setup, and no computer required. It installs and uninstalls like any standard app.",
  },
  {
    question:
      "How can I install Custos today or join Google Play Early Access?",
    answer:
      "Custos is finalizing its Google Play Store rollout (Early Access / Closed Testing). You can request early access by emailing contact@joshuanancey.com or inspect the MIT-licensed source code and releases directly on GitHub.",
  },
];

export function FaqSection() {
  const [openIndex, setOpenIndex] = useState<number | null>(0);

  return (
    <section
      id="faq"
      className="scroll-mt-24 border-b border-slate-200/80 bg-white py-20 sm:py-24 dark:border-zinc-800/80 dark:bg-[#09090B]"
    >
      <div className="mx-auto max-w-3xl px-4 sm:px-6 lg:px-8">
        <div className="text-center">
          <div className="inline-flex items-center gap-2 rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-xs font-medium text-slate-700 dark:border-zinc-800 dark:bg-zinc-900 dark:text-zinc-300">
            <HelpCircle className="h-3.5 w-3.5 text-indigo-600 dark:text-indigo-400" />
            <span>Frequently asked questions</span>
          </div>
          <h2 className="mt-4 text-3xl font-bold tracking-tight text-slate-950 sm:text-4xl dark:text-white">
            Everything about Custos
          </h2>
          <p className="mt-3 text-sm text-slate-600 sm:text-base dark:text-zinc-400">
            Android permissions, privacy, security delay, and early access.
          </p>
        </div>

        <div className="mt-10 divide-y divide-slate-200 rounded-2xl border border-slate-200 bg-[#FAFAFA] dark:divide-zinc-800 dark:border-zinc-800 dark:bg-[#111116]">
          {FAQ_ITEMS.map((item, idx) => {
            const isOpen = openIndex === idx;
            return (
              <div key={item.question} className="px-6 py-5">
                <button
                  type="button"
                  onClick={() => setOpenIndex(isOpen ? null : idx)}
                  aria-expanded={isOpen}
                  className="flex w-full items-center justify-between gap-4 text-left"
                >
                  <span className="text-sm font-semibold text-slate-950 sm:text-base dark:text-white">
                    {item.question}
                  </span>
                  <ChevronDown
                    className={`h-4 w-4 shrink-0 text-slate-600 transition-transform duration-200 dark:text-zinc-400 ${
                      isOpen ? "rotate-180 text-indigo-600 dark:text-indigo-400" : ""
                    }`}
                  />
                </button>

                <AnimatePresence initial={false}>
                  {isOpen && (
                    <motion.div
                      initial={{ height: 0, opacity: 0 }}
                      animate={{ height: "auto", opacity: 1 }}
                      exit={{ height: 0, opacity: 0 }}
                      transition={{
                        duration: 0.22,
                        ease: [0.22, 1, 0.36, 1] as const,
                      }}
                      className="overflow-hidden"
                    >
                      <p className="pt-3 text-sm leading-relaxed text-slate-600 dark:text-zinc-400">
                        {item.answer}
                      </p>
                    </motion.div>
                  )}
                </AnimatePresence>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
}
