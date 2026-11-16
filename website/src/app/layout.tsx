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
    default: "Custos — Contrôle du temps d’écran et auto-discipline sur Android",
    template: "%s | Custos",
  },
  description:
    "Custos est l’application Android d’auto-discipline conçue comme un engagement irréversible : quotas quotidiens, limites par session, couvre-feux, Accès Partiel in-app et Mode Nucléaire. 100 % local, zéro publicité.",
  applicationName: "Custos",
  authors: [{ name: "Joshua Nancey", url: SITE_CONFIG.domain }],
  creator: "Joshua Nancey",
  publisher: "Custos",
  keywords: [
    "Custos",
    "temps d'écran Android",
    "auto-discipline",
    "bloqueur d'applications Android",
    "mode nucléaire",
    "contrôle numérique",
    "productivité Android",
  ],
  openGraph: {
    type: "website",
    locale: "fr_FR",
    url: SITE_CONFIG.domain,
    siteName: "Custos",
    title: "Custos — Maîtrisez votre temps d’écran sur Android",
    description:
      "Quotas, couvre-feux, blocage d’onglets addictifs (Accès Partiel), Mode Nucléaire et délai de sécurité asymétrique. Traitement 100 % local, zéro tracking.",
  },
  twitter: {
    card: "summary",
    title: "Custos — Auto-discipline & temps d’écran sur Android",
    description:
      "Quotas quotidiens, Accès Partiel in-app, Mode Nucléaire et délai anti-impulsion. 100 % local sur votre téléphone.",
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
      lang="fr"
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
