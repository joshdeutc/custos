import type { Metadata } from "next";
import Link from "next/link";
import { CheckCircle2, ShieldCheck } from "lucide-react";
import { LegalPageShell } from "@/components/LegalPageShell";
import { SITE_CONFIG } from "@/lib/constants";

export const metadata: Metadata = {
  title: "Politique de confidentialité (RGPD)",
  description:
    "Politique de confidentialité officielle de l’application Android Custos : traitement 100 % local, aucune collecte serveur, transparence totale sur les permissions Android et droits RGPD.",
  alternates: {
    canonical: "/privacy",
  },
  openGraph: {
    type: "website",
    locale: "fr_FR",
    url: "/privacy",
    siteName: "Custos",
    title: "Politique de confidentialité (RGPD) | Custos",
    description:
      "Traitement 100 % local sur Android, zéro traceur, transparence sur les permissions et droits RGPD de Custos.",
  },
  twitter: {
    card: "summary",
    title: "Politique de confidentialité (RGPD) | Custos",
    description:
      "Traitement 100 % local sur Android, zéro collecte serveur et transparence sur les permissions.",
  },
};

const ANDROID_PERMISSIONS = [
  {
    permission: "Service d’accessibilité (BIND_ACCESSIBILITY_SERVICE)",
    purpose:
      "Identifier l’application affichée au premier plan, renvoyer vers l’écran d’accueil lorsqu’un quota ou un couvre-feu expire, décourager la désactivation impulsive dans les Paramètres Android et détecter localement les onglets ciblés par les règles d’Accès Partiel (ex. YouTube Shorts). Les éléments d’écran sont évalués exclusivement en mémoire vive (RAM) à la volée : aucune capture d’écran, aucune frappe clavier ni contenu personnel n’est enregistré ou transmis.",
  },
  {
    permission: "Accès aux données d’utilisation (PACKAGE_USAGE_STATS)",
    purpose:
      "Mesurer localement le temps passé au premier plan sur les applications que vous avez choisi de limiter, afin de décompter vos budgets quotidiens et vos limites par session.",
  },
  {
    permission: "Visibilité des applications installées (QUERY_ALL_PACKAGES)",
    purpose:
      "Afficher au sein de Custos la liste des applications installées sur votre appareil afin que vous puissiez sélectionner celles soumises à un quota, un couvre-feu ou au Mode Nucléaire. Cette liste reste strictement locale.",
  },
  {
    permission: "Notifications (POST_NOTIFICATIONS)",
    purpose:
      "Afficher sur Android 13 et versions ultérieures la notification persistante requise pour maintenir le service de surveillance en fonctionnement fiable.",
  },
  {
    permission:
      "Accès aux notifications — facultatif (BIND_NOTIFICATION_LISTENER_SERVICE)",
    purpose:
      "Mettre temporairement en sourdine les notifications provenant d’une application actuellement bloquée, puis les rétablir à la fin du blocage. Le contenu des messages n’est jamais sauvegardé.",
  },
  {
    permission: "Ne pas déranger — facultatif (ACCESS_NOTIFICATION_POLICY)",
    purpose:
      "Activer le mode Ne pas déranger pendant une session de Mode Nucléaire lorsque vous activez cette option.",
  },
  {
    permission:
      "Démarrage, alarmes exactes et service au premier plan (RECEIVE_BOOT_COMPLETED, SCHEDULE_EXACT_ALARM, FOREGROUND_SERVICE_SPECIAL_USE, REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)",
    purpose:
      "Relancer la protection après un redémarrage du téléphone et garantir l’exécution régulière des compteurs de temps sans mise en veille prématurée par le système.",
  },
  {
    permission: "Accès réseau technique (INTERNET)",
    purpose:
      "Déclarée uniquement à des fins techniques système sur Android. Custos fonctionne localement hors-ligne et n’envoie aucune donnée utilisateur, analytique ou télémétrique vers des serveurs tiers.",
  },
];

