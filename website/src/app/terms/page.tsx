import type { Metadata } from "next";
import Link from "next/link";
import { AlertTriangle } from "lucide-react";
import { LegalPageShell } from "@/components/LegalPageShell";
import { SITE_CONFIG } from "@/lib/constants";

export const metadata: Metadata = {
  title: "Conditions Générales d’Utilisation (CGU)",
  description:
    "Conditions Générales d’Utilisation et mentions légales officielles de l’application mobile Android Custos et du site custos.joshuanancey.com.",
  alternates: {
    canonical: "/terms",
  },
  openGraph: {
    type: "website",
    locale: "fr_FR",
    url: "/terms",
    siteName: "Custos",
    title: "Conditions Générales d’Utilisation (CGU) | Custos",
    description:
      "Mentions légales et Conditions Générales d’Utilisation de l’application mobile Android Custos.",
  },
  twitter: {
    card: "summary",
    title: "Conditions Générales d’Utilisation (CGU) | Custos",
    description:
      "Mentions légales et Conditions Générales d’Utilisation de l’application Android Custos.",
  },
  robots: {
    index: false,
    follow: true,
  },
};

export default function TermsPage() {
  const { legal } = SITE_CONFIG;

  return (
    <LegalPageShell
      badge="Conditions Générales d’Utilisation"
      title="Conditions Générales d’Utilisation"
      subtitle="Règles d’utilisation de l’application Android Custos et du site web custos.joshuanancey.com."
    >
      <section className="space-y-3">
        <h2 className="text-xl font-semibold text-slate-950 dark:text-white">
          1. Éditeur et Mentions Légales
        </h2>
        <p>
          L’application mobile <strong>{legal.publisherApp}</strong> (package{" "}
          <code className="rounded bg-slate-100 px-1.5 py-0.5 font-mono text-xs dark:bg-zinc-800">
            {SITE_CONFIG.packageName}
          </code>
          ) et le site{" "}
          <code className="rounded bg-slate-100 px-1.5 py-0.5 font-mono text-xs dark:bg-zinc-800">
            custos.joshuanancey.com
          </code>{" "}
          sont édités par :
        </p>
        <div className="rounded-xl border border-slate-200 bg-white p-4 text-sm dark:border-zinc-800 dark:bg-[#111116]">
          <p>
            <strong>Éditeur :</strong> {legal.publisherApp} —{" "}
            {legal.legalForm} {legal.legalEntity}
          </p>
          <p className="mt-1">
            <strong>SIRET :</strong>{" "}
            <span className="font-mono">{legal.siret}</span>
          </p>
          <p className="mt-1">
            <strong>Siège social :</strong> {legal.address}
          </p>
          <p className="mt-1">
            <strong>Contact &amp; Support :</strong>{" "}
            <a
              href={`mailto:${legal.email}`}
              className="font-medium text-indigo-600 hover:underline dark:text-indigo-400"
            >
              {legal.email}
            </a>
          </p>
          <p className="mt-1">
            <strong>Hébergeur du site :</strong> {legal.hostName} —{" "}
            {legal.hostAddress}
          </p>
        </div>
      </section>

      <section className="space-y-3">
        <h2 className="text-xl font-semibold text-slate-950 dark:text-white">
          2. Objet de l’application Custos
        </h2>
        <p>
          Custos est un logiciel utilitaire d’auto-discipline et de gestion du
          temps d’écran destiné aux smartphones fonctionnant sous{" "}
          <strong>{SITE_CONFIG.minAndroidVersion}</strong>. Il permet à
          l’utilisateur de configurer volontairement des quotas quotidiens,
          limites par session, couvre-feux, filtres ciblés d’accès partiel,
          listes blanches et sessions de blocage temporaire (Mode Nucléaire).
        </p>
        <p>
          L’installation et l’utilisation de Custos impliquent l’acceptation
          sans réserve des présentes Conditions Générales d’Utilisation ainsi que
          de la{" "}
          <Link
            href="/privacy"
            className="font-medium text-indigo-600 hover:underline dark:text-indigo-400"
          >
            Politique de confidentialité
          </Link>
          .
        </p>
      </section>

      <section className="space-y-3">
        <h2 className="text-xl font-semibold text-slate-950 dark:text-white">
          3. Avertissement relatif aux délais irréversibles et au blocage
        </h2>
        <div className="rounded-2xl border border-amber-200 bg-amber-50/70 p-5 dark:border-amber-500/30 dark:bg-amber-500/10">
          <div className="flex items-start gap-3">
            <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0 text-amber-600 dark:text-amber-400" />
            <div className="space-y-2 text-sm text-amber-950 dark:text-amber-100">
              <p className="font-semibold">
                Engagement volontaire de l’utilisateur
              </p>
              <p>
                Par conception, les mécanismes de protection de Custos (tels que
                le <strong>délai de réflexion avant allègement</strong> ou le{" "}
                <strong>Mode Nucléaire à durée fixe</strong>) empêchent
                volontairement la modification immédiate à la baisse ou la
                levée anticipée d’une restriction tant que le compte à rebours
                choisi par l’utilisateur n’est pas écoulé.
              </p>
              <p>
                Il appartient à l’utilisateur de configurer avec discernement
                ses règles, durées et listes d’applications afin de ne pas
                entraver ses activités professionnelles, personnelles ou
                essentielles.
              </p>
            </div>
          </div>
        </div>
        <p>
          <strong>Sécurité &amp; appels d’urgence :</strong> Custos ne bloque ni
          les fonctions système critiques d’appel téléphonique, ni l’accès aux
          numéros d’urgence, ni la réinitialisation matérielle d’usine native
          d’Android.
        </p>
      </section>

      <section className="space-y-3">
        <h2 className="text-xl font-semibold text-slate-950 dark:text-white">
          4. Conditions d’accès, usage autorisé et gratuité
        </h2>
        <ul className="list-disc space-y-2 pl-5">
          <li>
            Custos ne doit être installé et configuré que sur des appareils dont
            vous êtes le propriétaire légitime ou pour lesquels vous disposez
            d’une autorisation explicite de gestion.
          </li>
          <li>
            L’application dans sa version grand public est mise à disposition
            gratuitement. En cas d’introduction ultérieure de fonctionnalités
            optionnelles payantes, l’utilisateur en sera préalablement informé
            sur la fiche Google Play et aucune facturation ne saurait intervenir
            sans consentement explicite.
          </li>
          <li>
            L’utilisateur s’interdit toute utilisation détournée du logiciel à
            des fins illicites ou de surveillance non consentie d’un tiers.
          </li>
        </ul>
      </section>

      <section className="space-y-3">
        <h2 className="text-xl font-semibold text-slate-950 dark:text-white">
          5. Permissions système et compatibilité constructeurs
        </h2>
        <p>
          Le bon fonctionnement des blocages repose sur les autorisations
          accordées par l’utilisateur dans Android (Service d’accessibilité,
          Accès aux données d’utilisation et, le cas échéant, exemption
          d’optimisation de batterie). En raison de la diversité des surcouches
          Android (Pixel, Samsung One UI, Xiaomi HyperOS, etc.) et de leurs
          stratégies d’économie d’énergie, l’éditeur fournit des guides de
          configuration mais ne peut garantir une compatibilité absolue sur
          l’ensemble des terminaux et mises à jour système futures.
        </p>
      </section>

      <section className="space-y-3">
        <h2 className="text-xl font-semibold text-slate-950 dark:text-white">
          6. Propriété intellectuelle
        </h2>
        <p>
          La marque <strong>Custos</strong>, le nom commercial, le logo ainsi
          que l’architecture éditoriale et visuelle du présent site sont la
          propriété de l’éditeur ({legal.publisherDisplayName}), sans
          préjudice des composants logiciels libres publiés sous licence MIT sur
          le dépôt GitHub officiel (
          <a
            href={SITE_CONFIG.githubRepoUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="text-indigo-600 hover:underline dark:text-indigo-400"
          >
            github.com/joshdeutc/custos
          </a>
          ) dont les termes de licence continuent de s’appliquer aux fichiers
          sources concernés.
        </p>
      </section>

      <section className="space-y-3">
        <h2 className="text-xl font-semibold text-slate-950 dark:text-white">
          7. Limitation de responsabilité et disponibilité
        </h2>
        <p>
          L’application est fournie « en l’état » comme outil d’aide à la
          discipline numérique. Dans les limites permises par la loi applicable,
          l’éditeur ne saurait être tenu responsable d’un manquement de
          l’utilisateur résultant d’une configuration inadaptée de ses propres
          quotas ou délais, ni d’une indisponibilité temporaire du site vitrine
          ou du service tiers Google Play.
        </p>
      </section>

      <section className="space-y-3">
        <h2 className="text-xl font-semibold text-slate-950 dark:text-white">
          8. Résiliation &amp; Suppression des données
        </h2>
        <p>
          Vous pouvez cesser d’utiliser la version standard de Custos à tout
          moment en révoquant ses autorisations et en la désinstallant depuis
          Android. Pour supprimer toute donnée associée à un accès anticipé ou
          support, consultez{" "}
          <Link
            href="/delete-account"
            className="font-medium text-indigo-600 hover:underline dark:text-indigo-400"
          >
            notre page dédiée à la suppression de compte et de données
          </Link>
          .
        </p>
      </section>

      <section className="space-y-3">
        <h2 className="text-xl font-semibold text-slate-950 dark:text-white">
          9. Droit applicable et règlement des litiges
        </h2>
        <p>
          Les présentes CGU sont régies par le droit français. En cas de
          difficulté ou de réclamation relative à l’utilisation de Custos,
          l’utilisateur est invité à contacter préalablement l’éditeur à{" "}
          <a
            href={`mailto:${legal.email}`}
            className="font-medium text-indigo-600 hover:underline dark:text-indigo-400"
          >
            {legal.email}
          </a>{" "}
          afin de rechercher une solution amiable. À défaut d’accord, tout litige
          relèvera des juridictions françaises compétentes conformément aux règles
          de procédure applicables.
        </p>
      </section>
    </LegalPageShell>
  );
}
