"use client";

import React from "react";
import { motion } from "framer-motion";
import { Code2, Mail, Smartphone } from "lucide-react";
import { SITE_CONFIG } from "@/lib/constants";

export function FinalCtaSection() {
  const earlyAccessMailto = `mailto:${SITE_CONFIG.legal.email}?subject=${encodeURIComponent(
    "[Custos] Demande d'accès Early Access Google Play"
  )}&body=${encodeURIComponent(
    "Bonjour,\n\nJe souhaite rejoindre le programme Early Access de Custos sur Google Play.\nAdresse e-mail Google Play :\nModèle Android :"
  )}`;

  return (
    <section
      id="early-access"
      className="scroll-mt-20 bg-[#FAFAFA] py-20 sm:py-24 dark:bg-[#0C0C10]"
    >
      <div className="mx-auto max-w-5xl px-4 sm:px-6 lg:px-8">
        <motion.div
          initial={{ opacity: 0, y: 16 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, amount: 0.25 }}
          transition={{ duration: 0.45, ease: [0.22, 1, 0.36, 1] as const }}
          className="relative overflow-hidden rounded-3xl border border-indigo-200/80 bg-gradient-to-br from-slate-950 via-slate-900 to-indigo-950 px-6 py-12 text-center shadow-2xl sm:px-12 sm:py-16 dark:border-indigo-500/30"
        >
          <div
            aria-hidden="true"
            className="pointer-events-none absolute -right-24 -top-24 h-64 w-64 rounded-full bg-indigo-500/20 blur-3xl"
          />
          <div
            aria-hidden="true"
            className="pointer-events-none absolute -bottom-24 -left-24 h-64 w-64 rounded-full bg-purple-500/20 blur-3xl"
          />

          <div className="relative mx-auto max-w-2xl">
            <div className="inline-flex items-center gap-2 rounded-full border border-indigo-400/30 bg-indigo-500/15 px-3.5 py-1 text-xs font-medium text-indigo-200">
              <Smartphone className="h-3.5 w-3.5" />
              <span>Bientôt sur Google Play Store</span>
            </div>

            <h2 className="mt-5 text-3xl font-bold tracking-tight text-white sm:text-4xl">
              Prêt à reprendre la main sur votre attention ?
            </h2>
            <p className="mt-4 text-sm leading-relaxed text-slate-300 sm:text-base">
              Custos est en cours de déploiement sur le Google Play Store (Tests
              fermés / Early Access). Rejoignez l’accès anticipé par e-mail ou
              découvrez dès maintenant les versions publiées sur GitHub.
            </p>

            <div className="mt-8 flex flex-col justify-center gap-3.5 sm:flex-row">
              <a
                href={earlyAccessMailto}
                className="inline-flex items-center justify-center gap-2 rounded-xl bg-white px-6 py-3.5 text-sm font-semibold text-slate-950 shadow-sm transition-all hover:bg-indigo-50"
              >
                <Mail className="h-4 w-4 text-indigo-600" />
                <span>Rejoindre l’Early Access Google Play</span>
              </a>

              <a
                href={SITE_CONFIG.githubReleasesUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center justify-center gap-2 rounded-xl border border-white/20 bg-white/10 px-6 py-3.5 text-sm font-semibold text-white backdrop-blur transition-colors hover:bg-white/15"
              >
                <Code2 className="h-4 w-4" />
                <span>Voir les releases GitHub (MIT)</span>
              </a>
            </div>

            <p className="mt-6 text-xs text-slate-400">
              Android 8.0+ requis · Package :{" "}
              <span className="font-mono text-slate-300">
                {SITE_CONFIG.packageName}
              </span>
            </p>
          </div>
        </motion.div>
      </div>
    </section>
  );
}
