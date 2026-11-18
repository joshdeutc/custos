"use client";

import React, { useState } from "react";
import Link from "next/link";
import { Menu, Moon, Smartphone, Sun, X } from "lucide-react";
import { CustosLogo } from "@/components/CustosLogo";
import { useTheme } from "@/components/ThemeProvider";

const NAV_LINKS = [
  { label: "Features", href: "/#features" },
  { label: "Security", href: "/#security" },
  { label: "FAQ", href: "/#faq" },
];

export function Navbar() {
  const { theme, toggleTheme } = useTheme();
  const [mobileOpen, setMobileOpen] = useState(false);

  return (
    <header className="sticky top-0 z-50 border-b border-slate-200/80 bg-white/80 backdrop-blur-xl dark:border-zinc-800/80 dark:bg-[#09090B]/80">
      <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4 sm:px-6 lg:px-8">
        <Link
          href="/"
          className="flex items-center gap-2.5 text-slate-900 transition-opacity hover:opacity-90 dark:text-white"
          aria-label="Custos, back to home"
        >
          <CustosLogo size={32} className="shrink-0 shadow-sm" />
          <span className="text-lg font-semibold tracking-tight">Custos</span>
          <span className="hidden rounded-full border border-indigo-200 bg-indigo-50 px-2 py-0.5 text-[11px] font-medium text-indigo-700 sm:inline-flex dark:border-indigo-500/30 dark:bg-indigo-500/10 dark:text-indigo-300">
            Android
          </span>
        </Link>

        <nav
          className="hidden items-center gap-8 md:flex"
          aria-label="Main navigation"
        >
          {NAV_LINKS.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className="text-sm font-medium text-slate-600 transition-colors hover:text-slate-950 dark:text-zinc-400 dark:hover:text-white"
            >
              {item.label}
            </Link>
          ))}
        </nav>

        <div className="flex items-center gap-2.5">
          <button
            type="button"
            onClick={toggleTheme}
            className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-slate-200 bg-white text-slate-700 transition-colors hover:bg-slate-50 hover:text-slate-950 dark:border-zinc-800 dark:bg-zinc-900 dark:text-zinc-300 dark:hover:bg-zinc-800 dark:hover:text-white"
            aria-label={
              theme === "dark"
                ? "Switch to light mode"
                : "Switch to dark mode"
            }
          >
            {theme === "dark" ? (
              <Sun className="h-4 w-4" />
            ) : (
              <Moon className="h-4 w-4" />
            )}
          </button>

          <Link
            href="/#early-access"
            className="hidden items-center gap-2 rounded-lg bg-slate-900 px-3.5 py-2 text-xs font-semibold text-white shadow-sm transition-all hover:bg-indigo-600 sm:inline-flex dark:bg-indigo-600 dark:hover:bg-indigo-500"
          >
            <Smartphone className="h-3.5 w-3.5" />
            <span>Coming to Google Play</span>
          </Link>

          <button
            type="button"
            onClick={() => setMobileOpen((prev) => !prev)}
            className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-slate-200 text-slate-700 md:hidden dark:border-zinc-800 dark:text-zinc-300"
            aria-label="Open menu"
            aria-expanded={mobileOpen}
          >
            {mobileOpen ? (
              <X className="h-4 w-4" />
            ) : (
              <Menu className="h-4 w-4" />
            )}
          </button>
        </div>
      </div>

      {mobileOpen && (
        <div className="border-t border-slate-200 bg-white px-4 pb-4 pt-3 md:hidden dark:border-zinc-800 dark:bg-[#09090B]">
          <div className="flex flex-col gap-3">
            {NAV_LINKS.map((item) => (
              <Link
                key={item.href}
                href={item.href}
                onClick={() => setMobileOpen(false)}
                className="rounded-md px-2 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-100 dark:text-zinc-300 dark:hover:bg-zinc-900"
              >
                {item.label}
              </Link>
            ))}
            <Link
              href="/#early-access"
              onClick={() => setMobileOpen(false)}
              className="mt-1 inline-flex items-center justify-center gap-2 rounded-lg bg-slate-900 px-4 py-2.5 text-xs font-semibold text-white dark:bg-indigo-600"
            >
              <Smartphone className="h-3.5 w-3.5" />
              <span>Coming to Google Play</span>
            </Link>
          </div>
        </div>
      )}
    </header>
  );
}
