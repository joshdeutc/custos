# Directives d'Action pour les Agents IA (Antigravity & Assistants)

Ce document constitue la charte opérationnelle que tout agent IA doit lire et appliquer immédiatement dès l'ouverture du dépôt **Custos**.

---

## 1. Vue d'Ensemble & Passation Immédiate
Pour le contexte technique complet, la liste des téléphones connectés et l'historique des patchs, consulte :
👉 **[README_POUR_NOUVEL_AGENT.md](README_POUR_NOUVEL_AGENT.md)**
👉 **[docs/TIME_LOCK_RELEASE.md](docs/TIME_LOCK_RELEASE.md)**

---

## 2. Règles Cardinales Inviolables
1. **FAIL-SAFE SACRÉ** : Ne **JAMAIS** activer `DISALLOW_FACTORY_RESET`. L'accès au mode Recovery et à la réinitialisation matérielle aux paramètres d'usine doit rester intact à 100%.
2. **AUCUNE PORTE DÉROBÉE** : Le code de révocation du Device Owner (`REMOVE_OWNER`, `clearDeviceOwnerApp`) a été définitivement éradiqué. Ne jamais réintroduire de broadcast ou de mécanisme permettant d'annuler le statut Device Owner sans un Factory Reset.
3. **TIME-LOCK 24H RESPETÉ** : Tout build de release officiel doit obligatoirement passer par `.github/workflows/release.yml` sous le Wait Timer de 24h de l'environnement GitHub `production`.
4. **PÉRIMÈTRE PRIVÉ** : Ne jamais toucher aux WebViews personnalisées (`notubeplayer`), chats privés, applications de rencontre ou réseaux sociaux.

---

## 3. Cartographie & Exploration du Code (Graphify)
* Ce dépôt intègre un graphe de connaissances persistant généré par Graphify dans `graphify-out/`.
* Avant de faire des modifications lourdes ou transversales, consulte `.agents/rules/graphify.md` ou inspecte `graphify-out/graph.html`.

---

## 4. Outils d'Administration & Déploiement
* Compilation et mise à jour USB : `update_app.ps1`
* Commandes de diagnostic ADB : [docs/COMMANDES_ADB.md](docs/COMMANDES_ADB.md)
* Guide de provisioning Device Owner : [docs/DEVICE_OWNER_PROVISIONING.md](docs/DEVICE_OWNER_PROVISIONING.md)
