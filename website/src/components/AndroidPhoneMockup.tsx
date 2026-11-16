"use client";

import React, { useState } from "react";
import { motion } from "framer-motion";
import { ArrowLeft, BatteryCharging, Lock, Wifi } from "lucide-react";

type NodeId = "delay" | "limits" | "curfew" | "nuclear" | "partial" | "schedule";

interface CircleNode {
  id: NodeId;
  emoji: string;
  title: string;
  subtitle: string;
  cx: number;
  cy: number;
  size: number;
  isHub?: boolean;
  summaryColorClass: string;
  detailHeadline: string;
  detailSub: string;
  ctaLabel: string;
  ctaStyle: "black" | "red";
  rows: { title: string; meta: string; status: string }[];
}

// Stage: 306px wide x 448px tall; Hub center (153, 224), R = 104px, pentagon angles 270, 342, 54, 126, 198 deg
const HUB_CX = 153;
const HUB_CY = 224;
const HUB_R = 52; // diameter = 104
const SAT_R = 38; // diameter = 76
const EASE_CURVE = [0.22, 1, 0.36, 1] as const;

const NODES: CircleNode[] = [
  {
    id: "delay",
    emoji: "⏳",
    title: "Delay & Limits",
    subtitle: "Protection active",
    cx: HUB_CX,
    cy: HUB_CY,
    size: HUB_R * 2,
    isHub: true,
    summaryColorClass: "text-emerald-600 dark:text-emerald-400",
    detailHeadline: "Security & Delay",
    detailSub:
      "Renforcer une règle est immédiat. L’alléger exige d’attendre la fin du délai choisi.",
    ctaLabel: "Délai global : 12h 00",
    ctaStyle: "black",
    rows: [
      { title: "Durcissement d’une limite", meta: "Application directe", status: "Instantané" },
      { title: "Allègement / Suppression", meta: "Sas de réflexion actif", status: "11h 42m" },
      { title: "Protection Paramètres", meta: "Anti-désactivation rapide", status: "Actif" },
    ],
  },
  {
    id: "limits",
    emoji: "📱",
    title: "App Limits",
    subtitle: "3 apps · 1 bloquée",
    cx: 153,
    cy: 120,
    size: SAT_R * 2,
    summaryColorClass: "text-slate-600 dark:text-zinc-400",
    detailHeadline: "App Limits",
    detailSub:
      "Budget quotidien cumulé au premier plan (reset à 02h00) + limites par session.",
    ctaLabel: "+ Ajouter une application",
    ctaStyle: "black",
    rows: [
      { title: "Instagram", meta: "Quota : 30 min / jour", status: "24m / 30m" },
      { title: "TikTok", meta: "Quota quotidien atteint", status: "Bloqué" },
      { title: "Discord", meta: "Session 5 min · Pause 4h", status: "2 / 3 sess." },
    ],
  },
  {
    id: "curfew",
    emoji: "🌙",
    title: "Curfew",
    subtitle: "22:00 → 07:00",
    cx: 252,
    cy: 192,
    size: SAT_R * 2,
    summaryColorClass: "text-indigo-600 dark:text-indigo-400",
    detailHeadline: "Curfew",
    detailSub:
      "Blocage strict pendant vos heures de sommeil ou d’étude, notifications coupées.",
    ctaLabel: "+ Add curfew rule",
    ctaStyle: "black",
    rows: [
      { title: "Réseaux sociaux · Soir", meta: "Lun–Dim · Notifs en pause", status: "22h → 07h" },
      { title: "Deep Study · Matin", meta: "Lun–Ven · YouTube, Reddit", status: "09h → 12h" },
    ],
  },
  {
    id: "nuclear",
    emoji: "☢️",
    title: "Nuclear",
    subtitle: "Total block",
    cx: 214,
    cy: 308,
    size: SAT_R * 2,
    summaryColorClass: "text-rose-600 dark:text-rose-400",
    detailHeadline: "Nuclear Mode",
    detailSub:
      "Verrouillage ferme et irréversible sur une durée fixe avec presets nommés et Ne pas déranger.",
    ctaLabel: "Activate Nuclear Mode",
    ctaStyle: "red",
    rows: [
      { title: "Preset « Deep Work »", meta: "6 apps + Ne pas déranger", status: "2h 00" },
      { title: "Preset « Examens »", meta: "Tous réseaux sociaux", status: "4h 00" },
      { title: "Annulation anticipée", meta: "Impossible avant fin du timer", status: "Verrouillé" },
    ],
  },
  {
    id: "partial",
    emoji: "🎯",
    title: "Partial Access",
    subtitle: "In-app tabs",
    cx: 92,
    cy: 308,
    size: SAT_R * 2,
    summaryColorClass: "text-sky-600 dark:text-sky-400",
    detailHeadline: "Partial Access",
    detailSub:
      "Redirige instantanément hors des onglets addictifs sans fermer l’application utile.",
    ctaLabel: "+ Apprendre un écran",
    ctaStyle: "black",
    rows: [
      { title: "YouTube Shorts", meta: "Redirection → Accueil YouTube", status: "Bloqué" },
      { title: "WhatsApp · Chaînes", meta: "Discussions & appels gardés", status: "Bloqué" },
    ],
  },
  {
    id: "schedule",
    emoji: "📅",
    title: "Schedule",
    subtitle: "Jours & heures",
    cx: 54,
    cy: 192,
    size: SAT_R * 2,
    summaryColorClass: "text-slate-600 dark:text-zinc-400",
    detailHeadline: "Allowed Days & Hours",
    detailSub:
      "Autorisez certaines applications uniquement sur des jours ou créneaux précis.",
    ctaLabel: "Configurer les créneaux",
    ctaStyle: "black",
    rows: [
      { title: "Jeux & Streaming", meta: "Ven 18h → Dim 22h uniquement", status: "Restreint" },
      { title: "Navigateurs", meta: "Hors plages nocturnes", status: "08h → 22h" },
    ],
  },
];

