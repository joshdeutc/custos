"use client";

import React, { useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import {
  BatteryCharging,
  CalendarClock,
  Clock,
  Flame,
  Lock,
  MoonStar,
  Scissors,
  Wifi,
} from "lucide-react";

type NodeId = "delay" | "limits" | "curfew" | "partial" | "nuclear" | "schedule";

interface HubNode {
  id: NodeId;
  title: string;
  subtitle: string;
  badge: string;
  detailTitle: string;
  detailDesc: string;
  metrics: { label: string; value: string }[];
  positionClass: string;
  accentClass: string;
}

const HUB_NODES: HubNode[] = [
  {
    id: "limits",
    title: "App Limits",
    subtitle: "30m / jour",
    badge: "Quota & Sessions",
    detailTitle: "Quotas quotidiens & sessions",
    detailDesc:
      "Comptabilisation locale au second près via UsageStats. Reset logique configuré à 02h00.",
    metrics: [
      { label: "Instagram", value: "24 / 30 min" },
      { label: "Discord", value: "Session 5 min · Cooldown 4h" },
    ],
    positionClass: "left-3 top-5",
    accentClass: "text-indigo-600 dark:text-indigo-400",
  },
  {
    id: "curfew",
    title: "Curfew",
    subtitle: "22h → 07h",
    badge: "Couvre-feu actif",
    detailTitle: "Plages horaires strictes",
    detailDesc:
      "Blocage prioritaire sur les quotas avec mise en sourdine des notifications pendant le sommeil.",
    metrics: [
      { label: "Soir & Nuit", value: "22:00 → 07:00" },
      { label: "Notifications", value: "Mises en pause" },
    ],
    positionClass: "right-3 top-5",
    accentClass: "text-violet-600 dark:text-violet-400",
  },
  {
    id: "partial",
    title: "Partial Access",
    subtitle: "In-App",
    badge: "Blocage ciblé",
    detailTitle: "Filtrage d’onglets addictifs",
    detailDesc:
      "Bloque uniquement les flux Shorts ou Actualités sans fermer la messagerie ou la recherche.",
    metrics: [
      { label: "YouTube", value: "Shorts bloqués" },
      { label: "WhatsApp", value: "Onglet Chaînes filtré" },
    ],
    positionClass: "left-2 bottom-6",
    accentClass: "text-sky-600 dark:text-sky-400",
  },
  {
    id: "nuclear",
    title: "Nuclear",
    subtitle: "Focus Total",
    badge: "Mode Nucléaire",
    detailTitle: "Verrouillage irréversible à durée fixe",
    detailDesc:
      "Lance un preset Deep Work de 30 min à 4h avec activation automatique de Ne pas déranger.",
    metrics: [
      { label: "Preset actif", value: "Deep Work · 2h00" },
      { label: "Annulation", value: "Verrouillée jusqu’à fin" },
    ],
    positionClass: "right-2 bottom-6",
    accentClass: "text-rose-600 dark:text-rose-400",
  },
  {
    id: "schedule",
    title: "Plages & Jours",
    subtitle: "Horaires",
    badge: "Jours & Créneaux",
    detailTitle: "Jours et plages horaires autorisés",
    detailDesc:
      "Définissez quels jours de la semaine et entre quelles heures chaque application peut être ouverte.",
    metrics: [
      { label: "Semaine (Lun–Ven)", value: "18:00 → 21:30" },
      { label: "Fonctionnement", value: "Sans root · 100 % local" },
    ],
    positionClass: "left-1/2 -translate-x-1/2 top-0",
    accentClass: "text-emerald-600 dark:text-emerald-400",
  },
];

const CENTER_NODE: HubNode = {
  id: "delay",
  title: "Delay & Limits",
  subtitle: "Délai 12h actif",
  badge: "Asymétrie active",
  detailTitle: "Délai constitutionnel anti-impulsion",
  detailDesc:
    "Durcir une règle est immédiat. Alléger ou supprimer une limite attend la fin du compte à rebours.",
  metrics: [
    { label: "Durcissement", value: "Instantané (0s)" },
    { label: "Allègement", value: "Délai de sécurité 12h" },
  ],
  positionClass: "",
  accentClass: "text-indigo-600 dark:text-indigo-400",
};

function NodeIcon({ id, className }: { id: NodeId; className?: string }) {
  switch (id) {
    case "limits":
      return <Clock className={className} />;
    case "curfew":
      return <MoonStar className={className} />;
    case "partial":
      return <Scissors className={className} />;
    case "nuclear":
      return <Flame className={className} />;
    case "schedule":
      return <CalendarClock className={className} />;
    default:
      return <Lock className={className} />;
  }
}

export function AndroidPhoneMockup() {
  const [selectedId, setSelectedId] = useState<NodeId>("delay");

  const activeNode =
    selectedId === "delay"
      ? CENTER_NODE
      : HUB_NODES.find((n) => n.id === selectedId) ?? CENTER_NODE;

  return (
    <div className="relative mx-auto w-full max-w-[340px] select-none">
      {/* Subtle Ambient Glow */}
      <div
        aria-hidden="true"
        className="pointer-events-none absolute -inset-6 rounded-[3rem] bg-gradient-to-tr from-indigo-500/20 via-violet-500/15 to-sky-500/10 blur-2xl dark:from-indigo-500/25 dark:via-purple-500/20 dark:to-transparent"
      />

      {/* Android Device Shell */}
      <div className="relative overflow-hidden rounded-[2.6rem] border-[7px] border-slate-900 bg-white shadow-2xl dark:border-zinc-800 dark:bg-[#101014]">
        {/* Top Android Camera Punch-Hole + Status Bar */}
        <div className="relative flex items-center justify-between px-6 pb-2 pt-3 text-[11px] font-medium text-slate-600 dark:text-zinc-400">
          <span className="font-mono">09:41</span>
          <div
            aria-hidden="true"
            className="h-3.5 w-3.5 rounded-full bg-slate-900 ring-2 ring-slate-800/40 dark:bg-black dark:ring-zinc-700/60"
          />
          <div className="flex items-center gap-1.5">
            <Wifi className="h-3 w-3" />
            <BatteryCharging className="h-3.5 w-3.5" />
          </div>
        </div>

        {/* App Header inside Screen */}
        <div className="flex items-center justify-between border-b border-slate-100 px-5 pb-3 pt-1 dark:border-zinc-800/80">
          <div>
            <p className="text-xs font-semibold text-slate-900 dark:text-white">
              Custos
            </p>
            <p className="text-[10px] text-emerald-600 dark:text-emerald-400">
              ● Service d’accessibilité actif
            </p>
          </div>
          <span className="rounded-full border border-slate-200 bg-slate-50 px-2 py-0.5 font-mono text-[10px] font-medium text-slate-600 dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-300">
            Local-only
          </span>
        </div>

        {/* Interactive Pentagon Hub (faithful to ZoomableCanvasView / FeatureCircleView) */}
        <div className="relative h-[250px] px-2 pt-2">
          {/* Connector SVG lines linking Hub Center to 5 Satellite Nodes */}
          <svg
            viewBox="0 0 300 230"
            className="pointer-events-none absolute inset-0 h-full w-full"
            aria-hidden="true"
          >
            <g className="stroke-slate-200 dark:stroke-zinc-800" strokeWidth="1.5">
              <line x1="150" y1="122" x2="150" y2="30" />
              <line x1="150" y1="122" x2="54" y2="56" />
              <line x1="150" y1="122" x2="246" y2="56" />
              <line x1="150" y1="122" x2="58" y2="190" />
              <line x1="150" y1="122" x2="242" y2="190" />
            </g>
          </svg>

          {/* 5 Satellites */}
          {HUB_NODES.map((node) => {
            const isSelected = selectedId === node.id;
            return (
              <motion.button
                key={node.id}
                type="button"
                whileHover={{ scale: 1.04 }}
                whileTap={{ scale: 0.96 }}
                onClick={() => setSelectedId(node.id)}
                className={`absolute ${node.positionClass} flex w-[88px] flex-col items-center rounded-2xl border px-1.5 py-2 text-center transition-all ${
                  isSelected
                    ? "border-indigo-500 bg-indigo-50/90 shadow-sm dark:border-indigo-400 dark:bg-indigo-500/15"
                    : "border-slate-200 bg-white/95 hover:border-slate-300 dark:border-zinc-800 dark:bg-zinc-900/95 dark:hover:border-zinc-700"
                }`}
              >
                <NodeIcon id={node.id} className={`h-3.5 w-3.5 ${node.accentClass}`} />
                <span className="mt-1 text-[10px] font-semibold leading-tight text-slate-900 dark:text-zinc-100">
                  {node.title}
                </span>
                <span className="text-[9px] text-slate-500 dark:text-zinc-400">
                  {node.subtitle}
                </span>
              </motion.button>
            );
          })}

          {/* Central Hub Circle (Delay & Limits) */}
          <motion.button
            type="button"
            whileHover={{ scale: 1.04 }}
            whileTap={{ scale: 0.96 }}
            onClick={() => setSelectedId("delay")}
            className={`absolute left-1/2 top-[122px] flex h-[92px] w-[92px] -translate-x-1/2 -translate-y-1/2 flex-col items-center justify-center rounded-full border-2 p-2 text-center transition-all ${
              selectedId === "delay"
                ? "border-indigo-600 bg-white shadow-glow dark:border-indigo-400 dark:bg-zinc-900"
                : "border-slate-900 bg-white hover:border-indigo-500 dark:border-zinc-700 dark:bg-zinc-900"
            }`}
          >
            <Lock className="h-4 w-4 text-indigo-600 dark:text-indigo-400" />
            <span className="mt-1 text-[11px] font-bold leading-tight text-slate-900 dark:text-white">
              Delay &amp; Limits
            </span>
            <span className="mt-0.5 text-[9px] font-medium text-indigo-600 dark:text-indigo-400">
              12h verrouillé
            </span>
          </motion.button>
        </div>

        {/* Dynamic Detail Drawer inside Phone */}
        <div className="border-t border-slate-100 bg-slate-50/90 p-4 dark:border-zinc-800/80 dark:bg-zinc-950/80">
          <AnimatePresence mode="wait">
            <motion.div
              key={activeNode.id}
              initial={{ opacity: 0, y: 6 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -6 }}
              transition={{ duration: 0.2, ease: [0.22, 1, 0.36, 1] as const }}
            >
              <div className="flex items-center justify-between">
                <span className="inline-flex items-center gap-1 rounded-md bg-indigo-500/10 px-2 py-0.5 text-[10px] font-semibold text-indigo-600 dark:text-indigo-300">
                  {activeNode.badge}
                </span>
                <span className="text-[10px] text-slate-400 dark:text-zinc-500">
                  Touchez une bulle ↑
                </span>
              </div>

              <h3 className="mt-2 text-xs font-semibold text-slate-900 dark:text-white">
                {activeNode.detailTitle}
              </h3>
              <p className="mt-1 text-[11px] leading-relaxed text-slate-600 dark:text-zinc-400">
                {activeNode.detailDesc}
              </p>

              <div className="mt-3 space-y-1.5">
                {activeNode.metrics.map((m) => (
                  <div
                    key={m.label}
                    className="flex items-center justify-between rounded-lg border border-slate-200/80 bg-white px-2.5 py-1.5 text-[10px] dark:border-zinc-800 dark:bg-zinc-900"
                  >
                    <span className="font-medium text-slate-600 dark:text-zinc-400">
                      {m.label}
                    </span>
                    <span className="font-mono font-semibold text-slate-900 dark:text-zinc-100">
                      {m.value}
                    </span>
                  </div>
                ))}
              </div>
            </motion.div>
          </AnimatePresence>
        </div>

        {/* Bottom Android Gesture Pill */}
        <div className="flex justify-center bg-slate-50/90 pb-2.5 pt-1 dark:bg-zinc-950/80">
          <div className="h-1 w-24 rounded-full bg-slate-300 dark:bg-zinc-700" />
        </div>
      </div>
    </div>
  );
}
