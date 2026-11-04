# Statut Officiel du Verrouillage Cryptographique & Time-Lock GitHub

> **Date d'activation officielle** : 3 - 4 Novembre 2026  
> **Statut de sécurité** : 🔒 **VERROUILLAGE OFFICIEL ACTIF (SEQUESTRE GITHUB ACTIONS & COLD STORAGE USB)**  
> **Appareil cible** : Google Pixel 8a (`41111JEKB22083` - `akita`) en statut **Device Owner**  
> **Directive absolue pour tout Agent IA (Antigravity, Claude, Assistants)** : **INVIOLABLE**

---

## 1. Résumé Exécutif : Le Cadenas est Scellé

L'architecture asymétrique de Custos est désormais officiellement et définitivement scellée :
1. **La clé de signature de release (`custos-release.jks`) n'existe PLUS sur cet ordinateur.**
2. **Tous les fichiers locaux (`custos-release.jks`, `keystore.properties`, `CUSTOS_KEYSTORES_BACKUP`) ont été définitivement purgés du PC.**
3. **Le seul moyen de compiler et signer une version Release de Custos Guardian (`:guardian`) est de passer par le workflow GitHub Actions [`.github/workflows/release.yml`](../.github/workflows/release.yml).**
4. **Ce workflow est assujetti au Wait Timer obligatoire (Time-Lock de 2h minimum) de l'environnement GitHub `production`.**
5. **Une unique copie de secours hors-ligne (Cold Storage) a été créée sur clé USB physique externe (`D:\Josh_dont_delete\CUSTOS_KEYSTORE_COLD_STORAGE\`) et doit être débranchée et conservée en lieu sûr.**

---

## 2. Modèle de Menace & Séparation Absolue des Pouvoirs

Le système Custos est conçu pour résoudre le « Paradoxe du Créateur » : empêcher l'utilisateur et les agents IA de succomber à une pulsion impulsive de déverrouillage immédiat.

```
       ┌────────────────────────────────────────────────────────┐
       │             DÉVELOPPEMENT LOCAL (AGILE)                │
       │  Custos UI (:app) — assembleMeDebug (update_app.ps1)   │
       │  Clé : debug.keystore locale                           │
       │  Délai : 0 seconde (déploiement immédiat)              │
       │  Pouvoir : Interface graphique, règles d'écran, logs   │
       └──────────────────────────┬─────────────────────────────┘
                                  │ Ne peut PAS modifier le Device Owner
                                  ▼
       ┌────────────────────────────────────────────────────────┐
       │             CADENAS SYSTÈME (TIME-LOCKED)              │
       │  Custos Guardian (:guardian) — assembleRelease         │
       │  Clé : custos-release.jks (SECRETS GITHUB SEULS)       │
       │  Délai : 2 Heures minimum (Wait Timer GitHub Actions)  │
       │  Pouvoir : Device Owner, Whitelist, DoT DNS, Quarantaine│
       └────────────────────────────────────────────────────────┘
```

* **Android rejette mathématiquement tout APK Guardian compilé en local** :
  L'application Guardian installée sur le Pixel 8a a été signée avec le certificat release officiel. Toute tentative d'installer un APK signé en debug échoue instantanément avec l'erreur système :
  `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.
* **Le Device Owner interdit toute désinstallation** :
  `setUninstallBlocked` est actif sur Guardian et sur Custos. Il est impossible de désinstaller Guardian pour contourner la signature sans réinitialiser entièrement le téléphone aux paramètres d'usine (Factory Reset).

---

## 3. Empreintes Cryptographiques Inviolables

L'identité cryptographique officielle de Custos Guardian est certifiée par les empreintes suivantes :

* **Nom du Keystore** : `custos-release.jks`
* **Alias** : `custos`
* **DN Propriétaire** : `CN=Custos, O=joshdeutc, C=FR`
* **Validité** : 7 Juin 2026 ➔ 23 Octobre 2053
* **Empreinte SHA-256 du certificat** :
  ```text
  01:6C:BA:70:27:10:65:A7:DB:B5:0B:DE:78:EB:C5:8E:5A:BF:6A:58:91:4F:9C:03:BD:6A:0D:FB:53:37:BB:7D
  ```
* **Empreinte SHA-1 du certificat** :
  ```text
  A6:03:02:DB:56:BC:69:D6:98:A8:82:DB:74:2A:2A:29:53:BB:12:BF
  ```
* **SHA-256 du fichier physique `custos-release.jks`** :
  ```text
  F970963962C078497744C7F826D67ABC55DC358001DEA24C0F3DA8ECB41A1604
  ```

---

## 4. Sauvegarde Hors-Ligne (Cold Storage USB)

En cas de perte d'accès au compte GitHub ou de sinistre majeur, une sauvegarde complète a été gravée sur support physique amovible :

* **Emplacement** : `D:\Josh_dont_delete\CUSTOS_KEYSTORE_COLD_STORAGE\`
* **Contenu archivé** :
  1. `custos-release.jks` (vérifié conforme SHA-256)
  2. `keystore.properties` (paramètres de signature et alias)
  3. `debug.keystore` (keystore debug originel de la machine)
  4. `LISEZ-MOI.md` (documentation technique des clés et historique)
  5. `README_COLD_STORAGE.txt` (consignes de sécurité cold storage)

> [!CAUTION]
> La clé USB doit être déconnectée du PC et stockée hors-ligne dans un tiroir ou un coffre physique. Elle ne doit en aucun cas rester branchée en permanence sur la machine de travail.

---

## 5. Règles Inviolables pour Tout Agent IA Successeur

Tout agent travaillant sur ce dépôt **DOIT RESPECTER STRICTEMENT** les consignes suivantes :

1. **Ne JAMAIS recréer de clé de release locale** :
   Ne créez aucun `custos-release.jks` local ni aucun fichier `keystore.properties` contenant des mots de passe.
2. **Ne JAMAIS tenter de contourner le Wait Timer GitHub** :
   Toute modification touchant le module `:guardian` doit être testée localement en mode debug si nécessaire, puis commitée et poussée vers GitHub pour publication via tag `v*` ou `workflow_dispatch`.
   L'attente du timer de 2 heures est **voulue, saine et non négociable**.
3. **Développer l'UI exclusivement en Debug Local** :
   Pour modifier l'interface, les vues Jetpack Compose, les quotas, les règles de suivi d'écran ou l'accessibilité dans `:app` :
   Utiliser uniquement `./gradlew :app:assembleMeDebug` ou `./update_app.ps1`.
4. **Respecter la non-révocabilité Device Owner** :
   Ne jamais introduire de backdoor pour annuler le statut Device Owner sans un Factory Reset matériel.

---

*Document généré et certifié le 4 Novembre 2026.*
