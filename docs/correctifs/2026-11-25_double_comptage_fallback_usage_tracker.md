# Correctif — Double-comptage du quota quotidien dans `FallbackUsageTracker` (25/11/2026)

## 1. Symptôme observé
* Sur le Google Pixel 8a (`41111JEKB22083`), **Instagram (`com.instagram.android`)** était suspendu au niveau OS (`suspended=true`) par **Guardian** pour dépassement de quota quotidien (`30 min / 1800 s`), alors que :
  * **Custos UI (`:app`)** et `UsageStatsManager` indiquaient seulement **`1534 s` (25 min 34 s / 30 min)** utilisés dans la journée logique (il restait **4 min 26 s**).
  * Le quota de sessions (10 min par session, 1 min de pause, max 14 sessions/jour) n'était ni en pause (`COOLDOWN`) ni épuisé.

---

## 2. Cause racine
Dans [`guardian/src/main/java/com/jo/custos/guardian/UsageQuery.kt`](../../guardian/src/main/java/com/jo/custos/guardian/UsageQuery.kt), le temps utilisé par une application est calculé via :
```kotlin
maxOf(fromStats, fromTracker)
```
où :
* `fromStats` est la somme des intervalles fermés `[ACTIVITY_RESUMED -> ACTIVITY_PAUSED]` retournés par `UsageStatsManager.queryEvents()`.
* `fromTracker` est le compteur de secours basé sur les événements d'Accessibilité (`TYPE_WINDOW_STATE_CHANGED`) dans [`FallbackUsageTracker.kt`](../../guardian/src/main/java/com/jo/custos/guardian/FallbackUsageTracker.kt).

Trois mécanismes combinés provoquaient un **double-comptage** dans `FallbackUsageTracker` :
1. **`syncFloorFromUsageStats` n'avançait pas `currentSinceMs` lorsque l'application était au premier plan (`currentPkg == pkg`)** :
   * Lorsqu'une transition d'activité interne (ex. ouverture/fermeture de `ModalActivity` ou `TransparentModalActivity` dans Instagram) ou l'ouverture du panneau de notifications (`com.android.systemui`, ignoré par `GuardianWatcherService`) émettait un `ACTIVITY_PAUSED` dans `UsageStatsManager`, `fromStats` intégrait immédiatement la durée écoulée depuis `currentSinceMs`.
   * La boucle d'évaluation (toutes les 1 s) appelait `syncFloorFromUsageStats()`, qui écrasait `usageMs[pkg] = statsMs` **sans mettre à jour `currentSinceMs = nowMs`**.
   * Dès la sortie de l'application (ou lors de `secondsFor()`), `flushCurrent(nowMs)` **ajoutait une seconde fois** l'intervalle `nowMs - currentSinceMs` par-dessus `usageMs[pkg]`.
2. **`onForeground` flushait à chaque `TYPE_WINDOW_STATE_CHANGED` intra-application** :
   * Même sans changement de package (`pkg == currentPkg`), chaque popup/modale interne déplaçait le temps ouvert dans `usageMs[pkg]` avant que `UsageStatsManager` ne ferme son intervalle.
3. **Absence de réconciliation à la baisse hors sabotage** :
   * `usageMs[pkg]` ne pouvait que monter (`if (statsMs > closedMs)`), si bien que toute dérive accumulée restait figée dans `fallback_usage.json` jusqu'au reset de 02h00.

---

## 3. Correctif appliqué
Dans [`guardian/src/main/java/com/jo/custos/guardian/FallbackUsageTracker.kt`](../../guardian/src/main/java/com/jo/custos/guardian/FallbackUsageTracker.kt) :
1. **Ignorer les événements `TYPE_WINDOW_STATE_CHANGED` redondants du même package** dans `onForeground()` (`if (pkg == currentPkg && currentSinceMs in 1 until nowMs) return`).
2. **Recaler `currentSinceMs = nowMs` lors d'un `syncFloorFromUsageStats` au premier plan** : si `currentPkg == pkg` et `statsMs > closedMs`, l'adoption de `statsMs` avance `currentSinceMs` à `nowMs` pour ne jamais compter deux fois le sous-intervalle déjà fermé dans `UsageStatsManager`.
3. **Réconcilier `usageMs[pkg]` sur `UsageStatsManager` en arrière-plan** : lorsque l'application a quitté le premier plan depuis au moins 3 secondes (`BACKGROUND_RECONCILE_GRACE_MS = 3_000L`, garantissant que `ACTIVITY_PAUSED` est écrit dans `UsageStatsManager`) et que `GuardianEngine.isUsageStatsHealthy(ctx)` est vrai (`GET_USAGE_STATS` actif, non saboté), `usageMs[pkg]` est recalé sur `statsMs`.
