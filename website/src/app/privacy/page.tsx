import type { Metadata } from "next";
import Link from "next/link";
import { CheckCircle2, ShieldCheck } from "lucide-react";
import { LegalPageShell } from "@/components/LegalPageShell";
import { SITE_CONFIG } from "@/lib/constants";

export const metadata: Metadata = {
  title: "Privacy Policy (GDPR)",
  description:
    "Official Privacy Policy for the Custos Android app: 100% local processing, zero server collection, full transparency on Android permissions, and GDPR rights.",
  alternates: {
    canonical: "/privacy",
  },
  openGraph: {
    type: "website",
    locale: "en_US",
    url: "/privacy",
    siteName: "Custos",
    title: "Privacy Policy (GDPR) | Custos",
    description:
      "100% local processing on Android, zero trackers, full transparency on permissions, and GDPR rights for Custos.",
  },
  twitter: {
    card: "summary",
    title: "Privacy Policy (GDPR) | Custos",
    description:
      "100% local processing on Android, zero server collection, and transparent permissions.",
  },
};

const ANDROID_PERMISSIONS = [
  {
    permission: "Accessibility Service (BIND_ACCESSIBILITY_SERVICE)",
    purpose:
      "Identifies the active foreground app, returns you to the home screen when a quota or curfew expires, discourages impulsive deactivation in Android Settings, and locally detects tabs targeted by Partial Access rules (e.g. YouTube Shorts). Screen elements are evaluated strictly in volatile memory (RAM) on the fly: no screenshots, keystrokes, or personal content are ever recorded or transmitted.",
  },
  {
    permission: "Usage Access (PACKAGE_USAGE_STATS)",
    purpose:
      "Locally measures foreground time spent on the apps you chose to limit, in order to enforce your daily budgets and per-session limits.",
  },
  {
    permission: "Installed Apps Visibility (QUERY_ALL_PACKAGES)",
    purpose:
      "Displays the list of installed apps inside Custos so you can select which apps are subject to a quota, curfew, or Nuclear Mode. This list stays strictly on your device.",
  },
  {
    permission: "Notifications (POST_NOTIFICATIONS)",
    purpose:
      "Displays the persistent foreground notification required on Android 13+ to keep the enforcement service running reliably.",
  },
  {
    permission:
      "Notification Access — optional (BIND_NOTIFICATION_LISTENER_SERVICE)",
    purpose:
      "Temporarily mutes notifications from a currently blocked app, then restores them when the block ends. Message contents are never saved.",
  },
  {
    permission: "Do Not Disturb — optional (ACCESS_NOTIFICATION_POLICY)",
    purpose:
      "Enables Do Not Disturb during a Nuclear Mode session when you explicitly turn on this option.",
  },
  {
    permission:
      "Boot, Exact Alarms & Foreground Service (RECEIVE_BOOT_COMPLETED, SCHEDULE_EXACT_ALARM, FOREGROUND_SERVICE_SPECIAL_USE, REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)",
    purpose:
      "Restarts protection after a phone reboot and ensures time counters run reliably without premature OS sleep.",
  },
  {
    permission: "Technical Network Access (INTERNET)",
    purpose:
      "Declared solely for Android system compatibility. Custos operates locally offline and sends zero user data, analytics, or telemetry to third-party servers.",
  },
];

