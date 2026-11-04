# Architecture Asymétrique : Guardian (Release GitHub) vs Custos UI (Debug Local)

> **Document Fondateur et Charte Opérationnelle pour tous les Agents IA et Développeurs de Custos.**
> **Dernière mise à jour** : 2 Novembre 2026

---

## 1. Principe Fondamental : La Séparation des Pouvoirs

Pour concilier **inviolabilité de l'auto-discipline (Anti-Impulsion)** et **agilité totale de développement au quotidien**, Custos repose sur une architecture asymétrique stricte divisée en deux applications distinctes :

```
                  ┌──────────────────────────────────────────────┐
                  │           Google Pixel 8a (Josh)             │
                  └──────────────────────┬───────────────────────┘
                                         │
                 ┌───────────────────────┴───────────────────────┐
                 │                                               │
                 ▼                                               ▼
     ┌───────────────────────┐                       ┌───────────────────────┐
     │   Custos Guardian     │                       │       Custos UI       │
     │ `:guardian`           │                       │ `:app`                │
     │ com.jo.custos.guardian│                       │ com.jo.selfcontrol... │
     └───────────┬───────────┘                       └───────────┬───────────┘
                 │                                               │
                 ▼                                               ▼
       Statut : DEVICE OWNER                         Statut : Application Client
       Signature : custos-release.jks                Signature : debug.keystore (Local)
       Build : GITHUB ACTIONS SEUL                   Build : LOCAL (update_app.ps1)
       Délai : Time-Lock 24h/48h                     Délai : ZÉRO DÉLAI (Instantané)
       Rôle : Le Cadenas Constitutionnel             Rôle : Le Tableau de Bord
```

---

## 2. Rôles et Frontières de Sécurité

