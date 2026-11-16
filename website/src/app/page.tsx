import type { Metadata } from "next";
import { HeroSection } from "@/components/HeroSection";
import { FeaturesSection } from "@/components/FeaturesSection";
import { SecuritySection } from "@/components/SecuritySection";
import { FaqSection } from "@/components/FaqSection";
import { FinalCtaSection } from "@/components/FinalCtaSection";

export const metadata: Metadata = {
  title: "Custos — Contrôle du temps d’écran et auto-discipline sur Android",
  description:
    "Découvrez Custos : quotas quotidiens, limites par session, couvre-feux, Accès Partiel (YouTube Shorts, Reels), Mode Nucléaire et délai de sécurité asymétrique. Zéro cloud, 100 % local.",
  alternates: {
    canonical: "/",
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