export default function PrivacyPage() {
  const { legal } = SITE_CONFIG;

  return (
    <LegalPageShell
      badge="GDPR & Google Play Compliance"
      title="Privacy Policy"
      subtitle="Full transparency on Custos’s 100% local operation, required Android permissions, and your rights under the General Data Protection Regulation (GDPR)."
    >
      {/* Executive Summary Box */}
      <section className="rounded-2xl border border-indigo-200/80 bg-white p-6 shadow-soft dark:border-indigo-500/30 dark:bg-[#111116]">
        <div className="flex items-center gap-2 text-sm font-semibold text-indigo-700 dark:text-indigo-300">
          <ShieldCheck className="h-4 w-4" />
          <span>In short</span>
        </div>
        <ul className="mt-4 space-y-2.5">
          {[
            "Custos runs locally on your phone without remote account creation.",
            "No personal data, browsing history, or usage statistics are ever collected, sold, or shared with third parties.",
            "The app includes no ad SDKs, no behavioral trackers, and no third-party analytics tools.",
            "You retain full control to export, modify (subject to your own chosen delays), or immediately delete all your local data.",
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
          1. Data Controller
        </h2>
        <p>
          The Android mobile app <strong>{legal.publisherApp}</strong> (
          <code className="rounded bg-slate-100 px-1.5 py-0.5 font-mono text-xs dark:bg-zinc-800">
            {SITE_CONFIG.packageName}
          </code>
          ) and this official website (
          <code className="rounded bg-slate-100 px-1.5 py-0.5 font-mono text-xs dark:bg-zinc-800">
            custos.joshuanancey.com
          </code>
          ) are published by:
        </p>
        <div className="rounded-xl border border-slate-200 bg-white p-4 text-sm dark:border-zinc-800 dark:bg-[#111116]">
          <p>
            <strong>Data Controller:</strong> {legal.publisherApp} —{" "}
            {legal.publisherDisplayName} ({legal.city})
          </p>
          <p className="mt-1">
            <strong>Privacy / GDPR Contact:</strong>{" "}
            <a
              href={`mailto:${legal.email}`}
              className="font-medium text-indigo-600 hover:underline dark:text-indigo-400"
            >
              {legal.email}
            </a>
          </p>
          <p className="mt-1 text-xs text-slate-600 dark:text-zinc-400">
            Full legal details are available in the{" "}
            <Link
              href="/terms"
              className="font-medium text-indigo-600 hover:underline dark:text-indigo-400"
            >
              Terms of Use &amp; Legal Notice (/terms)
            </Link>
            .
          </p>
        </div>
      </section>

      <section className="space-y-3">
        <h2 className="text-xl font-semibold text-slate-950 dark:text-white">
          2. Data Processed Locally on Your Device
        </h2>
        <p>
          To enforce the rules you configure, Custos stores data exclusively in
          the app’s private internal storage on your Android phone (
          <code className="rounded bg-slate-100 px-1.5 py-0.5 font-mono text-xs dark:bg-zinc-800">
            filesDir
          </code>{" "}
          and{" "}
          <code className="rounded bg-slate-100 px-1.5 py-0.5 font-mono text-xs dark:bg-zinc-800">
            SharedPreferences
          </code>
          ):
        </p>
        <ul className="list-disc space-y-2 pl-5">
          <li>
            <strong>Your discipline configurations:</strong> restricted apps,
            daily quotas, curfew schedules, session limits, anti-impulse delay
            settings, and Nuclear Mode presets (
            <code className="font-mono text-xs">limits.json</code>,{" "}
            <code className="font-mono text-xs">delay_config.json</code>,{" "}
            <code className="font-mono text-xs">nuclear_presets.json</code>).
          </li>
          <li>
            <strong>Your Partial Access rules:</strong> target package name and
            UI component view IDs used to recognize a blocked tab — never the
            contents of messages, photos, or videos displayed on screen (
            <code className="font-mono text-xs">screen_rules.json</code>).
          </li>
          <li>
            <strong>Current daily state:</strong> time spent on restricted apps
            and sessions used to track quotas until the daily reset (
            <code className="font-mono text-xs">usage_state.json</code>).
          </li>
          <li>
            <strong>Local event log:</strong> disabled by default (
            <code className="font-mono text-xs">EVENT_LOG_ENABLED = false</code>
            ) in public releases so no cross-app activity history is stored on
            disk.
          </li>
        </ul>
        <p>
          <strong>Android Auto Backup:</strong> on devices where Android Auto
          Backup is enabled by the user in Google/Android settings, the OS may
          encrypt and back up local configuration files to the user’s personal
          Google account according to Android privacy settings. Custos never
          accesses this backup.
        </p>
      </section>

      <section className="space-y-4">
        <h2 className="text-xl font-semibold text-slate-950 dark:text-white">
          3. Android Permissions &amp; AccessibilityService API Disclosure
        </h2>
        <p>
          In accordance with Google Play Store policies, each sensitive
          permission is listed below with its strict purpose. No permission is
          used for advertising, surveillance, or monetization.
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
          4. Support Emails, Early Access &amp; Web Hosting
        </h2>
        <ul className="list-disc space-y-2 pl-5">
          <li>
            <strong>Support &amp; Google Play Early Access Program:</strong>{" "}
            when you voluntarily email us at{" "}
            <a
              href={`mailto:${legal.email}`}
              className="font-medium text-indigo-600 hover:underline dark:text-indigo-400"
            >
              {legal.email}
            </a>{" "}
            (for example to join Google Play Early Access or ask a technical
            question), we process only your email address and message content on
            the basis of fulfilling your request / legitimate support interest
            (Art. 6.1.b / 6.1.f GDPR).
          </li>
          <li>
            <strong>Website Hosting:</strong>{" "}
            <code className="font-mono text-xs">custos.joshuanancey.com</code>{" "}
            is hosted by <strong>{legal.hostName}</strong> ({legal.hostAddress}
            ). This website uses zero advertising cookies or audience trackers.
            Like any web host, Vercel may process standard technical connection
            logs (IP address, timestamp, user-agent) solely for security and
            service continuity (see{" "}
            <a
              href={legal.hostUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="text-indigo-600 hover:underline dark:text-indigo-400"
            >
              Vercel’s Privacy Policy
            </a>
            ).
          </li>
        </ul>
      </section>

      <section className="space-y-3">
        <h2 className="text-xl font-semibold text-slate-950 dark:text-white">
          5. Third-Party Sharing &amp; Data Retention
        </h2>
        <p>
          <strong>Sharing:</strong> Custos never sells, rents, or transfers any
          user data to data brokers, ad networks, or third-party analytics
          providers.
        </p>
        <p>
          <strong>Retention:</strong> Configurations stored on your phone remain
          present only while the app is installed and are deleted immediately
          when you clear storage or uninstall Custos. Support emails are kept
          only as long as needed to handle your request, and for a maximum of{" "}
          <strong>36 months</strong> after the last exchange unless deleted
          earlier upon request.
        </p>
      </section>

      <section className="space-y-3">
        <h2 className="text-xl font-semibold text-slate-950 dark:text-white">
          6. Your GDPR Rights &amp; Data Deletion
        </h2>
        <p>
          Under the GDPR and French data protection law, you have the right to
          access, rectify, erase, restrict, object to, and port any contact
          information you have shared with us.
        </p>
        <p>
          To exercise your rights or request permanent deletion of any
          associated data, visit our dedicated{" "}
          <Link
            href="/delete-account"
            className="font-semibold text-indigo-600 hover:underline dark:text-indigo-400"
          >
            Account &amp; Data Deletion (/delete-account)
          </Link>{" "}
          page or email{" "}
          <a
            href={`mailto:${legal.email}`}
            className="font-semibold text-indigo-600 hover:underline dark:text-indigo-400"
          >
            {legal.email}
          </a>
          . You also have the right to lodge a complaint with the{" "}
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
          7. Minors &amp; Policy Updates
        </h2>
        <p>
          Custos does not knowingly collect personal data from minors. If any
          future feature introduces new data processing, this page will be
          updated with a new effective date at the top prior to release.
        </p>
      </section>
    </LegalPageShell>
  );
}
