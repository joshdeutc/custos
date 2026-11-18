import type { Metadata } from "next";
import Link from "next/link";
import { Clock, Mail, Smartphone, Trash2 } from "lucide-react";
import { LegalPageShell } from "@/components/LegalPageShell";
import { SITE_CONFIG } from "@/lib/constants";

export const metadata: Metadata = {
  title: "Account & Data Deletion",
  description:
    "Official procedure to delete your Early Access entry and any data associated with the Custos Android app (Google Play Data Safety).",
  alternates: {
    canonical: "/delete-account",
  },
  openGraph: {
    type: "website",
    locale: "en_US",
    url: "/delete-account",
    siteName: "Custos",
    title: "Account & Data Deletion | Custos",
    description:
      "Official instructions to clear local Android data or request deletion of your Custos Early Access email.",
  },
  twitter: {
    card: "summary",
    title: "Account & Data Deletion | Custos",
    description:
      "Clear and transparent procedure to delete data associated with the Custos Android app.",
  },
};

export default function DeleteAccountPage() {
  const { legal } = SITE_CONFIG;

  const deletionMailto = `mailto:${legal.email}?subject=${encodeURIComponent(
    "[Custos] Account & Data Deletion Request"
  )}&body=${encodeURIComponent(
    "Hello,\n\nI request the permanent deletion of my Early Access entry / account and all associated data for the Custos app.\n\nEmail address concerned: \nAdditional details (optional): \n\nThank you."
  )}`;

  return (
    <LegalPageShell
      badge="Google Play Data Safety · GDPR Art. 17"
      title="Account & Data Deletion"
      subtitle="Clear, transparent instructions to erase your local data on Android or request by email the permanent deletion of any information linked to Early Access or support."
    >
      {/* Important Clarification Banner */}
      <section className="rounded-2xl border border-emerald-200 bg-emerald-50/70 p-6 dark:border-emerald-500/30 dark:bg-emerald-500/10">
        <h2 className="text-base font-semibold text-emerald-950 dark:text-emerald-200">
          Good to know: Custos works without cloud account creation
        </h2>
        <p className="mt-2 text-sm leading-relaxed text-emerald-900 dark:text-emerald-100/90">
          The Custos mobile app does not require creating an account on our
          servers: all of your app limits, curfews, and statistics remain stored
          locally on your phone. If you shared an email address for{" "}
          <strong>Google Play Early Access</strong> or{" "}
          <strong>technical support</strong>, you can request its permanent
          deletion at any time using the steps below.
        </p>
      </section>

      {/* Method 1: Remote Account / Email Data Deletion via Email */}
      <section className="space-y-5">
        <div className="flex items-center gap-2.5">
          <div className="inline-flex h-9 w-9 items-center justify-center rounded-xl bg-indigo-500/10 text-indigo-600 dark:text-indigo-400">
            <Mail className="h-5 w-5" />
          </div>
          <h2 className="text-xl font-semibold text-slate-950 dark:text-white">
            1. Request Permanent Deletion of Your Account / Email
          </h2>
        </div>

        <p>
          Follow these steps to request complete deletion of your Early Access
          email address (Google Play Closed Testing) and any support message
          history held by{" "}
          <strong>
            {legal.publisherApp} — {legal.publisherDisplayName} ({legal.city})
          </strong>{" "}
          ({legal.email}):
        </p>

        <ol className="space-y-4">
          {[
            {
              step: "Step 1",
              title: "Email from the address concerned",
              text: `Send an email to ${legal.email}, ideally from the email address associated with your Early Access request or support messages.`,
            },
            {
              step: "Step 2",
              title: "Include the request subject",
              text: "Use the subject line: “[Custos] Account & Data Deletion Request” so your request is prioritized.",
            },
            {
              step: "Step 3",
              title: "Specify what to delete",
              text: "Confirm the Google Play or support email address concerned and, if applicable, that you wish to leave the Custos Early Access tester list.",
            },
            {
              step: "Step 4",
              title: "Confirmation & permanent erasure",
              text: "You will receive an acknowledgment within 48 business hours. Permanent deletion is completed within 7 business days maximum (well within the 1-month GDPR limit) and confirmed by email.",
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
              Send a deletion request in one click
            </p>
            <p className="mt-1 flex items-center gap-1.5 text-xs text-slate-600 dark:text-zinc-400">
              <Clock className="h-3.5 w-3.5 text-indigo-600 dark:text-indigo-400" />
              <span>Processing time: within 7 business days maximum</span>
            </p>
          </div>
          <a
            href={deletionMailto}
            className="inline-flex items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-indigo-600 to-purple-600 px-5 py-3 text-sm font-semibold text-white shadow-sm transition-all hover:brightness-110"
          >
            <Trash2 className="h-4 w-4" />
            <span>Email {legal.email}</span>
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
            2. Immediately Erase Local Data on Android
          </h2>
        </div>

        <p>
          To instantaneously wipe 100% of rules, quotas, preferences, and states
          stored on your phone:
        </p>

        <div className="rounded-xl border border-slate-200 bg-white p-5 dark:border-zinc-800 dark:bg-[#111116]">
          <ol className="list-decimal space-y-2 pl-5 text-sm text-slate-700 dark:text-zinc-300">
            <li>
              Open your device’s <strong>Android Settings</strong>.
            </li>
            <li>
              Go to <strong>Apps</strong> → <strong>Custos</strong> →{" "}
              <strong>Storage &amp; cache</strong>.
            </li>
            <li>
              Tap <strong>Clear storage</strong> (or simply uninstall the app).
            </li>
          </ol>
          <p className="mt-3 text-xs text-slate-600 dark:text-zinc-400">
            Note: if Google Drive / Android Auto Backup is enabled on your
            device, you can also delete the app backup inside your Google
            account settings.
          </p>
        </div>
      </section>

      {/* Summary of Deleted vs Retained Data */}
      <section className="space-y-3">
        <h2 className="text-xl font-semibold text-slate-950 dark:text-white">
          3. Scope of Deleted Data &amp; Legal Exceptions
        </h2>
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="rounded-xl border border-slate-200 bg-white p-5 dark:border-zinc-800 dark:bg-[#111116]">
            <h3 className="text-sm font-semibold text-emerald-700 dark:text-emerald-400">
              Permanently Deleted Data
            </h3>
            <ul className="mt-2 list-disc space-y-1.5 pl-4 text-xs leading-relaxed text-slate-600 dark:text-zinc-400">
              <li>
                Email address enrolled in Google Play Closed Testing / Early
                Access.
              </li>
              <li>
                All support messages and attachments exchanged with {legal.email}.
              </li>
              <li>
                On your phone: all local JSON files and preferences upon
                uninstallation.
              </li>
            </ul>
          </div>

          <div className="rounded-xl border border-slate-200 bg-white p-5 dark:border-zinc-800 dark:bg-[#111116]">
            <h3 className="text-sm font-semibold text-slate-900 dark:text-zinc-200">
              Residual Technical Retention
            </h3>
            <ul className="mt-2 list-disc space-y-1.5 pl-4 text-xs leading-relaxed text-slate-600 dark:text-zinc-400">
              <li>
                Transient web security logs from the hosting provider (
                {legal.hostName}), automatically purged according to its
                standard retention windows.
              </li>
              <li>
                Records strictly required by French accounting law in the event
                of future commercial billing (not applicable to free users).
              </li>
            </ul>
          </div>
        </div>
        <p className="pt-2 text-xs text-slate-600 dark:text-zinc-400">
          See also:{" "}
          <Link
            href="/privacy"
            className="text-indigo-600 hover:underline dark:text-indigo-400"
          >
            Privacy Policy
          </Link>{" "}
          ·{" "}
          <Link
            href="/terms"
            className="text-indigo-600 hover:underline dark:text-indigo-400"
          >
            Terms of Use &amp; Legal Notice
          </Link>
          .
        </p>
      </section>
    </LegalPageShell>
  );
}
