"use client";

import React from "react";
import Link from "next/link";
import { motion } from "framer-motion";
import {
  ArrowRight,
  CheckCircle2,
  Download,
  Lock,
  ShieldCheck,
} from "lucide-react";
import { AndroidPhoneMockup } from "@/components/AndroidPhoneMockup";

export function HeroSection() {
  return (
    <section className="relative overflow-hidden border-b border-slate-200/80 bg-grid-pattern dark:border-zinc-800/80">
      {/* Subtle Top Radial Gradient */}
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-x-0 -top-40 -z-10 flex justify-center overflow-hidden blur-3xl"
      >
        <div className="h-[360px] w-[720px] rounded-full bg-gradient-to-tr from-indigo-500/20 via-violet-500/15 to-transparent dark:from-indigo-500/25 dark:via-purple-600/20" />
      </div>

      <div className="mx-auto max-w-6xl px-4 pb-20 pt-14 sm:px-6 sm:pt-20 lg:px-8 lg:pb-24 lg:pt-24">
        <div className="grid items-center gap-14 lg:grid-cols-12 lg:gap-10">
          {/* Left Copy Column */}
          <motion.div
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.45, ease: [0.22, 1, 0.36, 1] as const }}
            className="lg:col-span-7"
          >
            <div className="inline-flex items-center gap-2 rounded-full border border-indigo-200/80 bg-white/90 px-3.5 py-1.5 text-xs font-medium text-indigo-700 shadow-sm dark:border-indigo-500/30 dark:bg-zinc-900/90 dark:text-indigo-300">
              <ShieldCheck className="h-3.5 w-3.5" />
              <span>Android 8.0+ · Architecture 100 % locale · Zéro publicité</span>
            </div>

            <h1 className="mt-6 text-4xl font-bold tracking-tight text-slate-950 sm:text-5xl lg:text-[3.4rem] lg:leading-[1.08] dark:text-white">
              Maîtrisez votre temps d’écran sur Android.{" "}
              <span className="bg-gradient-to-r from-indigo-600 to-purple-600 bg-clip-text text-transparent dark:from-indigo-400 dark:to-purple-400">
                Sans contournement impulsif.
              </span>
            </h1>

            <p className="mt-5 max-w-2xl text-base leading-relaxed text-slate-600 sm:text-lg dark:text-zinc-400">
              Custos remplace la volonté à chaud par un engagement technique
              fiable : quotas quotidiens, limites par session, couvre-feux,
              blocage chirurgical <strong className="font-semibold text-slate-800 dark:text-zinc-200">Accès Partiel</strong>{" "}
              et délai de sécurité asymétrique avant tout allègement — sans
              compte cloud ni collecte de vos données.
            </p>

            {/* Primary & Secondary CTAs */}
            <div className="mt-8 flex flex-col gap-3.5 sm:flex-row sm:items-center">
              <Link
                href="/#early-access"
                className="inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-xl bg-gradient-to-r from-indigo-600 to-purple-600 px-5 py-3.5 text-sm font-semibold text-white shadow-glow transition-all hover:brightness-110 active:scale-[0.99]"
              >
                <Download className="h-4 w-4 shrink-0" />
                <span>Google Play (Early Access)</span>
              </Link>

              <Link
                href="/#features"
                className="inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-xl border border-slate-200 bg-white px-5 py-3.5 text-sm font-semibold text-slate-800 shadow-sm transition-colors hover:bg-slate-50 dark:border-zinc-800 dark:bg-zinc-900 dark:text-zinc-200 dark:hover:bg-zinc-800"
              >
                <span>Explorer les fonctionnalités</span>
                <ArrowRight className="h-4 w-4 shrink-0" />
              </Link>
            </div>

            {/* Trust Micro-Metrics */}
            <div className="mt-9 grid grid-cols-1 gap-3 border-t border-slate-200/80 pt-6 sm:grid-cols-3 dark:border-zinc-800/80">
              <div className="flex items-center gap-2 text-xs font-medium text-slate-600 sm:whitespace-nowrap dark:text-zinc-400">
                <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-700 dark:text-emerald-400" />
                <span>Zéro tracking serveur</span>
              </div>
              <div className="flex items-center gap-2 text-xs font-medium text-slate-600 sm:whitespace-nowrap dark:text-zinc-400">
                <Lock className="h-4 w-4 shrink-0 text-indigo-600 dark:text-indigo-400" />
                <span>Délai anti-impulsion</span>
              </div>
              <div className="flex items-center gap-2 text-xs font-medium text-slate-600 sm:whitespace-nowrap dark:text-zinc-400">
                <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-700 dark:text-emerald-400" />
                <span>Code MIT auditable</span>
              </div>
            </div>
          </motion.div>

          {/* Right Smartphone Mockup Column */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{
              duration: 0.55,
              delay: 0.1,
              ease: [0.22, 1, 0.36, 1] as const,
            }}
            className="lg:col-span-5"
          >
            <AndroidPhoneMockup />
          </motion.div>
        </div>
      </div>
    </section>
  );
}
