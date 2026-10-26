# Guide d'Accueil et Mémoire pour le Nouvel Agent IA — Custos

> **Dernière mise à jour** : 26 Octobre 2026  
> **Dépôt unique** : `C:\Users\jo\Documents\Personnal_project\custos`  
> **Branche de travail active** : `feat/guardian-architecture` (liée à `origin/feat/guardian-architecture` sur `https://github.com/joshdeutc/custos.git`)

Bienvenue sur le projet **Custos** (anciennement *SelfControl Ultimate*). Ce document te donne en 2 minutes tout le contexte architectural, les règles de sécurité inviolables et l'état exact des appareils.

---

## 1. Vue d'Ensemble de l'Architecture

Custos est un système de contrôle de soi et de discipline numérique Android ultra-résilient basé sur une architecture multi-modules :

1. **`:app` (Custos Client / UI)** :
   * Interface utilisateur, moteur de détection en avant-plan (`AppWatcherService`), règles d'écran partiel (`ScreenRuleManager`), suivi du temps et quotas journaliers (`LimitService`).
   * Gère les listes de blocage, horaires et notifications muettes.
2. **`:guardian` (Custos Guardian Service)** :
   * Service démon autonome détenant ou partageant le statut **Device Owner**.
   * Applique le verrouillage système strict, l'interdiction de désinstallation croisée (`setUninstallBlocked`), la quarantaine des nouvelles applications (délai de 24h/48h) et le DNS Privé chiffré (DoT).
   * Dialoguant avec l'application principale via AIDL (`IGuardianService.aidl`).

---

## 2. Les Appareils et Flavors Actifs

| Appareil | Modèle / Serial | IP Tailscale | Statut Système | Flavor de Build |
|---|---|---|---|---|
| **Téléphone de Josh** | Google Pixel 8a (`akita` - `41111JEKB22083`) | `100.112.214.84:5555` | **Device Owner actif**, NextDNS actif | `meDebug` / `meRelease` |
| **Téléphone du Frère** | Redmi Note 13 (`22101316G` - `qc4dbuv8emzh6575`) | Câble USB / Wi-Fi | **Device Owner actif**, NextDNS actif | `brotherDebug` |
| **Samsung A53 de test** | Galaxy A53 (`SM-A536B` - `RZCT30L9EGJ`) | `100.100.229.39:5555` | **Device Owner actif**, NextDNS actif | `meDebug` |
| **Téléphone du Pote** | Galaxy A54 5G (`SM-A546U1` - `R5CW627PZEM`) | Câble USB | Mode standard (AccessibilityService seul) | `basicRelease` / `adminRelease` |

---

## 3. Les Règles Cardinales de Sécurité (Inviolables)

L'agent IA doit respecter scrupuleusement ces principes :

1. **Préserver impérativement la Réinitialisation d'Usine** :
   * `DISALLOW_FACTORY_RESET` ne doit **JAMAIS** être activé (`clearUserRestriction(UserManager.DISALLOW_FACTORY_RESET)`).
   * Le Factory Reset matériel en mode Recovery est l'unique porte de secours légitime pour éviter de briquer un téléphone.
2. **Aucune Porte Dérobée (Backdoor) Device Owner** :
   * La commande de secours `REMOVE_OWNER` et `clearDeviceOwnerApp()` a été **totalement supprimée** du code (commit `8909cc0`).
   * Il n'existe aucun code dans l'application capable de révoquer le statut Device Owner. Seul un Factory Reset peut remettre l'appareil à zéro.
3. **Time-Lock Release GitHub Actions (Délai de 24h)** :
   * Tout build Release de production est géré par `.github/workflows/release.yml` sous l'environnement `production` avec un délai d'attente (Wait Timer) de 24 heures.
   * En local sur la machine de Josh, le logiciel **Pluckeye** bloque au niveau noyau WFP le binaire `gh.exe` et le domaine `api.github.com`, interdisant toute modification impulsive des timers via script ou `curl`.
4. **Périmètre Strict** :
   * Ne jamais toucher aux WebViews (`notubeplayer`), applications de messagerie privée, applications de rencontre ou données personnelles de l'utilisateur.

---

## 4. Outils et Base de Connaissances (Graphify)

* **Graphify Knowledge Graph** :
  * Le projet est cartographié sous forme de graphe persistant dans `graphify-out/` (900 nœuds, 2820 arêtes).
  * Les règles de consultation pour Antigravity sont dans `.agents/rules/graphify.md` et `.agents/workflows/graphify.md`.
  * La visualisation interactive 2D est consultable dans `graphify-out/graph.html`.

---

## 5. Historique Récent & Derniers Correctifs Appliqués

1. **Suppression de la backdoor `REMOVE_OWNER`** (`commit 8909cc0`) :
   * `CommandReceiver.kt` et `DeviceOwnerHelper.kt` purgés de tout appel à `clearDeviceOwnerApp()`.
2. **Correction de la règle d'accès partiel WhatsApp** :
   * La règle `WhatsApp_Actus` bloquait les infos de groupe car `wds_profile_picture` était listé comme identifiant bloqué.
   * `screen_rules.json` sur le Pixel 8a a été épuré pour cibler uniquement l'annuaire des chaînes/actus (`newsletter_directory_*`).
3. **Résolution du plantage Accessibilité Samsung A54 (ami de Josh)** :
   * Purgé `Crashed services` via ADB.
   * Sécurisé `MainActivity.kt` (`commit 6437335`) pour éviter un `IndexOutOfBoundsException` lors de la suppression/édition de plages horaires `periodBlocks`.
4. **Consolidation en Projet Unique** :
   * Les anciens projets (`_android_projet-*`, `android_tmp`) ont été archivés dans `Documents/Archives_Anciens_Projets/custos_anciens_projets_20261026.zip`.
   * Tous les outils et docs de `Custos_Discussion_Memory` ont été fusionnés ici dans `custos/`.
