# Architecture de Verrouillage Temporel (Time-Lock Release) — Custos & Guardian

> **Directive de sécurité inviolable pour Antigravity, les sous-agents et l'utilisateur.**

---

## 1. Contexte & Modèle de Menace (Le Paradoxe du Créateur)

Custos est un système de contrôle et d'auto-discipline numérique destiné à protéger l'utilisateur contre ses impulsions numériques.
Dans une architecture monolithique classique où le développeur est lui-même l'utilisateur final :
1. L'utilisateur possède les sources, le compilateur et la clé locale.
2. En cas de pulsion impulsive (ex: vouloir débloquer une application ou raccourcir un délai), il pourrait modifier le code en local et flasher immédiatement l'APK via ADB (`adb install -r`).

Pour rendre le système mathématiquement inviolable, l'architecture sépare les pouvoirs :
* **Custos UI (`:app`)** : Interface graphique, règles d'écran, accessibilité.
* **Custos Guardian (`:guardian`)** : Gardien de sécurité détenant le statut **Device Owner**, gestionnaire de la Whitelist et de la Quarantaine constitutionnelle.

---

## 2. Le Mécanisme de Verrouillage Cryptographique & Temporel

1. **Signature cryptographique stricte (Android)** :
   * Le module `:guardian` (et `:app` en release) est signé avec une clé privée unique (`custos-release.jks`).
   * Une fois déployé sur l'appareil, Android rejette toute tentative de mise à jour ou de remplacement avec une signature différente (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`).
   * Le statut Device Owner interdit toute désinstallation via l'interface (`setUninstallBlocked`).

2. **Séquestration temporelle (GitHub Actions Time-Lock)** :
   * La clé de signature de release n'est **PAS** conservée sur le PC local.
   * Elle est stockée dans les secrets GitHub d'un environnement protégé : **`production`**.
   * L'environnement `production` est muni d'un **Wait Timer obligatoire** (ex: 24h ou 48h / 1440 à 2880 minutes).

3. **Cycle de modification imposé** :
   * Toute modification de Guardian nécessite de :
     1. Pousser le commit sur GitHub (`git push`).
     2. Déclencher le workflow `.github/workflows/release.yml` (ou un tag `v*`).
     3. **Attendre obligatoirement l'expiration du Wait Timer GitHub (24h/48h)**.
     4. Télécharger l'APK signé produit par GitHub Actions et l'installer.
   * L'utilisateur dispose ainsi de 24 à 48 heures de réflexion pour que toute tentation impulsive retombe.

---

## 3. Directives Strictes pour Antigravity (Agents IA)

1. **Interdiction absolue de contournement local** :
   * L'agent ne doit **JAMAIS** chercher à contourner ce délai en créant des clés locales ou en forçant une compilation release non temporisée pour affaiblir les règles.
   * L'agent ne doit pas modifier Guardian pour affaiblir la Constitution (réduire les délais minimums, bypasser la quarantaine).
2. **Cycle normal de mise à jour** :
   * Appliquer les modifications légitimes dans le code.
   * Vérifier la compilation locale.
   * Commiter et pousser sur Git.
   * Laisser le workflow GitHub Actions exécuter la release après écoulement du Wait Timer.

---

## 4. Procédure de Configuration GitHub

### A. Obtenir la clé en Base64
Dans PowerShell :
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("custos-release.jks")) | Set-Content "keystore_base64.txt"
```

### B. Configurer l'Environnement GitHub
1. Aller sur le dépôt GitHub de Custos ➔ **Settings** ➔ **Environments** ➔ **New environment** : `production`.
2. Cocher **Wait timer** et renseigner la durée (ex: `1440` minutes pour 24h, ou `2880` minutes pour 48h).
3. Dans la section **Environment secrets**, ajouter :
   * `KEYSTORE_BASE64` : Contenu du fichier `keystore_base64.txt`.
   * `KEYSTORE_PASSWORD` : Mot de passe du keystore (`duxJLcbWga5Rf9wU8eNGOz1o`).
   * `KEY_ALIAS` : Alias de la clé (`custos`).
   * `KEY_PASSWORD` : Mot de passe de la clé (`duxJLcbWga5Rf9wU8eNGOz1o`).

### C. Sécurisation Locale Finale
Une fois les APKs Release installés sur le téléphone et les secrets enregistrés sur GitHub :
* Supprimer `custos-release.jks`, `keystore.properties` et `keystore_base64.txt` du PC de travail (les archiver uniquement sur clé USB hors-ligne ou coffre-fort froid).
