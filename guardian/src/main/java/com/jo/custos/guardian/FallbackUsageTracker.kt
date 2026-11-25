package com.jo.custos.guardian

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File

/**
 * Accessibility-driven foreground-time tracker — F4 remediation.
 *
 * Independent of [android.app.usage.UsageStatsManager], so daily quotas keep working even if
 * an attacker revokes Guardian's usage-stats access via adb:
 *     appops set com.jo.custos.guardian android:get_usage_stats ignore
 * In that state `UsageStatsManager.queryEvents()` returns nothing (used=0s, quota never trips).
 *
 * [GuardianWatcherService] feeds foreground transitions here (it already receives every
 * TYPE_WINDOW_STATE_CHANGED); [UsageQuery.foregroundSeconds] then returns
 *     max(usageStatsSeconds, trackerSeconds)
 * so the quota is driven by whichever source has counted more.
 *
 * The logical day boundary is aligned with [GuardianEngine.dayStartMillis] (02:00 reset).
 * This tracker only works while the accessibility service is alive; the F1/F3 alarm backstop
 * re-enables that service if it is killed, so the two remediations compose.
 */
object FallbackUsageTracker {

    private const val TAG = "SelfControl.FallbackUsage"
    private const val FILE_NAME = "fallback_usage.json"
    private const val PERSIST_INTERVAL_MS = 15_000L

    private val lock = Any()
    private var loaded = false
    private var logicalDayStartMs = 0L
    private val usageMs = mutableMapOf<String, Long>()
    private val lastForegroundEndMs = mutableMapOf<String, Long>()
    private var currentPkg: String? = null
    private var currentSinceMs = 0L
    private var lastPersistMs = 0L
    private const val BACKGROUND_RECONCILE_GRACE_MS = 3_000L

