# Directives d'Action pour les Agents IA (Antigravity & Assistants)

Ce document constitue la charte opérationnelle que tout agent IA doit lire et appliquer immédiatement dès l'ouverture du dépôt **Custos**.

---

## 1. Vue d'Ensemble & Passation Immédiate
Pour le contexte technique complet, la liste des téléphones connectés et l'historique des patchs, consulte :
👉 **[docs/ARCHITECTURE_ASYMETRIQUE_DEV_RELEASE.md](docs/ARCHITECTURE_ASYMETRIQUE_DEV_RELEASE.md)** (RÈGLE MAÎTRESSE : Guardian en Release GitHub vs Custos UI en Debug Local)
👉 **[README_POUR_NOUVEL_AGENT.md](README_POUR_NOUVEL_AGENT.md)**
👉 **[docs/TIME_LOCK_RELEASE.md](docs/TIME_LOCK_RELEASE.md)**

---

## 2. Règles Cardinales Inviolables
1. **FAIL-SAFE SACRÉ** : Ne **JAMAIS** activer `DISALLOW_FACTORY_RESET`. L'accès au mode Recovery et à la réinitialisation matérielle aux paramètres d'usine doit rester intact à 100%.
2. **AUCUNE PORTE DÉROBÉE** : Le code de révocation du Device Owner (`REMOVE_OWNER`, `clearDeviceOwnerApp`) a été définitivement éradiqué. Ne jamais réintroduire de broadcast ou de mécanisme permettant d'annuler le statut Device Owner sans un Factory Reset.
3. **ARCHITECTURE ASYMÉTRIQUE STRICTE** :
   * **Guardian (`:guardian`)** est le Device Owner inviolable : il doit **TOUJOURS** être compilé via GitHub Actions (`release.yml`), signé avec `custos-release.jks` sous le Wait Timer 24h.
   * **Custos UI (`:app`)** est la console de développement : il se compile **EN LOCAL EN DEBUG** (`:app:assembleMeDebug`) via `update_app.ps1` pour une agilité totale sans attendre GitHub.
4. **TIME-LOCK 24H RESPECTÉ** : Tout build de release officiel de Guardian doit obligatoirement passer par `.github/workflows/release.yml` sous le Wait Timer de 24h de l'environnement GitHub `production`.
5. **PÉRIMÈTRE PRIVÉ** : Ne jamais toucher aux WebViews personnalisées (`notubeplayer`), chats privés, applications de rencontre ou réseaux sociaux.
6. **IMMUNITÉ SYSTÈME ABSOLUE (Anti-Bootloop)** : Ne **JAMAIS** masquer (`setApplicationHidden`) ou suspendre (`setPackagesSuspended`) une application système (`FLAG_SYSTEM` ou `FLAG_UPDATED_SYSTEM_APP`) ni un composant critique du framework Android (notamment `com.google.android.permissioncontroller` / `com.android.permissioncontroller`). Voir [docs/POST_MORTEM_BOOTLOOP_20261101.md](docs/POST_MORTEM_BOOTLOOP_20261101.md).

---

## 3. Cartographie & Exploration du Code (Graphify)
* Ce dépôt intègre un graphe de connaissances persistant généré par Graphify dans `graphify-out/`.
* Avant de faire des modifications lourdes ou transversales, consulte `.agents/rules/graphify.md` ou inspecte `graphify-out/graph.html`.

---

## 4. Outils d'Administration & Déploiement
* Compilation et mise à jour USB : `update_app.ps1`
* Commandes de diagnostic ADB : [docs/COMMANDES_ADB.md](docs/COMMANDES_ADB.md)
* Guide de provisioning Device Owner : [docs/DEVICE_OWNER_PROVISIONING.md](docs/DEVICE_OWNER_PROVISIONING.md)