function computeEdgeLine(sat: CircleNode) {
  const dx = sat.cx - HUB_CX;
  const dy = sat.cy - HUB_CY;
  const dist = Math.hypot(dx, dy);
  const ux = dx / dist;
  const uy = dy / dist;
  return {
    x1: HUB_CX + ux * HUB_R,
    y1: HUB_CY + uy * HUB_R,
    x2: sat.cx - ux * SAT_R,
    y2: sat.cy - uy * SAT_R,
  };
}

export function AndroidPhoneMockup() {
  const [zoomedId, setZoomedId] = useState<NodeId | null>(null);
  const satellites = NODES.filter((n) => !n.isHub);

  return (
    <div className="relative mx-auto w-full max-w-[330px] select-none">
      {/* Subtle Ambient Glow */}
      <div
        aria-hidden="true"
        className="pointer-events-none absolute -inset-6 rounded-[3rem] bg-gradient-to-tr from-indigo-500/20 via-violet-500/15 to-sky-500/10 blur-2xl dark:from-indigo-500/25 dark:via-purple-500/20 dark:to-transparent"
      />

      {/* Android Smartphone Shell */}
      <div className="relative overflow-hidden rounded-[2.6rem] border-[7px] border-slate-900 bg-white shadow-2xl dark:border-zinc-800 dark:bg-[#111116]">
        {/* Top Android Camera Punch-Hole + Status Bar */}
        <div className="relative z-10 flex items-center justify-between bg-white px-6 pb-1.5 pt-3 text-[11px] font-medium text-slate-600 dark:bg-[#111116] dark:text-zinc-400">
          <span className="font-mono">09:41</span>
          <div
            aria-hidden="true"
            className="h-3.5 w-3.5 rounded-full bg-slate-900 ring-2 ring-slate-800/30 dark:bg-black dark:ring-zinc-700/60"
          />
          <div className="flex items-center gap-1.5">
            <Wifi className="h-3 w-3" />
            <BatteryCharging className="h-3.5 w-3.5" />
          </div>
        </div>

        {/* Zoomable Canvas Stage (1:1 mirror of ZoomableCanvasView + FeatureCircleView) */}
        <div className="relative mx-auto h-[448px] w-[306px] overflow-hidden bg-white dark:bg-[#111116]">
          {/* Subtle hint pill when overview is visible */}
          <motion.div
            animate={{ opacity: zoomedId ? 0 : 1 }}
            transition={{ duration: 0.2 }}
            className="pointer-events-none absolute inset-x-0 top-2.5 flex justify-center"
          >
            <span className="inline-flex items-center gap-1.5 rounded-full border border-slate-200 bg-slate-50 px-2.5 py-0.5 text-[10px] font-medium text-slate-600 dark:border-zinc-800 dark:bg-zinc-900 dark:text-zinc-300">
              <Lock className="h-2.5 w-2.5 text-emerald-500" />
              Custos · Touchez un cercle pour zoomer
            </span>
          </motion.div>

          {/* SVG Connector Lines + Endpoint Dots (fade out on zoomInto) */}
          <motion.svg
            viewBox="0 0 306 448"
            className="pointer-events-none absolute inset-0 h-full w-full"
            animate={{ opacity: zoomedId ? 0 : 1 }}
            transition={{ duration: 0.22 }}
            aria-hidden="true"
          >
            {satellites.map((sat) => {
              const line = computeEdgeLine(sat);
              return (
                <g key={sat.id}>
                  <line
                    x1={line.x1}
                    y1={line.y1}
                    x2={line.x2}
                    y2={line.y2}
                    className="stroke-slate-300 dark:stroke-zinc-700"
                    strokeWidth="1.5"
                    strokeLinecap="round"
                  />
                  <circle
                    cx={line.x1}
                    cy={line.y1}
                    r="2.4"
                    className="fill-slate-400 dark:fill-zinc-500"
                  />
                  <circle
                    cx={line.x2}
                    cy={line.y2}
                    r="2.4"
                    className="fill-slate-400 dark:fill-zinc-500"
                  />
                </g>
              );
            })}
          </motion.svg>

          {/* Single-Element Morphing Feature Circles (morphProgress: 0 -> 1) */}
          {NODES.map((node) => {
            const isZoomed = zoomedId === node.id;
            const isOtherZoomed = zoomedId !== null && !isZoomed;
            const baseLeft = node.cx - node.size / 2;
            const baseTop = node.cy - node.size / 2;

            return (
              <motion.div
                key={node.id}
                initial={false}
                animate={{
                  left: isZoomed ? 10 : baseLeft,
                  top: isZoomed ? 10 : baseTop,
                  width: isZoomed ? 286 : node.size,
                  height: isZoomed ? 428 : node.size,
                  borderRadius: isZoomed ? 24 : 999,
                  opacity: isOtherZoomed ? 0 : 1,
                  scale: isOtherZoomed ? 0.92 : 1,
                }}
                transition={{
                  duration: 0.44,
                  ease: EASE_CURVE,
                }}
                style={{
                  zIndex: isZoomed ? 30 : 10,
                  pointerEvents: isOtherZoomed ? "none" : "auto",
                }}
                className="absolute overflow-hidden border-[1.75px] border-slate-900 bg-white shadow-sm dark:border-zinc-300 dark:bg-[#14141A]"
              >
                {!isZoomed ? (
                  <button
                    type="button"
                    onClick={() => setZoomedId(node.id)}
                    aria-label={`Ouvrir ${node.title}`}
                    className="flex h-full w-full flex-col items-center justify-center px-1.5 text-center transition-colors hover:bg-slate-50/70 dark:hover:bg-zinc-900/50"
                  >
                    <span
                      className={node.isHub ? "text-base" : "text-sm"}
                      aria-hidden="true"
                    >
                      {node.emoji}
                    </span>
                    <span
                      className={`mt-0.5 font-bold leading-tight text-slate-950 dark:text-white ${
                        node.isHub ? "text-[11px]" : "text-[9.5px]"
                      }`}
                    >
                      {node.title}
                    </span>
                    <span
                      className={`mt-0.5 leading-tight ${
                        node.isHub ? "text-[8.5px]" : "text-[8px]"
                      } ${node.summaryColorClass}`}
                    >
                      {node.subtitle}
                    </span>
                  </button>
                ) : (
                  <motion.div
                    initial={{ opacity: 0, y: 6 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.24, delay: 0.16 }}
                    className="flex h-full w-full flex-col justify-between p-4"
                  >
                    <div>
                      <div className="flex items-center justify-between border-b border-slate-100 pb-3 dark:border-zinc-800">
                        <button
                          type="button"
                          onClick={() => setZoomedId(null)}
                          className="inline-flex items-center gap-1 rounded-lg border border-slate-200 bg-slate-50 px-2.5 py-1 text-[10px] font-semibold text-slate-700 transition-colors hover:bg-slate-100 dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-200 dark:hover:bg-zinc-800"
                        >
                          <ArrowLeft className="h-3 w-3" />
                          <span>Pentagone</span>
                        </button>
                        <span className="text-base" aria-hidden="true">
                          {node.emoji}
                        </span>
                      </div>

                      <h3 className="mt-3 text-center text-sm font-bold text-slate-950 dark:text-white">
                        {node.detailHeadline}
                      </h3>
                      <p className="mt-1.5 text-center text-[11px] leading-relaxed text-slate-600 dark:text-zinc-400">
                        {node.detailSub}
                      </p>

                      <button
                        type="button"
                        onClick={() => setZoomedId(null)}
                        className={`mt-4 w-full rounded-xl px-3 py-2 text-[11px] font-semibold text-white transition-opacity hover:opacity-90 ${
                          node.ctaStyle === "red"
                            ? "bg-rose-600"
                            : "bg-slate-950 dark:bg-indigo-600"
                        }`}
                      >
                        {node.ctaLabel}
                      </button>

                      <div className="mt-4 space-y-2">
                        {node.rows.map((row) => (
                          <div
                            key={row.title}
                            className="flex items-center justify-between rounded-xl border border-slate-200/80 bg-slate-50/80 px-3 py-2 dark:border-zinc-800 dark:bg-zinc-900/70"
                          >
                            <div className="min-w-0 pr-2">
                              <p className="truncate text-[11px] font-semibold text-slate-900 dark:text-zinc-100">
                                {row.title}
                              </p>
                              <p className="truncate text-[9.5px] text-slate-500 dark:text-zinc-400">
                                {row.meta}
                              </p>
                            </div>
                            <span className="shrink-0 rounded-md bg-white px-2 py-0.5 font-mono text-[9.5px] font-semibold text-slate-800 shadow-sm dark:bg-zinc-800 dark:text-zinc-200">
                              {row.status}
                            </span>
                          </div>
                        ))}
                      </div>
                    </div>

                    <p className="mt-3 text-center text-[10px] text-slate-400 dark:text-zinc-500">
                      Appuyez sur « Pentagone » pour dézoomer
                    </p>
                  </motion.div>
                )}
              </motion.div>
            );
          })}
        </div>

        {/* Bottom Android Gesture Pill */}
        <div className="flex justify-center bg-white pb-2.5 pt-1 dark:bg-[#111116]">
          <div className="h-1 w-24 rounded-full bg-slate-300 dark:bg-zinc-700" />
        </div>
      </div>
    </div>
  );
}