    private fun ensureLoaded(ctx: Context) {
        if (loaded) return
        try {
            val f = File(ctx.filesDir, FILE_NAME)
            if (f.exists()) {
                val j = JSONObject(f.readText())
                logicalDayStartMs = j.optLong("logical_day_start_ms", 0L)
                val u = j.optJSONObject("usage_ms")
                if (u != null) {
                    val ks = u.keys()
                    while (ks.hasNext()) {
                        val k = ks.next()
                        usageMs[k] = u.getLong(k)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "load failed: ${e.message}")
        }
        loaded = true
    }

    /** Discards accumulated time when the logical day (02:00-aligned) has rolled over. */
    private fun rollDayIfNeeded() {
        val todayStart = GuardianEngine.dayStartMillis()
        if (todayStart != logicalDayStartMs) {
            logicalDayStartMs = todayStart
            usageMs.clear()
            lastForegroundEndMs.clear()
            currentPkg = null
            currentSinceMs = 0L
        }
    }

    private fun flushCurrent(nowMs: Long) {
        val p = currentPkg
        if (p != null && currentSinceMs in 1 until nowMs) {
            val start = maxOf(currentSinceMs, logicalDayStartMs)
            val delta = nowMs - start
            if (delta > 0) usageMs[p] = (usageMs[p] ?: 0L) + delta
            lastForegroundEndMs[p] = nowMs
        }
        currentSinceMs = nowMs
    }

    /** Called by GuardianWatcherService whenever the foreground package changes. */
    fun onForeground(ctx: Context, pkg: String, nowMs: Long) {
        if (pkg.isBlank()) return
        synchronized(lock) {
            ensureLoaded(ctx)
            rollDayIfNeeded()
            // Ignore intra-package window state changes (e.g. ModalActivity, dialogs, bottom sheets
            // inside the same app) so we don't prematurely move open time into usageMs before
            // UsageStatsManager closes its activity interval.
            if (pkg == currentPkg && currentSinceMs in 1 until nowMs) {
                return
            }
            flushCurrent(nowMs)
            currentPkg = pkg
            currentSinceMs = nowMs
            if (nowMs - lastPersistMs > PERSIST_INTERVAL_MS) {
                persist(ctx)
                lastPersistMs = nowMs
            }
        }
    }

    /** Closes any active foreground interval when the device screen is turned off. */
    fun onScreenOff(ctx: Context, nowMs: Long) {
        synchronized(lock) {
            if (currentPkg == null) return
            ensureLoaded(ctx)
            rollDayIfNeeded()
            flushCurrent(nowMs)
            currentPkg = null
            currentSinceMs = 0L
            persist(ctx)
            lastPersistMs = nowMs
        }
    }

    /**
     * Synchronizes the closed-intervals floor with UsageStatsManager when UsageStats is healthy.
     *
     * Bugfix (2026-11-25):
     * 1. When [pkg] is currently in the foreground ([currentPkg] == [pkg]) and [statsClosedSeconds]
     *    jumps above [usageMs] (because an intra-app activity like Instagram ModalActivity or
     *    SystemUI notification shade triggered ACTIVITY_PAUSED in UsageStatsManager), [statsMs]
     *    already includes the elapsed time since [currentSinceMs]. We must advance
     *    [currentSinceMs] = [nowMs] when adopting [statsMs], otherwise [secondsFor] and the next
     *    [flushCurrent] count that same interval a second time.
     * 2. When [pkg] has been in the background ([currentPkg] != [pkg]) for at least
     *    [BACKGROUND_RECONCILE_GRACE_MS] (so UsageStatsManager has committed its ACTIVITY_PAUSED)
     *    and [statsClosedSeconds] > 0 (proving GET_USAGE_STATS is operational, not sabotaged),
     *    reconcile [usageMs] directly to [statsMs] so any drift from SystemUI overlays or prior
     *    double-counting is healed automatically.
     */
    fun syncFloorFromUsageStats(ctx: Context, pkg: String, statsClosedSeconds: Int, nowMs: Long) {
        if (statsClosedSeconds <= 0) return
        synchronized(lock) {
            ensureLoaded(ctx)
            rollDayIfNeeded()
            val statsMs = statsClosedSeconds * 1000L
            val closedMs = usageMs[pkg] ?: 0L
            if (currentPkg == pkg) {
                if (statsMs > closedMs) {
                    usageMs[pkg] = statsMs
                    if (currentSinceMs in 1 until nowMs) {
                        currentSinceMs = nowMs
                    }
                    if (nowMs - lastPersistMs > PERSIST_INTERVAL_MS) {
                        persist(ctx)
                        lastPersistMs = nowMs
                    }
                }
            } else {
                val leftAt = lastForegroundEndMs[pkg] ?: 0L
                val canReconcileDown = (nowMs - leftAt) >= BACKGROUND_RECONCILE_GRACE_MS &&
                    GuardianEngine.isUsageStatsHealthy(ctx)
                if (statsMs > closedMs || (canReconcileDown && statsMs != closedMs)) {
                    usageMs[pkg] = statsMs
                    persist(ctx)
                    lastPersistMs = nowMs
                }
            }
        }
    }

    /** Foreground seconds counted for [pkg] during the current logical day, including the live interval. */
    fun secondsFor(ctx: Context, pkg: String, nowMs: Long): Int {
        synchronized(lock) {
            ensureLoaded(ctx)
            rollDayIfNeeded()
            var ms = usageMs[pkg] ?: 0L
            if (currentPkg == pkg && currentSinceMs in 1 until nowMs) {
                ms += nowMs - maxOf(currentSinceMs, logicalDayStartMs)
            }
            return (ms / 1000L).toInt().coerceAtLeast(0)
        }
    }

    private fun persist(ctx: Context) {
        try {
            val j = JSONObject()
            j.put("logical_day_start_ms", logicalDayStartMs)
            val u = JSONObject()
            for ((k, v) in usageMs) u.put(k, v)
            j.put("usage_ms", u)
            File(ctx.filesDir, FILE_NAME).writeText(j.toString())
        } catch (e: Exception) {
            Log.e(TAG, "persist failed: ${e.message}")
        }
    }
}