export default function PrivacyPage() {
  const { legal } = SITE_CONFIG;

  return (
    <LegalPageShell
      badge="Conformité RGPD & Google Play"
      title="Politique de confidentialité"
      subtitle="Transparence complète sur le fonctionnement 100 % local de Custos, les permissions Android requises et vos droits au titre du Règlement Général sur la Protection des Données (RGPD)."
    >
      {/* Executive Summary Box */}
      <section className="rounded-2xl border border-indigo-200/80 bg-white p-6 shadow-soft dark:border-indigo-500/30 dark:bg-[#111116]">
        <div className="flex items-center gap-2 text-sm font-semibold text-indigo-700 dark:text-indigo-300">
          <ShieldCheck className="h-4 w-4" />
          <span>En résumé</span>
        </div>
        <ul className="mt-4 space-y-2.5">
          {[
            "Custos fonctionne localement sur votre téléphone sans création de compte distante.",
            "Aucune donnée personnelle, historique de navigation ou statistique d’usage n’est collectée, vendue ou partagée avec des tiers.",
            "L’application n’embarque aucun SDK publicitaire, aucun traceur comportemental et aucun outil d’analytics tiers.",
            "Vous conservez un contrôle total pour exporter, modifier (sous réserve de vos propres délais choisis) ou effacer immédiatement toutes vos données locales.",
          ].map((point) => (
            <li key={point} className="flex items-start gap-2.5">
              <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-emerald-700 dark:text-emerald-400" />
              <span>{point}</span>
            </li>
          ))}
        </ul>
      </section>

      <section className="space-y-3">
        <h2 className="text-xl font-semibold text-slate-950 dark:text-white">
          1. Identité du responsable de traitement
        </h2>
        <p>
          L’application mobile Android <strong>{legal.publisherApp}</strong> (
          <code className="rounded bg-slate-100 px-1.5 py-0.5 font-mono text-xs dark:bg-zinc-800">
            {SITE_CONFIG.packageName}
          </code>
          ) et le présent site officiel (
          <code className="rounded bg-slate-100 px-1.5 py-0.5 font-mono text-xs dark:bg-zinc-800">
            custos.joshuanancey.com
          </code>
          ) sont édités par :
        </p>
        <div className="rounded-xl border border-slate-200 bg-white p-4 text-sm dark:border-zinc-800 dark:bg-[#111116]">
          <p>
            <strong>Responsable de traitement :</strong> {legal.publisherApp} —{" "}
            {legal.publisherDisplayName} ({legal.city})
          </p>
          <p className="mt-1">
            <strong>Contact RGPD / DPO :</strong>{" "}
            <a
              href={`mailto:${legal.email}`}
              className="font-medium text-indigo-600 hover:underline dark:text-indigo-400"
            >
              {legal.email}
            </a>
          </p>
          <p className="mt-1 text-xs text-slate-600 dark:text-zinc-400">
            Mentions légales complètes disponibles dans les{" "}
            <Link
              href="/terms"
              className="font-medium text-indigo-600 hover:underline dark:text-indigo-400"
            >
              Conditions Générales d’Utilisation (/terms)
            </Link>
            .
          </p>
        </div>
      </section>

      <section className="space-y-3">
        <h2 className="text-xl font-semibold text-slate-950 dark:text-white">
          2. Données traitées localement sur votre appareil
        </h2>
        <p>
          Pour assurer le respect des règles que vous configurez vous-même,
          Custos enregistre exclusivement dans l’espace de stockage privé interne
          de l’application sur votre smartphone Android (
          <code className="rounded bg-slate-100 px-1.5 py-0.5 font-mono text-xs dark:bg-zinc-800">
            filesDir
          </code>{" "}
          et{" "}
          <code className="rounded bg-slate-100 px-1.5 py-0.5 font-mono text-xs dark:bg-zinc-800">
            SharedPreferences
          </code>
          ) :
        </p>
        <ul className="list-disc space-y-2 pl-5">
          <li>
            <strong>Vos configurations de discipline :</strong> applications
            limitées, quotas quotidiens, plages de couvre-feu, limites par
            session, réglages de délai anti-impulsion et presets du Mode
            Nucléaire (<code className="font-mono text-xs">limits.json</code>,{" "}
            <code className="font-mono text-xs">delay_config.json</code>,{" "}
            <code className="font-mono text-xs">nuclear_presets.json</code>).
          </li>
          <li>
            <strong>Vos règles d’Accès Partiel :</strong> nom du package cible
            et identifiants techniques de composants d’interface (view IDs)
            permettant de reconnaître un onglet bloqué — jamais le contenu des
            messages, photos ou vidéos affichés à l’écran (
            <code className="font-mono text-xs">screen_rules.json</code>).
          </li>
          <li>
            <strong>L’état courant de la journée :</strong> temps consommé sur
            les applications limitées et sessions utilisées pour le suivi des
            quotas jusqu’à la réinitialisation quotidienne (
            <code className="font-mono text-xs">usage_state.json</code>).
          </li>
          <li>
            <strong>Journal d’événements local :</strong> désactivé par défaut (
            <code className="font-mono text-xs">EVENT_LOG_ENABLED = false</code>
            ) dans les versions publiques de l’application afin qu’aucun
            historique d’activité inter-applications ne soit conservé sur disque.
          </li>
        </ul>
        <p>
          <strong>Sauvegarde automatique Android :</strong> sur les appareils où
          Android Auto Backup est activé par l’utilisateur dans les réglages
          Google/Android, le système d’exploitation peut chiffrer et sauvegarder
          les fichiers de configuration locaux vers le compte Google personnel de
          l’utilisateur conformément aux paramètres de confidentialité Android.
          Custos n’accède jamais à cette sauvegarde.
        </p>
      </section>

      <section className="space-y-4">
        <h2 className="text-xl font-semibold text-slate-950 dark:text-white">
          3. Permissions Android et divulgation API AccessibilityService
        </h2>
        <p>
          Conformément aux règles du Google Play Store, chaque autorisation
          sensible est présentée ci-dessous avec sa finalité stricte. Aucune
          permission n’est utilisée à des fins publicitaires, d’espionnage ou de
          monétisation.
        </p>

        <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white dark:border-zinc-800 dark:bg-[#111116]">
          <div className="divide-y divide-slate-200 dark:divide-zinc-800">
            {ANDROID_PERMISSIONS.map((row) => (
              <div key={row.permission} className="p-5">
                <h3 className="font-mono text-xs font-semibold text-indigo-700 sm:text-sm dark:text-indigo-400">
                  {row.permission}
                </h3>
                <p className="mt-2 text-sm leading-relaxed text-slate-600 dark:text-zinc-400">
                  {row.purpose}
                </p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="space-y-3">
        <h2 className="text-xl font-semibold text-slate-950 dark:text-white">
          4. Échanges support, Early Access et hébergement web
        </h2>
        <ul className="list-disc space-y-2 pl-5">
          <li>
            <strong>Support &amp; Programme Early Access Google Play :</strong>{" "}
            lorsque vous nous écrivez volontairement à{" "}
            <a
              href={`mailto:${legal.email}`}
              className="font-medium text-indigo-600 hover:underline dark:text-indigo-400"
            >
              {legal.email}
            </a>{" "}
            (par exemple pour tester l’accès anticipé Google Play ou poser une
            question technique), nous traitons uniquement votre adresse e-mail et
            le contenu de votre message sur la base de l’exécution de votre
            demande / intérêt légitime de support (Art. 6.1.b / 6.1.f RGPD).
          </li>
          <li>
            <strong>Hébergement du site vitrine :</strong> le site{" "}
            <code className="font-mono text-xs">custos.joshuanancey.com</code>{" "}
            est hébergé par <strong>{legal.hostName}</strong> ({legal.hostAddress}
            ). Ce site ne dépose aucun cookie publicitaire ni traceur d’audience.
            Comme tout hébergeur web, Vercel peut traiter des journaux techniques
            de connexion (adresse IP, horodatage, user-agent) exclusivement pour
            la sécurité et la continuité du service (voir la{" "}
            <a
              href={legal.hostUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="text-indigo-600 hover:underline dark:text-indigo-400"
            >
              politique de confidentialité de Vercel
            </a>
            ).
          </li>
        </ul>
      </section>

      <section className="space-y-3">
        <h2 className="text-xl font-semibold text-slate-950 dark:text-white">
          5. Partage avec des tiers et conservation des données
        </h2>
        <p>
          <strong>Partage :</strong> Custos ne vend, ne loue et ne transfère
          aucune donnée utilisateur à des courtiers de données, régies
          publicitaires ou tiers analytiques.
        </p>
        <p>
          <strong>Durée de conservation :</strong> Les configurations stockées sur
          votre téléphone restent présentes uniquement tant que l’application est
          installée et sont supprimées immédiatement lors du nettoyage du
          stockage ou de la désinstallation de Custos. Les e-mails adressés au
          support sont conservés pour la durée strictement nécessaire au suivi
          de votre demande, et au maximum <strong>36 mois</strong> après le
          dernier échange, sauf demande d’effacement anticipée.
        </p>
      </section>

      <section className="space-y-3">
        <h2 className="text-xl font-semibold text-slate-950 dark:text-white">
          6. Vos droits RGPD et suppression des données
        </h2>
        <p>
          Conformément au RGPD et à la loi Informatique et Libertés, vous
          disposez des droits d’accès, de rectification, d’effacement, de
          limitation, d’opposition et de portabilité sur les éventuelles données
          de contact que vous nous auriez transmises.
        </p>
        <p>
          Pour exercer vos droits ou demander la suppression définitive de toute
          donnée associée, consultez notre page dédiée{" "}
          <Link
            href="/delete-account"
            className="font-semibold text-indigo-600 hover:underline dark:text-indigo-400"
          >
            Suppression de compte et des données (/delete-account)
          </Link>{" "}
          ou écrivez directement à{" "}
          <a
            href={`mailto:${legal.email}`}
            className="font-semibold text-indigo-600 hover:underline dark:text-indigo-400"
          >
            {legal.email}
          </a>
          . Vous disposez également du droit d’introduire une réclamation auprès
          de la{" "}
          <a
            href="https://www.cnil.fr"
            target="_blank"
            rel="noopener noreferrer"
            className="text-indigo-600 hover:underline dark:text-indigo-400"
          >
            CNIL (cnil.fr)
          </a>
          .
        </p>
      </section>

      <section className="space-y-3">
        <h2 className="text-xl font-semibold text-slate-950 dark:text-white">
          7. Mineurs &amp; Évolutions de cette politique
        </h2>
        <p>
          Custos ne collecte volontairement aucune donnée personnelle auprès des
          mineurs. En cas d’évolution fonctionnelle impliquant un traitement
          nouveau, la présente page sera mise à jour avec une nouvelle date en
          haut du document avant toute mise en ligne.
        </p>
      </section>
    </LegalPageShell>
  );
}
