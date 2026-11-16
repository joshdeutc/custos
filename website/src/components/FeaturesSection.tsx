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
    tag: "Asymétrie anti-impulsion",
    title: "Délai de Sécurité Anti-Contournement",
    description:
      "Durcir une limite ou ajouter une application bloquée prend effet immédiatement. En revanche, relever un quota ou supprimer une règle déclenche obligatoirement un délai de réflexion (ex. 30 min à 24 h) avant application.",
    highlights: [
      "Durée de protection personnalisable à la règle près",
      "Protection des pages sensibles dans Paramètres Android",
      "Délai conservé même après redémarrage du téléphone",
    ],
    icon: "delay",
    accentBg: "bg-indigo-500/10 dark:bg-indigo-500/15",
    accentText: "text-indigo-600 dark:text-indigo-400",
  },
  {
    id: "partial",
    tag: "Filtrage chirurgical in-app",
    title: "Accès Partiel (Onglets & Flux)",
    description:
      "Ne sacrifiez plus toute une application utile à cause d’un seul onglet addictif. Custos identifie localement l’écran ciblé (ex. YouTube Shorts ou flux d’actualités) et vous redirige instantanément vers l’écran autorisé.",
    highlights: [
      "Apprentissage différentiel en 2 taps sur votre téléphone",
      "Conserve la messagerie, les appels et la recherche utiles",
      "Analyse exclusivement en RAM, sans capture ni stockage d’écran",
    ],
    icon: "partial",
    accentBg: "bg-sky-500/10 dark:bg-sky-500/15",
    accentText: "text-sky-700 dark:text-sky-400",
  },
  {
    id: "quotas",
    tag: "Moteur temporel 1 Hz",
    title: "Quotas, Sessions & Couvre-feux",
    description:
      "Combinez budgets quotidiens (réinitialisation logique à 2h00 du matin), couvre-feux nocturnes prioritaires et limites par session (durée max par ouverture + temps de pause obligatoire avant de pouvoir rouvrir l’app).",
    highlights: [
      "Mise en sourdine automatique des notifications pendant le blocage",
      "Mesure précise basée sur UsageStats events",
      "Redémarrage automatique après reboot du téléphone",
    ],
    icon: "quotas",
    accentBg: "bg-violet-500/10 dark:bg-violet-500/15",
    accentText: "text-violet-600 dark:text-violet-400",
  },
  {
    id: "nuclear",
    tag: "Focus radical & Presets",
    title: "Mode Nucléaire & Presets Nommés",
    description:
      "Besoin d’une concentration totale pour un examen, une session Deep Work ou une soirée sans écran ? Déclenchez un blocage ferme sur une durée fixe (30 min, 1 h, 2 h, 4 h ou sur mesure), sans possibilité d’annulation anticipée.",
    highlights: [
      "Presets nommés réutilisables en un tap (Deep Work, Examens)",
      "Activation automatique du mode Ne pas déranger (optionnel)",
      "Appels d’urgence et applications critiques toujours préservés",
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
            <span>Fonctionnalités réelles</span>
          </div>
          <h2 className="mt-4 text-3xl font-bold tracking-tight text-slate-950 sm:text-4xl dark:text-white">
            Conçu pour couper le réflexe automatique, pas votre productivité.
          </h2>
          <p className="mt-4 text-base leading-relaxed text-slate-600 dark:text-zinc-400">
            Chaque module de Custos répond à une faille classique des
            limiteurs d’écran traditionnels : trop faciles à désactiver en un
            instant ou trop radicaux pour les outils utiles du quotidien.
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
