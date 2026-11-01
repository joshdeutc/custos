# Post-Mortem : Incident Bootloop Pixel 8a (1er Novembre 2026)

> **Gravité** : CRITIQUE (Soft-brick / Bootloop nécessitant un Factory Reset matériel depuis le Recovery)  
> **Appareil touché** : Google Pixel 8a (`akita` - Android 14)  
> **Date de l'incident** : 1er Novembre 2026 à 16h50 CET  
> **Auteur du post-mortem** : Antigravity (Assistant IA)

---

## 1. Résumé Exécutif

Lors du déploiement de la mise à jour de Custos Guardian (`:guardian:assembleRelease`), le Google Pixel 8a est entré dans un bootloop continu au démarrage système (`system_server`).
Le processus système Android crashait fatalement dès la phase `startBootstrapServices` avec l'exception :
```
FATAL EXCEPTION IN SYSTEM PROCESS: main
java.lang.RuntimeException: There must be exactly one permissions manager; found []
    at com.android.server.pm.PackageManagerService.getRequiredPermissionControllerLPr(PackageManagerService.java:2773)
    at com.android.server.pm.PackageManagerService.<init>(PackageManagerService.java:2350)
    at com.android.server.pm.PackageManagerService.main(PackageManagerService.java:1789)
    at com.android.server.SystemServer.startBootstrapServices(SystemServer.java:1371)
```
Le gestionnaire de permissions Android (`com.google.android.permissioncontroller`) avait été marqué comme masqué (`hidden=true`) dans `/data/system/users/0/package-restrictions.xml` par le Guardian. Au démarrage suivant, `PackageManagerService` ne trouvait plus aucun gestionnaire de permissions, déclenchant un crash en boucle irrécupérable sans réinitialisation d'usine.

---

## 2. Chronologie et Cause Racine

### A. Origine de la régression (`commit cbfd4b4` du 31/10/2026)
Dans le commit `cbfd4b4` (*"fix(sync): fix Play Store quota blocking, granular pending delays, and delay UI visibility"*), la vérification d'immunité des applications système avait été supprimée de `GuardianEngine.isGuarded()` :
```kotlin
// Code SUPPRIMÉ par erreur dans cbfd4b4 :
val pm = ctx.packageManager
val isSys = try {
    val appInfo = pm.getApplicationInfo(pkg, PackageManager.MATCH_UNINSTALLED_PACKAGES)
    ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) ||
    ((appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0)
} catch (e: Exception) { false }
if (isSys) return true
```

### B. Le piège du nom AOSP vs Google Pixel
La liste de secours en dur `HARD_GUARDS` contenait uniquement le nom générique AOSP :
```kotlin
"com.android.permissioncontroller"
```
Or, sur les téléphones Google (Pixel 8a, Pixel 7, etc.), le composant système s'appelle :
```kotlin
"com.google.android.permissioncontroller"
```

### C. Déclenchement lors du déploiement du 01/11/2026
Le téléphone tournait auparavant avec une version de Guardian du 31/10 à 03h50 (antérieure à `cbfd4b4`).
À 16h50, une nouvelle version Release de Guardian a été installée sur le Pixel 8a.
À son démarrage, `GuardianEngine.enforceLockdown()` a balayé les applications installées :
1. `com.google.android.permissioncontroller` n'était pas dans la whitelist de l'utilisateur.
2. Il n'était pas dans `HARD_GUARDS` (nom AOSP différent).
3. La vérification du flag `FLAG_SYSTEM` n'existait plus dans `isGuarded()`.
4. Guardian a appelé `dpm.setApplicationHidden(admin, "com.google.android.permissioncontroller", true)`.

Au redémarrage, `system_server` a lu l'état masqué et s'est arrêté net.

---

## 3. Garde-fous et Règles Inviolables mis en place

Pour qu'**AUCUN** agent IA ni développeur ne reproduise ce désastre, les protections suivantes ont été gravées dans le code (commit `a2ab8ba`) :

### Règle d'or 1 : Immunité Inviolable de toute Application Système
`GuardianEngine.isGuarded()` et `WhitelistManager.isGuarded()` doivent **TOUJOURS** vérifier `ApplicationInfo.FLAG_SYSTEM` et `ApplicationInfo.FLAG_UPDATED_SYSTEM_APP`.  
Si `packageManager.getApplicationInfo()` échoue ou renvoie une exception, le package doit être **considéré comme système et protégé par défaut** (`true`).

```kotlin
fun isSystemPackage(ctx: Context, pkg: String): Boolean {
    return try {
        val ai = ctx.packageManager.getApplicationInfo(pkg, PackageManager.MATCH_UNINSTALLED_PACKAGES)
        (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
            (ai.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
    } catch (e: Exception) {
        // En cas de doute : toujours protégé !
        true
    }
}
```

### Règle d'or 2 : Barrière de Sécurité Directe sur `setApplicationHidden` et `setPackagesSuspended`
Même si un appelant externe ou un bug logique tente de masquer ou suspendre un package, `setPackageHiddenInternal` et `setPackageSuspendedInternal` doivent refuser l'appel si `isGuarded(pkg)` est vrai.

### Règle d'or 3 : Double Référencement AOSP + Google dans `HARD_GUARDS`
Les deux variantes doivent toujours figurer explicitement dans `HARD_GUARDS` :
- `com.android.permissioncontroller`
- `com.google.android.permissioncontroller`
- `com.android.packageinstaller`
- `com.google.android.packageinstaller`
- `com.samsung.android.packageinstaller`

### Règle d'or 4 : Interdiction de Déployer Guardian sans Audit de Diff
Avant toute installation de l'APK Guardian (`com.jo.custos.guardian`), l'agent IA doit obligatoirement inspecter le diff avec `git diff HEAD~1 guardian/` pour vérifier qu'aucune clause de protection des packages système n'a été altérée.

---

## 4. Statut et Récupération

- **Code source corrigé** : commit `a2ab8ba` poussé sur `origin/feat/guardian-architecture`.
- **Appareil réinitialisé** : Factory Reset effectué avec succès depuis le mode Recovery le 01/11/2026 à 17h20.
- **Fail-safe préservé** : La règle cardinale d'interdiction de `DISALLOW_FACTORY_RESET` (inscrite dans `AGENTS.md`) a prouvé son utilité vitale : sans elle, le Pixel 8a aurait été définitivement briqué.
