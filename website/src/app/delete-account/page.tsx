import type { Metadata } from "next";
import Link from "next/link";
import { Clock, Mail, Smartphone, Trash2 } from "lucide-react";
import { LegalPageShell } from "@/components/LegalPageShell";
import { SITE_CONFIG } from "@/lib/constants";

export const metadata: Metadata = {
  title: "Suppression de compte et des données",
  description:
    "Procédure officielle pour supprimer votre compte Early Access et toutes vos données liées à l’application Android Custos (Google Play Data Safety).",
  alternates: {
    canonical: "/delete-account",
  },
  openGraph: {
    type: "website",
    locale: "fr_FR",
    url: "/delete-account",
    siteName: "Custos",
    title: "Suppression de compte et des données | Custos",
    description:
      "Procédure officielle pour effacer vos données locales Android ou demander la suppression de votre e-mail Early Access Custos.",
  },
  twitter: {
    card: "summary",
    title: "Suppression de compte et des données | Custos",
    description:
      "Procédure simple et transparente pour supprimer vos données liées à l’application Android Custos.",
  },
};

export default function DeleteAccountPage() {
  const { legal } = SITE_CONFIG;

  const deletionMailto = `mailto:${legal.email}?subject=${encodeURIComponent(
    "[Custos] Demande de suppression de compte et de données"
  )}&body=${encodeURIComponent(
    "Bonjour,\n\nJe demande la suppression définitive de mon compte / inscription Early Access et de toutes les données associées à l'application Custos.\n\nAdresse e-mail concernée : \nDétails complémentaires (facultatif) : \n\nMerci."
  )}`;

  return (
    <LegalPageShell
      badge="Google Play Sécurité des données · RGPD Art. 17"
      title="Suppression de compte et des données"
      subtitle="Procédure simple et transparente pour effacer vos données locales sur Android ou demander par e-mail la suppression définitive de toute information liée à votre accès Early Access ou support."
    >
      {/* Important Clarification Banner */}
      <section className="rounded-2xl border border-emerald-200 bg-emerald-50/70 p-6 dark:border-emerald-500/30 dark:bg-emerald-500/10">
        <h2 className="text-base font-semibold text-emerald-950 dark:text-emerald-200">
          À savoir : Custos fonctionne sans création de compte cloud
        </h2>
        <p className="mt-2 text-sm leading-relaxed text-emerald-900 dark:text-emerald-100/90">
          L’application mobile Custos ne nécessite pas la création d’un compte
          sur nos serveurs : toutes vos limites d’applications, couvre-feux et
          statistiques restent stockés localement sur votre téléphone. Si vous
          avez communiqué une adresse e-mail dans le cadre du{" "}
          <strong>programme Early Access Google Play</strong> ou du{" "}
          <strong>support technique</strong>, vous pouvez demander leur
          suppression définitive à tout moment selon la procédure ci-dessous.
        </p>
      </section>

      {/* Method 1: Remote Account / Email Data Deletion via Email */}
      <section className="space-y-5">
        <div className="flex items-center gap-2.5">
          <div className="inline-flex h-9 w-9 items-center justify-center rounded-xl bg-indigo-500/10 text-indigo-600 dark:text-indigo-400">
            <Mail className="h-5 w-5" />
          </div>
          <h2 className="text-xl font-semibold text-slate-950 dark:text-white">
            1. Demander la suppression définitive de votre compte / e-mail
          </h2>
        </div>

        <p>
          Suivez ces étapes pour demander la suppression complète de votre
          adresse e-mail d’accès anticipé (Google Play Closed Testing) et de tout
          historique d’échanges auprès de l’éditeur{" "}
          <strong>
            {legal.publisherApp} — {legal.publisherDisplayName} ({legal.city})
          </strong>{" "}
          ({legal.email}) :
        </p>

        <ol className="space-y-4">
          {[
            {
              step: "Étape 1",
              title: "Rédiger un e-mail depuis l’adresse concernée",
              text: `Envoyez un e-mail à ${legal.email} en utilisant de préférence l’adresse e-mail associée à votre demande d’Early Access ou à vos échanges avec le support.`,
            },
            {
              step: "Étape 2",
              title: "Indiquer l’objet de la demande",
              text: "Utilisez l’objet : « [Custos] Demande de suppression de compte et de données » afin d’assurer un traitement prioritaire.",
            },
            {
              step: "Étape 3",
              title: "Préciser les éléments à supprimer",
              text: "Confirmez l’adresse e-mail Google Play ou support concernée ainsi que, si applicable, votre souhait de quitter la liste des testeurs Early Access Custos.",
            },
            {
              step: "Étape 4",
              title: "Accusé de réception et exécution définitive",
              text: "Vous recevez un accusé de réception sous 48 heures ouvrées. La suppression définitive est exécutée sous 7 jours ouvrés maximum (bien en deçà du délai légal RGPD d’un mois) et confirmée par retour d’e-mail.",
            },
          ].map((item) => (
            <li
              key={item.step}
              className="rounded-xl border border-slate-200 bg-white p-5 shadow-soft dark:border-zinc-800 dark:bg-[#111116]"
            >
              <span className="inline-flex rounded-md bg-indigo-500/10 px-2 py-0.5 font-mono text-xs font-semibold text-indigo-700 dark:text-indigo-300">
                {item.step}
              </span>
              <h3 className="mt-2 text-base font-semibold text-slate-950 dark:text-white">
                {item.title}
              </h3>
              <p className="mt-1 text-sm leading-relaxed text-slate-600 dark:text-zinc-400">
                {item.text}
              </p>
            </li>
          ))}
        </ol>

        <div className="flex flex-col gap-3 rounded-2xl border border-indigo-200 bg-white p-6 sm:flex-row sm:items-center sm:justify-between dark:border-indigo-500/30 dark:bg-[#111116]">
          <div>
            <p className="font-semibold text-slate-950 dark:text-white">
              Envoyer une demande en un clic
            </p>
            <p className="mt-1 flex items-center gap-1.5 text-xs text-slate-600 dark:text-zinc-400">
              <Clock className="h-3.5 w-3.5 text-indigo-600 dark:text-indigo-400" />
              <span>Délai de traitement : sous 7 jours ouvrés maximum</span>
            </p>
          </div>
          <a
            href={deletionMailto}
            className="inline-flex items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-indigo-600 to-purple-600 px-5 py-3 text-sm font-semibold text-white shadow-sm transition-all hover:brightness-110"
          >
            <Trash2 className="h-4 w-4" />
            <span>Écrire à {legal.email}</span>
          </a>
        </div>
      </section>

      {/* Method 2: Immediate On-Device Data Deletion */}
      <section className="space-y-4">
        <div className="flex items-center gap-2.5">
          <div className="inline-flex h-9 w-9 items-center justify-center rounded-xl bg-slate-200/70 text-slate-700 dark:bg-zinc-800 dark:text-zinc-300">
            <Smartphone className="h-5 w-5" />
          </div>
          <h2 className="text-xl font-semibold text-slate-950 dark:text-white">
            2. Effacer immédiatement vos données locales sur Android
          </h2>
        </div>

        <p>
          Pour supprimer instantanément 100 % des règles, quotas, préférences et
          états stockés sur votre téléphone :
        </p>

        <div className="rounded-xl border border-slate-200 bg-white p-5 dark:border-zinc-800 dark:bg-[#111116]">
          <ol className="list-decimal space-y-2 pl-5 text-sm text-slate-700 dark:text-zinc-300">
            <li>
              Ouvrez les <strong>Paramètres Android</strong> de votre appareil.
            </li>
            <li>
              Accédez à <strong>Applications</strong> → <strong>Custos</strong>{" "}
              → <strong>Espace de stockage et cache</strong>.
            </li>
            <li>
              Appuyez sur <strong>Supprimer les données</strong> (ou désinstallez
              simplement l’application).
            </li>
          </ol>
          <p className="mt-3 text-xs text-slate-600 dark:text-zinc-400">
            Note : si vous avez activé la sauvegarde Google Drive / Android Auto
            Backup sur votre appareil, vous pouvez également supprimer la
            sauvegarde de l’application dans les paramètres de votre compte
            Google.
          </p>
        </div>
      </section>

      {/* Summary of Deleted vs Retained Data */}
      <section className="space-y-3">
        <h2 className="text-xl font-semibold text-slate-950 dark:text-white">
          3. Périmètre des données supprimées et exceptions légales
        </h2>
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="rounded-xl border border-slate-200 bg-white p-5 dark:border-zinc-800 dark:bg-[#111116]">
            <h3 className="text-sm font-semibold text-emerald-700 dark:text-emerald-400">
              Données définitivement effacées
            </h3>
            <ul className="mt-2 list-disc space-y-1.5 pl-4 text-xs leading-relaxed text-slate-600 dark:text-zinc-400">
              <li>
                Adresse e-mail inscrite aux tests fermés / Early Access Google
                Play.
              </li>
              <li>
                Intégralité des messages et pièces échangées avec{" "}
                {legal.email}.
              </li>
              <li>
                Sur votre téléphone : l’ensemble des fichiers JSON et
                préférences locales lors de la désinstallation.
              </li>
            </ul>
          </div>

          <div className="rounded-xl border border-slate-200 bg-white p-5 dark:border-zinc-800 dark:bg-[#111116]">
            <h3 className="text-sm font-semibold text-slate-900 dark:text-zinc-200">
              Durées techniques résiduelles
            </h3>
            <ul className="mt-2 list-disc space-y-1.5 pl-4 text-xs leading-relaxed text-slate-600 dark:text-zinc-400">
              <li>
                Journaux techniques transitoires de sécurité web de l’hébergeur (
                {legal.hostName}), purgés automatiquement selon ses fenêtres de
                rétention standards.
              </li>
              <li>
                Données strictement imposées par une obligation légale française
                en cas de facturation commerciale future (non applicable aux
                utilisateurs gratuits).
              </li>
            </ul>
          </div>
        </div>
        <p className="pt-2 text-xs text-slate-600 dark:text-zinc-400">
          Voir également :{" "}
          <Link
            href="/privacy"
            className="text-indigo-600 hover:underline dark:text-indigo-400"
          >
            Politique de confidentialité
          </Link>{" "}
          ·{" "}
          <Link
            href="/terms"
            className="text-indigo-600 hover:underline dark:text-indigo-400"
          >
            Conditions Générales d’Utilisation
          </Link>
          .
        </p>
      </section>
    </LegalPageShell>
  );
}
