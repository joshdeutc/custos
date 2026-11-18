import type { Metadata } from "next";
import { Inter, JetBrains_Mono } from "next/font/google";
import "./globals.css";
import { ThemeProvider } from "@/components/ThemeProvider";
import { Navbar } from "@/components/Navbar";
import { Footer } from "@/components/Footer";
import { SITE_CONFIG } from "@/lib/constants";

const inter = Inter({
  subsets: ["latin"],
  variable: "--font-inter",
  display: "swap",
});

const jetbrainsMono = JetBrains_Mono({
  subsets: ["latin"],
  variable: "--font-mono",
  display: "swap",
});

export const metadata: Metadata = {
  metadataBase: new URL(SITE_CONFIG.domain),
  title: {
    default: "Custos — Screen Time Control & Self-Discipline on Android",
    template: "%s | Custos",
  },
  description:
    "Custos is the Android self-discipline app built as an irreversible commitment: daily quotas, session limits, curfews, in-app Partial Access, and Nuclear Mode. 100% local, zero ads.",
  applicationName: "Custos",
  authors: [{ name: "Joshua Nancey", url: SITE_CONFIG.domain }],
  creator: "Joshua Nancey",
  publisher: "Custos",
  keywords: [
    "Custos",
    "Android screen time",
    "self-discipline",
    "Android app blocker",
    "nuclear mode",
    "digital wellbeing",
    "Android productivity",
  ],
  openGraph: {
    type: "website",
    locale: "en_US",
    url: SITE_CONFIG.domain,
    siteName: "Custos",
    title: "Custos — Master Your Screen Time on Android",
    description:
      "Quotas, curfews, addictive tab blocking (Partial Access), Nuclear Mode, and asymmetric security delay. 100% local processing, zero tracking.",
  },
  twitter: {
    card: "summary",
    title: "Custos — Self-Discipline & Screen Time on Android",
    description:
      "Daily quotas, in-app Partial Access, Nuclear Mode, and anti-impulse delay. 100% local on your phone.",
  },
  robots: {
    index: true,
    follow: true,
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="en"
      suppressHydrationWarning
      className={`${inter.variable} ${jetbrainsMono.variable}`}
    >
      <body className="font-sans antialiased">
        <ThemeProvider>
          <div className="flex min-h-screen flex-col">
            <Navbar />
            <main className="flex-1">{children}</main>
            <Footer />
          </div>
        </ThemeProvider>
      </body>
    </html>
  );
}