### A. Custos Guardian (`:guardian`) — Le Cadenas Inviolable
* **Détenteur unique du statut Device Owner**.
* **Ce qu'il contrôle** :
  - DNS Privé sécurisé (DoT NextDNS) et lockdown réseau.
  - Quarantaine de la Whitelist (délai constitutionnel avant d'autoriser une app).
  - Masquage (`setApplicationHidden`) et suspension (`setPackagesSuspended`) des applications non autorisées au niveau système.
  - Protection anti-désinstallation (`setUninstallBlocked`) sur Guardian et sur Custos UI.
  - Protection de l'accès aux paramètres Android sensibles (A11Y HOME bounce).
* **Règle absolue** :
  - **NE DOIT JAMAIS ÊTRE COMPILÉ AVEC UNE CLÉ LOCALE SUR LE TÉLÉPHONE DE PRODUCTION**.
  - Tout changement dans Guardian **doit obligatoirement** être commité, poussé sur GitHub et compilé via `.github/workflows/release.yml` sous le Wait Timer de 24h de l'environnement GitHub `production`.
  - La clé `custos-release.jks` est séquestrée sur GitHub (et stockée hors-ligne sur clé USB).

### B. Custos UI (`:app`) — L'Espace de Développement & Tableau de Bord
* **Application client et interface utilisateur**.
* **Ce qu'il contrôle** :
  - Écrans Jetpack Compose / Vues (Statistiques, Quotas, Curfews, Liste Whitelist).
  - Détection locale de l'avant-plan (`AppWatcherService`) et suivi du temps d'écran (`LimitService`).
  - Notification Listener (`SelfControlNotificationListener`).
* **Règle absolue** :
  - **SE COMPILE EN LOCAL EN DEBUG** (`:app:assembleMeDebug`).
  - L'agent et Josh peuvent modifier l'interface, tester des composants, corriger des bugs UI et déployer en 10 secondes via `update_app.ps1` ou Android Studio.
* **Pourquoi la sécurité reste-t-elle intacte ?**
  - Même si Josh ou un agent modifie le code de l'UI en local, **les règles réelles ne peuvent pas être bypassées** :
    1. Le masquage des applications et la quarantaine sont exécutés par **Guardian**.
    2. L'accès au DNS chiffré est verrouillé par **Guardian**.
    3. Toute requête de désactivation envoyée par l'UI via IPC AIDL est filtrée par le moteur de Guardian.

---

## 3. Gestion des Mises à Jour & Déploiements

| Action voulue | Cible | Commande / Procédure |
|---|---|---|
| **Modifier l'interface, corriger un bug UI, ajouter un écran** | `:app` | Modifier le code localement puis exécuter `./update_app.ps1` (ou `adb install -r -d -t app-me-debug.apk`). |
| **Modifier les règles système, la quarantaine ou Device Owner** | `:guardian` | 1. Modifier le code dans `guardian/`<br>2. Créer un commit et un tag git (`v1.x`)<br>3. `git push origin v1.x`<br>4. Attendre le workflow GitHub Actions (Time-Lock)<br>5. Télécharger `guardian-release.apk` et installer via `adb install -r`. |

---

## 4. Mise à Jour et Robustesse ADB (Zéro Porte Dérobée)

Guardian applique `dpm.setUninstallBlocked(admin, "com.jo.selfcontrol.ultimate", true)`. Pour mettre à jour Custos UI en cours de dev, **aucune désinstallation n'est requise** : Android permet l'écrasement en place via `adb install -r -d -t`.

Les anciennes commandes broadcast de secours (`UNBLOCK_CLIENT_UNINSTALL`, `RESET_WHITELIST_DELAY`, `FORCE_SET_CONFIG`) ont été **définitivement supprimées** lors de l'audit de sécurité :
- Elles constituaient des portes dérobées applicatives contournant le Time-Lock.
- Guardian est désormais hermétique : aucune commande broadcast ADB ne peut désactiver la protection ni altérer les délais.
- **Verrouillage de l'Horloge Système (`DISALLOW_CONFIG_DATE_TIME`)** : Guardian active automatiquement `UserManager.DISALLOW_CONFIG_DATE_TIME`, `setAutoTimeEnabled(true)` et `setAutoTimeZoneEnabled(true)` dans `GuardianEngine.enforceLockdown()` pour empêcher toute avance rapide de l'horloge système visant à faire expirer prématurément les délais (`System.currentTimeMillis()`).
- **Validation Constitutionnelle des Transitions AIDL (`saveValidatedDelayStateJson` & `saveValidatedScreenRulesJson`)** : Toute synchronisation JSON envoyée par `com.jo.selfcontrol.ultimate` (`:app` en debug) vers Guardian est inspectée différentiellement : les durcissements (ajout de règles, augmentation de délais, annulation de suppressions en attente) sont appliqués immédiatement, tandis que toute suppression immédiate, réduction de délai ou accélération de timestamp (`execute_at`) est rejetée ou bornée au délai de protection actif.
- Le seul moyen d'altérer la constitution de Guardian est de passer par le workflow GitHub Actions sous Time-Lock (2h).

---

## 5. Restauration Automatique des Permissions de Custos UI via ADB

Après toute réinstallation de Custos UI en debug, exécuter ce bloc pour restaurer l'intégralité des autorisations sans passer par les menus Android :

```bash
# Statistiques d'utilisation (UsageStats)
adb shell appops set com.jo.selfcontrol.ultimate android:get_usage_stats allow

# Notifications
adb shell pm grant com.jo.selfcontrol.ultimate android.permission.POST_NOTIFICATIONS

# Exclusion d'optimisation de batterie
adb shell dumpsys deviceidle whitelist +com.jo.selfcontrol.ultimate

# Services d'accessibilité (Guardian + Custos)
adb shell settings put secure enabled_accessibility_services com.jo.custos.guardian/.GuardianWatcherService:com.jo.selfcontrol.ultimate/.AppWatcherService
adb shell settings put secure accessibility_enabled 1

# Écoute des notifications (NotificationListener)
adb shell cmd notification allow_listener com.jo.selfcontrol.ultimate/.SelfControlNotificationListener
```
