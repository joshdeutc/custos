import React from "react";
import Link from "next/link";
import { ArrowRight, Mail, MapPin, ShieldCheck } from "lucide-react";
import { CustosLogo } from "@/components/CustosLogo";
import { SITE_CONFIG } from "@/lib/constants";

export function Footer() {
  const { legal } = SITE_CONFIG;

  return (
    <footer className="border-t border-slate-200 bg-white dark:border-zinc-800/80 dark:bg-[#09090B]">
      <div className="mx-auto max-w-6xl px-4 py-12 sm:px-6 lg:px-8">
        <div className="grid gap-10 md:grid-cols-12">
          {/* Brand Summary */}
          <div className="md:col-span-4">
            <Link href="/" className="inline-flex items-center gap-2.5">
              <CustosLogo size={28} />
              <span className="text-base font-semibold tracking-tight text-slate-900 dark:text-white">
                Custos
              </span>
            </Link>
            <p className="mt-3 max-w-xs text-sm leading-relaxed text-slate-600 dark:text-zinc-400">
              Android self-discipline and screen-time control app. 100% local
              processing on your device.
            </p>
            <div className="mt-4 inline-flex items-center gap-2 rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1 text-xs font-medium text-emerald-700 dark:border-emerald-500/30 dark:bg-emerald-500/10 dark:text-emerald-300">
              <ShieldCheck className="h-3.5 w-3.5" />
              <span>Zero ads · Zero trackers</span>
            </div>
          </div>

          {/* Publisher & Contact */}
          <div className="md:col-span-4">
            <h2 className="text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-zinc-400">
              Publisher &amp; Contact
            </h2>
            <dl className="mt-3 space-y-2.5 text-sm text-slate-700 dark:text-zinc-300">
              <div>
                <dt className="inline font-medium text-slate-900 dark:text-white">
                  Publisher:{" "}
                </dt>
                <dd className="inline">
                  {legal.publisherApp} — {legal.publisherDisplayName}
                </dd>
              </div>
              <div className="flex items-center gap-1.5">
                <MapPin className="h-4 w-4 shrink-0 text-slate-600 dark:text-zinc-400" />
                <div>
                  <dt className="inline font-medium text-slate-900 dark:text-white">
                    Location:{" "}
                  </dt>
                  <dd className="inline">{legal.city}</dd>
                </div>
              </div>
              <div className="flex items-center gap-1.5">
                <Mail className="h-4 w-4 shrink-0 text-slate-600 dark:text-zinc-400" />
                <div>
                  <dt className="inline font-medium text-slate-900 dark:text-white">
                    Support &amp; Privacy:{" "}
                  </dt>
                  <dd className="inline">
                    <a
                      href={`mailto:${legal.email}`}
                      className="font-medium text-indigo-600 underline-offset-4 hover:underline dark:text-indigo-400"
                    >
                      {legal.email}
                    </a>
                  </dd>
                </div>
              </div>
            </dl>
            <div className="mt-3">
              <Link
                href="/terms"
                className="inline-flex items-center gap-1 text-xs font-medium text-indigo-600 underline-offset-4 hover:underline dark:text-indigo-400"
              >
                <span>Full Legal Notice</span>
                <ArrowRight className="h-3.5 w-3.5" />
              </Link>
            </div>
          </div>

          {/* Navigation & Legal Links */}
          <div className="md:col-span-4">
            <h2 className="text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-zinc-400">
              Compliance &amp; Docs
            </h2>
            <ul className="mt-3 space-y-2.5 text-sm">
              <li>
                <Link
                  href="/privacy"
                  className="text-slate-600 transition-colors hover:text-indigo-600 dark:text-zinc-400 dark:hover:text-indigo-400"
                >
                  Privacy Policy (GDPR)
                </Link>
              </li>
              <li>
                <Link
                  href="/terms"
                  className="text-slate-600 transition-colors hover:text-indigo-600 dark:text-zinc-400 dark:hover:text-indigo-400"
                >
                  Legal Notice &amp; Terms
                </Link>
              </li>
              <li>
                <Link
                  href="/delete-account"
                  className="text-slate-600 transition-colors hover:text-indigo-600 dark:text-zinc-400 dark:hover:text-indigo-400"
                >
                  Account &amp; Data Deletion
                </Link>
              </li>
              <li>
                <a
                  href={SITE_CONFIG.githubRepoUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-slate-600 transition-colors hover:text-indigo-600 dark:text-zinc-400 dark:hover:text-indigo-400"
                >
                  Source Code (GitHub · MIT)
                </a>
              </li>
            </ul>
          </div>
        </div>

        <div className="mt-10 flex flex-col items-start justify-between gap-3 border-t border-slate-200/80 pt-6 text-xs text-slate-600 sm:flex-row sm:items-center dark:border-zinc-800/80 dark:text-zinc-400">
          <p>
            © 2026 {legal.publisherApp} — {legal.publisherDisplayName}. All
            rights reserved.
          </p>
          <p className="font-mono">
            custos.joshuanancey.com
          </p>
        </div>
      </div>
    </footer>
  );
}
