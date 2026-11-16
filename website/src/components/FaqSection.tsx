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
      "Pourquoi Custos demande-t-il l’autorisation Service d’Accessibilité ?",
    answer:
      "Sur Android, le service d’accessibilité est indispensable pour connaître en temps réel l’application au premier plan, vous ramener automatiquement sur l’écran d’accueil lorsqu’un quota ou couvre-feu est dépassé, et reconnaître les onglets bloqués par la fonction Accès Partiel (comme YouTube Shorts). Aucune frappe clavier, aucun mot de passe ni contenu de message n’est jamais lu ou enregistré.",
  },
  {
    question:
      "Mes données d’utilisation quittent-elles mon smartphone ?",
    answer:
      "Non. Custos fonctionne sans serveur distant ni création de compte. Vos règles (limits.json, screen_rules.json), compteurs quotidiens et préférences restent strictement dans le stockage privé local de votre appareil.",
  },
  {
    question:
      "Que se passe-t-il si j’essaie d’alléger une limite ou d’annuler le Mode Nucléaire ?",
    answer:
      "C’est le cœur du mécanisme anti-impulsion de Custos : renforcer une limite est instantané, mais l’alléger exige d’attendre l’expiration du délai de sécurité que vous avez défini (de 30 minutes à plusieurs heures). Le Mode Nucléaire, quant à lui, reste verrouillé jusqu’à la fin du minuteur choisi.",
  },
  {
    question: "L’application nécessite-t-elle un téléphone rooté ou des droits spéciaux ?",
    answer:
      "Non. Custos fonctionne sur n’importe quel smartphone Android 8.0 ou supérieur grâce aux autorisations standard d’Android (Accessibilité et Données d’utilisation) — sans root, sans privilège administrateur d’appareil et sans ordinateur. Elle s’installe et se désinstalle comme une application classique.",
  },
  {
    question:
      "Comment installer Custos aujourd’hui ou rejoindre l’Early Access Google Play ?",
    answer:
      "Custos finalise son déploiement sur le Google Play Store (phase Early Access / Tests fermés). Vous pouvez demander un accès anticipé par e-mail à contact@joshuanancey.com ou consulter dès maintenant le code source et les versions publiées sous licence MIT sur GitHub.",
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
            <span>Questions fréquentes</span>
          </div>
          <h2 className="mt-4 text-3xl font-bold tracking-tight text-slate-950 sm:text-4xl dark:text-white">
            Tout savoir sur Custos
          </h2>
          <p className="mt-3 text-sm text-slate-600 sm:text-base dark:text-zinc-400">
            Permissions Android, vie privée, principe du délai et accès au
            téléchargement.
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
