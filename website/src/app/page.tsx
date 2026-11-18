import type { Metadata } from "next";
import { HeroSection } from "@/components/HeroSection";
import { FeaturesSection } from "@/components/FeaturesSection";
import { SecuritySection } from "@/components/SecuritySection";
import { FaqSection } from "@/components/FaqSection";
import { FinalCtaSection } from "@/components/FinalCtaSection";

export const metadata: Metadata = {
  title: {
    absolute:
      "Custos — Screen Time Control & Self-Discipline on Android",
  },
  description:
    "Discover Custos: daily quotas, session limits, curfews, Partial Access (YouTube Shorts, Reels), Nuclear Mode, and asymmetric security delay. Zero cloud, 100% local.",
  alternates: {
    canonical: "/",
  },
  openGraph: {
    type: "website",
    locale: "en_US",
    url: "/",
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
