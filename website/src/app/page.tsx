import type { Metadata } from "next";
import { HeroSection } from "@/components/HeroSection";
import { FeaturesSection } from "@/components/FeaturesSection";
import { SecuritySection } from "@/components/SecuritySection";
import { FaqSection } from "@/components/FaqSection";
import { FinalCtaSection } from "@/components/FinalCtaSection";

export const metadata: Metadata = {
  title: {
    absolute:
      "Custos — Contrôle du temps d’écran et auto-discipline sur Android",
  },
  description:
    "Découvrez Custos : quotas quotidiens, limites par session, couvre-feux, Accès Partiel (YouTube Shorts, Reels), Mode Nucléaire et délai de sécurité asymétrique. Zéro cloud, 100 % local.",
  alternates: {
    canonical: "/",
  },
  openGraph: {
    type: "website",
    locale: "fr_FR",
    url: "/",
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
};

export default function HomePage() {
  return (
    <>
      <HeroSection />
      <FeaturesSection />
      <SecuritySection />
      <FaqSection />
      <FinalCtaSection />
    </>
  );
}
