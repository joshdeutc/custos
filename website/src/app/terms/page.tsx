import type { Metadata } from "next";
import Link from "next/link";
import { AlertTriangle } from "lucide-react";
import { LegalPageShell } from "@/components/LegalPageShell";
import { SITE_CONFIG } from "@/lib/constants";

export const metadata: Metadata = {
  title: "Terms of Use & Legal Notice",
  description:
    "Official Terms of Use and Legal Notice for the Custos Android app and custos.joshuanancey.com.",
  alternates: {
    canonical: "/terms",
  },
  openGraph: {
    type: "website",
    locale: "en_US",
    url: "/terms",
    siteName: "Custos",
    title: "Terms of Use & Legal Notice | Custos",
    description:
      "Legal Notice and Terms of Use for the Custos Android mobile application.",
  },
  twitter: {
    card: "summary",
    title: "Terms of Use & Legal Notice | Custos",
    description:
      "Legal Notice and Terms of Use for the Custos Android mobile application.",
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
      badge="Terms of Use & Legal Notice"
      title="Terms of Use & Legal Notice"
      subtitle="Terms governing the use of the Custos Android application and the custos.joshuanancey.com website."
    >
      <section className="space-y-3">
        <h2 className="text-xl font-semibold text-slate-950 dark:text-white">
          1. Publisher &amp; Legal Notice
        </h2>
        <p>
          The mobile app <strong>{legal.publisherApp}</strong> (package{" "}
          <code className="rounded bg-slate-100 px-1.5 py-0.5 font-mono text-xs dark:bg-zinc-800">
            {SITE_CONFIG.packageName}
          </code>
          ) and the website{" "}
          <code className="rounded bg-slate-100 px-1.5 py-0.5 font-mono text-xs dark:bg-zinc-800">
            custos.joshuanancey.com
          </code>{" "}
          are published by:
        </p>
        <div className="rounded-xl border border-slate-200 bg-white p-4 text-sm dark:border-zinc-800 dark:bg-[#111116]">
          <p>
            <strong>Publisher:</strong> {legal.publisherApp} â€”{" "}
            {legal.legalForm} {legal.legalEntity}
          </p>
          <p className="mt-1">
            <strong>SIRET:</strong>{" "}
            <span className="font-mono">{legal.siret}</span>
          </p>
          <p className="mt-1">
            <strong>Registered Address:</strong> {legal.address}
          </p>
          <p className="mt-1">
            <strong>Contact &amp; Support:</strong>{" "}
            <a
              href={`mailto:${legal.email}`}
              className="font-medium text-indigo-600 hover:underline dark:text-indigo-400"
            >
              {legal.email}
            </a>
          </p>
          <p className="mt-1">
            <strong>Website Host:</strong> {legal.hostName} â€”{" "}
            {legal.hostAddress}
          </p>
        </div>
      </section>

      <section className="space-y-3">
        <h2 className="text-xl font-semibold text-slate-950 dark:text-white">
          2. Purpose of Custos
        </h2>
        <p>
          Custos is a self-discipline and screen-time management utility for
          smartphones running <strong>{SITE_CONFIG.minAndroidVersion}</strong>.
          It enables users to voluntarily configure daily quotas, session
          limits, curfews, targeted Partial Access filters, whitelists, and fixed
          lockdown sessions (Nuclear Mode).
        </p>
        <p>
          Installing and using Custos implies full acceptance of these Terms of
          Use as well as our{" "}
          <Link
            href="/privacy"
            className="font-medium text-indigo-600 hover:underline dark:text-indigo-400"
          >
            Privacy Policy
          </Link>
          .
        </p>
      </section>

      <section className="space-y-3">
        <h2 className="text-xl font-semibold text-slate-950 dark:text-white">
          3. Notice Regarding Irreversible Delays &amp; Lockdowns
        </h2>
        <div className="rounded-2xl border border-amber-200 bg-amber-50/70 p-5 dark:border-amber-500/30 dark:bg-amber-500/10">
          <div className="flex items-start gap-3">
            <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0 text-amber-600 dark:text-amber-400" />
            <div className="space-y-2 text-sm text-amber-950 dark:text-amber-100">
              <p className="font-semibold">Voluntary User Commitment</p>
              <p>
                By design, Custosâ€™s protection mechanisms (such as the{" "}
                <strong>cooling-off delay before relaxing a rule</strong> or{" "}
                <strong>fixed-duration Nuclear Mode</strong>) intentionally
                prevent immediate relaxation or early cancellation of a
                restriction until the user-selected countdown expires.
              </p>
              <p>
                Users are responsible for configuring their rules, durations,
                and app lists thoughtfully so as not to interfere with
                professional, personal, or essential activities.
              </p>
            </div>
          </div>
        </div>
        <p>
          <strong>Safety &amp; emergency calls:</strong> Custos never blocks
          critical phone calling functions, emergency numbers, or Androidâ€™s
          native hardware factory reset.
        </p>
      </section>

      <section className="space-y-3">
        <h2 className="text-xl font-semibold text-slate-950 dark:text-white">
          4. Access, Authorized Use &amp; Pricing
        </h2>
        <ul className="list-disc space-y-2 pl-5">
          <li>
            Custos may only be installed and configured on devices you
            legitimately own or are explicitly authorized to manage.
          </li>
          <li>
            The public Play Store release is provided free of charge. Should
            optional paid features be introduced later, users will be informed
            on the Google Play listing and no charges will occur without
            explicit consent.
          </li>
          <li>
            Users agree not to misuse the software for unlawful purposes or
            non-consensual monitoring of third parties.
          </li>
        </ul>
      </section>

      <section className="space-y-3">
        <h2 className="text-xl font-semibold text-slate-950 dark:text-white">
          5. System Permissions &amp; OEM Compatibility
        </h2>
        <p>
          Blocking reliability depends on permissions granted by the user in
          Android (Accessibility Service, Usage Access, and where applicable,
          battery optimization exemption). Due to the diversity of Android OEM
          skins (Pixel, Samsung One UI, Xiaomi HyperOS, etc.) and power-saving
          policies, the publisher provides setup guidance but cannot guarantee
          universal compatibility across all devices and future OS updates.
        </p>
      </section>

      <section className="space-y-3">
        <h2 className="text-xl font-semibold text-slate-950 dark:text-white">
          6. Intellectual Property
        </h2>
        <p>
          The <strong>Custos</strong> brand, trade name, logo, and website
          editorial/visual design are the property of the publisher (
          {legal.publisherDisplayName}), without prejudice to open-source
          components published under the MIT License on the official GitHub
          repository (
          <a
            href={SITE_CONFIG.githubRepoUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="text-indigo-600 hover:underline dark:text-indigo-400"
          >
            github.com/joshdeutc/custos-studio
          </a>
          ), whose license terms continue to apply to those source files.
        </p>
      </section>

      <section className="space-y-3">
        <h2 className="text-xl font-semibold text-slate-950 dark:text-white">
          7. Limitation of Liability &amp; Availability
        </h2>
        <p>
          The application is provided â€œas isâ€ as a digital self-discipline tool.
          To the fullest extent permitted by applicable law, the publisher shall
          not be liable for any user inconvenience resulting from
          misconfigured quotas or delays, nor for temporary unavailability of
          the website or third-party Google Play services.
        </p>
      </section>

      <section className="space-y-3">
        <h2 className="text-xl font-semibold text-slate-950 dark:text-white">
          8. Termination &amp; Data Deletion
        </h2>
        <p>
          You may stop using the standard version of Custos at any time by
          revoking its permissions and uninstalling it from Android. To delete
          any data associated with Early Access or support, visit our{" "}
          <Link
            href="/delete-account"
            className="font-medium text-indigo-600 hover:underline dark:text-indigo-400"
          >
            Account &amp; Data Deletion page
          </Link>
          .
        </p>
      </section>

      <section className="space-y-3">
        <h2 className="text-xl font-semibold text-slate-950 dark:text-white">
          9. Governing Law &amp; Disputes
        </h2>
        <p>
          These Terms of Use are governed by French law. In the event of any
          issue or claim regarding Custos, users are invited to first contact
          the publisher at{" "}
          <a
            href={`mailto:${legal.email}`}
            className="font-medium text-indigo-600 hover:underline dark:text-indigo-400"
          >
            {legal.email}
          </a>{" "}
          to seek an amicable resolution. Failing agreement, disputes shall be
          submitted to the competent French courts.
        </p>
      </section>
    </LegalPageShell>
  );
}

