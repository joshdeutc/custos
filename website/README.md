# Custos — Site Web Vitrine Officiel

Site vitrine SaaS & Pages légales officielles de l'application Android **Custos** (`https://custos.joshuanancey.com`), conçu avec **Next.js (App Router)**, **Tailwind CSS**, **Framer Motion** et **Lucide Icons**.

## Routes disponibles

- `/` — Landing Page officielle (Hero + Mockup Android interactif, Fonctionnalités extraites du code source, Architecture Privacy-First & Sécurité, FAQ, Accès anticipé Google Play)
- `/privacy` — Politique de confidentialité RGPD complète & déclaration transparente des permissions Android (Play Console)
- `/delete-account` — Procédure dédiée de suppression de compte et des données (exigence Google Play Data Safety)
- `/terms` — Conditions Générales d'Utilisation (CGU)

## Déploiement sur Vercel (`custos.joshuanancey.com`)

1. Sur Vercel, importer le dépôt GitHub `joshdeutc/custos`.
2. Dans **Project Settings → General → Root Directory**, définir `website`.
3. Framework Preset : **Next.js** (détecté automatiquement).
4. Build Command : `npm run build` | Output Directory : `.next` (par défaut).
5. Dans **Domains**, associer le sous-domaine `custos.joshuanancey.com` (CNAME pointant vers `cname.vercel-dns.com`).

## Développement local

```bash
cd website
npm install
npm run dev
```
