import React from "react";
import Link from "next/link";
import { ArrowLeft, Calendar } from "lucide-react";
import { SITE_CONFIG } from "@/lib/constants";

interface LegalPageShellProps {
  badge: string;
  title: string;
  subtitle: string;
  lastUpdated?: string;
  children: React.ReactNode;
}

export function LegalPageShell({
  badge,
  title,
  subtitle,
  lastUpdated = SITE_CONFIG.lastLegalUpdate,
  children,
}: LegalPageShellProps) {
  return (
    <div className="bg-[#FAFAFA] py-12 sm:py-16 dark:bg-[#09090B]">
      <div className="mx-auto max-w-4xl px-4 sm:px-6 lg:px-8">
        <Link
          href="/"
          className="inline-flex items-center gap-2 text-sm font-medium text-slate-600 transition-colors hover:text-indigo-600 dark:text-zinc-400 dark:hover:text-indigo-400"
        >
          <ArrowLeft className="h-4 w-4" />
          <span>Retour à l’accueil</span>
        </Link>

        <header className="mt-6 border-b border-slate-200 pb-8 dark:border-zinc-800">
          <span className="inline-flex items-center rounded-full border border-indigo-200 bg-indigo-50 px-3 py-1 text-xs font-semibold text-indigo-700 dark:border-indigo-500/30 dark:bg-indigo-500/10 dark:text-indigo-300">
            {badge}
          </span>
          <h1 className="mt-4 text-3xl font-bold tracking-tight text-slate-950 sm:text-4xl dark:text-white">
            {title}
          </h1>
          <p className="mt-3 text-base leading-relaxed text-slate-600 dark:text-zinc-400">
            {subtitle}
          </p>
          <div className="mt-4 flex flex-wrap items-center gap-4 text-xs text-slate-500 dark:text-zinc-400">
            <span className="inline-flex items-center gap-1.5">
              <Calendar className="h-3.5 w-3.5" />
              <span>Dernière mise à jour : {lastUpdated}</span>
            </span>
            <span className="font-mono">
              Application : {SITE_CONFIG.legal.publisherApp} ({SITE_CONFIG.packageName})
            </span>
          </div>
        </header>

        <article className="mt-8 space-y-8 text-sm leading-relaxed text-slate-700 sm:text-[15px] dark:text-zinc-300">
          {children}
        </article>
      </div>
    </div>
  );
}
