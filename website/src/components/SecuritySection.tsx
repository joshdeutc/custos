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
    title: "Zéro cloud, zéro télémétrie",
    description:
      "Custos n’intègre ni Firebase Analytics, ni régie publicitaire, ni traceur tiers. Aucune statistique d’usage ni habitude d’application n’est jamais envoyée vers nos serveurs.",
  },
  {
    icon: Database,
    title: "Stockage 100 % sur l’appareil",
    description:
      "Vos quotas, règles de couvre-feu, sessions et presets Nucléaires sont enregistrés exclusivement dans l’espace privé sécurisé de l’application (filesDir) sur votre téléphone Android.",
  },
  {
    icon: EyeOff,
    title: "AccessibilityService transparent",
    description:
      "Le service d’accessibilité détecte uniquement l’application au premier plan et les identifiants d’onglets visés par l’Accès Partiel, en mémoire vive à la volée : aucune frappe clavier ni contenu personnel n’est enregistré.",
  },
];

export function SecuritySection() {
  return (
    <section
      id="security"
      className="scroll-mt-20 border-b border-slate-200/80 bg-[#FAFAFA] py-20 sm:py-24 dark:border-zinc-800/80 dark:bg-[#0C0C10]"
    >
      <div className="mx-auto max-w-6xl px-4 sm:px-6 lg:px-8">
        <div className="grid items-start gap-12 lg:grid-cols-12">
          {/* Left Narrative */}
          <div className="lg:col-span-5">
            <div className="inline-flex items-center gap-2 rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1 text-xs font-medium text-emerald-700 dark:border-emerald-500/30 dark:bg-emerald-500/10 dark:text-emerald-300">
              <LockKeyhole className="h-3.5 w-3.5" />
              <span>Confiance &amp; Vie Privée</span>
            </div>

            <h2 className="mt-4 text-3xl font-bold tracking-tight text-slate-950 sm:text-4xl dark:text-white">
              Vos habitudes numériques vous appartiennent. Point final.
            </h2>

            <p className="mt-4 text-sm leading-relaxed text-slate-600 sm:text-base dark:text-zinc-400">
              Un outil qui connaît les applications que vous limitez mérite un
              niveau d’exigence irréprochable. Custos a été architecturé dès le
              premier jour pour exécuter 100 % de sa logique localement, sans
              création de compte ni dépendance serveur.
            </p>

            {/* Local Pipeline Diagram */}
            <div className="mt-8 rounded-2xl border border-slate-200 bg-white p-5 shadow-soft dark:border-zinc-800 dark:bg-[#111116]">
              <p className="font-mono text-[11px] font-semibold uppercase tracking-wider text-indigo-600 dark:text-indigo-400">
                Pipeline local sur votre Android
              </p>
              <div className="mt-4 space-y-3 text-xs">
                <div className="flex items-center justify-between rounded-lg border border-slate-200/80 bg-slate-50 px-3 py-2.5 dark:border-zinc-800 dark:bg-zinc-900/70">
                  <span className="flex items-center gap-2 font-medium text-slate-800 dark:text-zinc-200">
                    <Cpu className="h-4 w-4 text-indigo-500" />
                    1. Lecture locale (UsageStats + A11y RAM)
                  </span>
                  <span className="font-mono text-[10px] text-emerald-600 dark:text-emerald-400">
                    On-device
                  </span>
                </div>
                <div className="flex items-center justify-between rounded-lg border border-slate-200/80 bg-slate-50 px-3 py-2.5 dark:border-zinc-800 dark:bg-zinc-900/70">
                  <span className="flex items-center gap-2 font-medium text-slate-800 dark:text-zinc-200">
                    <FileCheck2 className="h-4 w-4 text-violet-500" />
                    2. Évaluation des règles (limits.json)
                  </span>
                  <span className="font-mono text-[10px] text-emerald-600 dark:text-emerald-400">
                    1 sec tick
                  </span>
                </div>
                <div className="flex items-center justify-between rounded-lg border border-slate-200/80 bg-slate-50 px-3 py-2.5 dark:border-zinc-800 dark:bg-zinc-900/70">
                  <span className="flex items-center gap-2 font-medium text-slate-800 dark:text-zinc-200">
                    <ServerOff className="h-4 w-4 text-emerald-500" />
                    3. Transmission cloud ou revente
                  </span>
                  <span className="font-mono text-[10px] font-semibold text-emerald-600 dark:text-emerald-400">
                    0 octet
                  </span>
                </div>
              </div>
            </div>

            <div className="mt-6 flex flex-wrap gap-4 text-sm font-semibold">
              <Link
                href="/privacy"
                className="inline-flex items-center gap-1.5 text-indigo-600 hover:underline dark:text-indigo-400"
              >
                <span>Lire la politique de confidentialité</span>
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
