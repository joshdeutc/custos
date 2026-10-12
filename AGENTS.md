# Directives de Sécurité pour les Agents IA (Antigravity & Assistants)

## 1. Séparation des Privilèges : Custos UI (:app) vs Custos Guardian (:guardian)
- **Custos UI (`:app`)** :
  Interface utilisateur, accessibilité, règles d'écran, logs.
  Peut être modifiée et déployée en local à chaud pour itérer rapidement sur l'ergonomie.
- **Custos Guardian (`:guardian`)** :
  Gardien de sécurité autonome détenant le statut **Device Owner**.
  Gère les restrictions OS inviolables, les suspensions matérielles et la Constitution de Quarantaine.

## 2. Verrouillage Temporel des Mises à Jour (Time-Lock Release)
- **Friction psychologique par conception** :
  Toute modification du module Guardian doit être signée avec la clé de release sécurisée sous le **Wait Timer** GitHub Actions (voir [docs/TIME_LOCK_RELEASE.md](docs/TIME_LOCK_RELEASE.md)).
- **Interdiction stricte de contournement local** :
  L'agent IA ne doit **JAMAIS** :
  1. Générer de clé locale pour signer un build release de contournement.
  2. Bypasser les délais de quarantaine imposés par Guardian.
  3. Proposer des mécanismes affaiblissant la sécurité sans respecter le délai de temporisation (24h/48h).
- **Cycle de développement légitime** :
  Les améliorations ou correctifs sur Guardian doivent être commités, poussés sur GitHub, et validés via le workflow temporisé de GitHub Actions.
