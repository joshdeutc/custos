package com.jo.selfcontrol.ultimate

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.Calendar

/**
 * Foreground Service.
 * Central hub for monitoring usage and enforcing limits.
 */
class LimitService : Service() {

    companion object {
        private const val TAG = "SelfControl.Service"
        private const val CHANNEL_ID = "selfcontrol_monitor"
        private const val CHOICE_CHANNEL_ID = "selfcontrol_choices"
        private const val NOTIFICATION_ID = 1
        private const val ENFORCE_INTERVAL_MS = 1_000L   // 1 second

        @Volatile
        var isRunning = false
            private set

        @Volatile
        private var instance: LimitService? = null

        fun getUsageData(): Map<String, Int> = instance?.usageToday?.toMap() ?: emptyMap()
        fun getLimits(): Map<String, ConfigManager.AppLimit> = instance?.limitsByPackage?.toMap() ?: emptyMap()
        fun getBlockedApps(): Set<String> = instance?.suspendedApps?.toSet() ?: emptySet()
        fun getNuclearState(): NuclearManager.NuclearState? = instance?.nuclearState

        fun startNuclearMode(
            packages: List<String>,
            durationMs: Long
        ) {
            instance?.activateNuclearMode(packages, durationMs)
        }

        fun requestCancelNuclearMode() {
            instance?.requestCancelNuclearModeInternal()
        }

        fun cancelPendingNuclearCancel() {
            instance?.cancelPendingNuclearCancelInternal()
        }

        /** Debug-only: force a quota block for automated tests. */
        fun forceBlockForTest(packageName: String) {
            val inst = instance ?: return
            // Set usage to 1s — enough to exceed a 0s limit, but low enough
            // that raising the limit will correctly unblock the app.
            inst.usageToday[packageName] = 1
            inst.blockApp(packageName, "quota")
        }

        fun start(context: Context) {
            try {
                val intent = Intent(context, LimitService::class.java)
                context.startForegroundService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start FGS: ${e.message}")
            }
        }

        fun handleBlockedNotificationChoice(context: Context, packageName: String, mute: Boolean) {
            BlockedNotificationManager.setMuteWhenBlockedPreference(context, packageName, mute)
            instance?.onBlockedNotificationChoiceApplied(packageName, mute)
        }

        fun notifyConfigChanged(context: Context) {
            val inst = instance
            if (inst != null) {
                inst.checkConfigReload(force = true)
            } else {
                start(context)
            }
        }
    }

    private lateinit var config: ConfigManager.Config
    private val limitsByPackage = mutableMapOf<String, ConfigManager.AppLimit>()
    private val periodBlockRules = mutableListOf<ConfigManager.PeriodBlockRule>()

    // Usage tracking per day in seconds
    private val usageToday = mutableMapOf<String, Int>()
    private var trackingDay = -1
    // Persisted alongside usageToday so we can detect "same logical day" on restart
    // without relying on DAY_OF_YEAR (which collides across years).
    private var logicalDayStartMs: Long = 0L
    private var lastPersistMs: Long = 0L
    private val PERSIST_THROTTLE_MS = 5_000L

    // Simulated "suspended" apps list representing apps blocked by HOME spam
    private val suspendedApps = mutableSetOf<String>()
    private val promptedBlockedNotificationChoice = mutableSetOf<String>()
    private val currentlyMutedBySelfControl = mutableSetOf<String>()
    private val curfewMutedApps = mutableSetOf<String>() // apps muted specifically by curfew rules

    @Volatile
    private var nuclearState: NuclearManager.NuclearState? = null

    private var lastSystemQueryTime = 0L
    private var lastEnforceTime = 0L
    private var lastA11YReEnableTime = 0L
    private val SYSTEM_SYNC_INTERVAL_MS = 3_000L
    private val A11Y_REENABLE_INTERVAL_MS = 30_000L

    @Volatile
    private var startupComplete = false

    private var configLastModified = 0L

    /** Kept so onDestroy can unregister it — see [registerPackageAddedReceiver]. */
    private var packageAddedReceiver: android.content.BroadcastReceiver? = null

    private val nuclearMutedApps = mutableSetOf<String>()

    private val checkLimitsAndDelayHandler = Handler(Looper.getMainLooper())
    private val enforceRunnable = object : Runnable {
        override fun run() {
            // Regularly check if pending delay actions should be applied
            DelayManager.applyPendingConfigsIfReady(this@LimitService)
            DelayManager.applyPendingDelayIfReady(this@LimitService)
            WhitelistManager.checkAndPromotePendingRequests(this@LimitService)
            checkConfigReload()
            checkA11YReEnable()

            enforceLimit()
            checkLimitsAndDelayHandler.postDelayed(this, ENFORCE_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "🚀 LimitService onCreate")
        instance = this
        GuardianClient.init(this)

        // Re-apply Device Owner policies on every service start. Idempotent + heals any drift.
        if (DeviceOwnerHelper.isDeviceOwner(this)) {
            DeviceOwnerHelper.applyInitialPolicies(this)
            Log.i(TAG, "🔐 Device Owner policies applied")
        }

        // Restore persisted state BEFORE clearing OS suspensions. If we have valid state
        // from the same logical day, we want to keep apps suspended (re-applied below).
        val currentDayStart = dayStartMillis()
        val persisted = UsageStateStore.load(this)
        val restoreSameDay = persisted != null && persisted.logicalDayStartMs == currentDayStart
        if (restoreSameDay) {
            usageToday.putAll(persisted!!.usageToday)
            suspendedApps.addAll(persisted.suspendedApps)
            for (pkg in persisted.suspendedApps) {
                AppWatcherService.blockedApps.add(pkg)
            }
            logicalDayStartMs = persisted.logicalDayStartMs
            Log.i(TAG, "♻️ Restored state: ${usageToday.size} tracked, ${suspendedApps.size} suspended")
        } else {
            if (persisted != null) {
                Log.i(TAG, "📅 Persisted state from previous logical day — discarding")
            }
            logicalDayStartMs = currentDayStart
        }

        // Config must be loaded BEFORE the suspension sweep below: install-blocked packages
        // have to be named in `keep`, or the sweep would un-suspend the ones we hold via the
        // suspension fallback (hideApp refused) and leave them usable until the next tick.
        config = ConfigManager.loadConfig(this)
        configLastModified = ConfigManager.getConfigLastModified(this)
        limitsByPackage.clear()
        periodBlockRules.clear()
        for (limit in config.limits) {
            limitsByPackage[limit.packageName] = limit
            Log.i(TAG, "   ${limit.packageName} → ${limit.maxSecondsPerDay}s/day")
        }
        periodBlockRules.addAll(config.periodBlocks)
        if (config.limits.isEmpty()) {
            Log.i(TAG, "📋 No limits configured (empty config)")
        } else {
            Log.i(TAG, "📋 Config loaded : ${config.limits.size} rule(s)")
        }
        if (periodBlockRules.isNotEmpty()) {
            Log.i(TAG, "🌙 Period blocks: ${periodBlockRules.size} rule(s)")
        }

        if (DeviceOwnerHelper.isDeviceOwner(this)) {
            // Sweep stale OS-level suspensions. On flavors where OS suspension is disabled (me, basic, admin),
            // NO apps should ever remain suspended (no icon graying out — only Accessibility HOME return).
            val keep = if (BuildConfig.OS_SUSPENSION_ENABLED) {
                suspendedApps + InstallBlockManager.blockedInstalledPackages(this, config)
            } else {
                emptySet()
            }
            DeviceOwnerHelper.clearAllStuckSuspensions(this, keep = keep)
            if (BuildConfig.OS_SUSPENSION_ENABLED) {
                for (pkg in suspendedApps) {
                    DeviceOwnerHelper.suspendApp(this, pkg)
                }
            }
        }

        // Catch up on anything installed while the service was dead, and release groups whose
        // protection timer expired and were removed from the config in the meantime.
        InstallBlockManager.enforce(this, config)
        WhitelistManager.enforce(this)
        registerPackageAddedReceiver()

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Starting..."))
        isRunning = true
        WatchdogReceiver.schedule(this)
        DailyResetReceiver.schedule(this)
        currentlyMutedBySelfControl.clear()
        currentlyMutedBySelfControl.addAll(BlockedNotificationManager.getCurrentlyMutedBySelfControl(this))
        // Restore muted packages into the NotificationListener in-memory set
        SelfControlNotificationListener.mutedPackages.addAll(currentlyMutedBySelfControl)

        loadNuclearState()

        updateNotification("Active — ${config.limits.size} app(s) monitored")

        if (limitsByPackage.isNotEmpty()) {
            Thread {
                Thread.sleep(1000)
                Handler(Looper.getMainLooper()).post {
                    trackingDay = Calendar.getInstance().apply {
                        add(Calendar.HOUR_OF_DAY, -2)
                    }.get(Calendar.DAY_OF_YEAR)

                    // Only query UsageStatsManager on true cold start (no persisted state
                    // for the current logical day). Restored state is more reliable than
                    // queryAndAggregateUsageStats which has bucket-aggregation quirks.
                    if (!restoreSameDay) {
                        loadTodayUsageFromSystem()
                    }
                    persistState()
                    lastEnforceTime = System.currentTimeMillis()
                    startupComplete = true
                    Log.i(TAG, "✅ Startup complete — enforcing immediately")
                    enforceLimit()
                }
            }.start()
        } else {
            startupComplete = true
        }

        checkLimitsAndDelayHandler.postDelayed(enforceRunnable, ENFORCE_INTERVAL_MS)
        Log.i(TAG, "⏱️ Enforcement started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "🔄 LimitService onStartCommand")
        return START_STICKY
    }

    private fun dayStartMillis(): Long {
        val cal = Calendar.getInstance()
        if (cal.get(Calendar.HOUR_OF_DAY) < 2) {
            cal.add(Calendar.DATE, -1)
        }
        cal.apply {
            set(Calendar.HOUR_OF_DAY, 2)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun syncUsageFromSystem(pkg: String) {
        val systemSeconds = UsageQuery.foregroundSeconds(this, pkg, dayStartMillis(), System.currentTimeMillis())
        if (systemSeconds > 0) {
            val localSeconds = usageToday[pkg] ?: 0
            val best = maxOf(systemSeconds, localSeconds)
            usageToday[pkg] = best
        }
    }

    private fun loadTodayUsageFromSystem() {
        val seconds = UsageQuery.foregroundSecondsByPackage(this, dayStartMillis(), System.currentTimeMillis())
        for ((pkg, limit) in limitsByPackage) {
            val usedSeconds = seconds[pkg] ?: continue
            usageToday[pkg] = usedSeconds
            if (usedSeconds >= limit.maxSecondsPerDay) {
                blockApp(pkg, "quota")
            }
        }
        Log.i(TAG, "📊 Initial system usage loaded (queryEvents)")
    }

    private fun enforceLimit() {
        if (!startupComplete) return

        checkNuclearExpiration()
        checkNuclearCancelReady()
        enforceNuclearNotificationMuting()

        val calReset = Calendar.getInstance()
        calReset.add(Calendar.HOUR_OF_DAY, -2)
        val logicalToday = calReset.get(Calendar.DAY_OF_YEAR)
        if (logicalToday != trackingDay) {
            trackingDay = logicalToday
            logicalDayStartMs = dayStartMillis()
            usageToday.clear()
            Log.i(TAG, "📅 New day detected — resetting counters...")

            val appsToCheck = suspendedApps.toList()
            for (pkg in appsToCheck) {
                if (!isAppBlocked(pkg)) {
                    suspendedApps.remove(pkg)
                    AppWatcherService.blockedApps.remove(pkg)
                    DeviceOwnerHelper.unsuspendApp(this, pkg)
                    ensureNotificationUnmuted(pkg)
                    curfewMutedApps.remove(pkg)
                    nuclearMutedApps.remove(pkg)
                    onAppFullyUnblocked(pkg)
                    Log.i(TAG, "🔓 Midnight unblock: $pkg (no other restrictions)")
                } else {
                    Log.i(TAG, "🌙 Midnight rollover: $pkg remains blocked (curfew/session/nuclear active)")
                }
            }

            SessionManager.resetDayIfNeeded(this, logicalToday)
            // Skip loadTodayUsageFromSystem at rollover — its UsageStatsManager query
            // can return yesterday's data (bucket aggregation quirk) and immediately re-block
            // apps that should be free. Restart in-memory counter from zero.
            persistState()
        }

        var currentApp = AppWatcherService.currentForegroundApp
        val now = System.currentTimeMillis()

        // End sessions for any pkg the user is no longer foregrounding.
        // Done up-front so cooldown timestamp reflects the actual leave instant.
        endSessionsForLeftPackages(currentApp, now)
        val gap = now - lastEnforceTime
        lastEnforceTime = now

        // If service was asleep/killed for more than 5s, force immediate system sync
        val syncNeeded = if (gap > 5_000L) {
            Log.w(TAG, "⚠️ Gap detected: ${gap / 1000}s since last enforce — forcing system sync")
            true
        } else {
            now - lastSystemQueryTime > SYSTEM_SYNC_INTERVAL_MS
        }

        if (syncNeeded) {
            lastSystemQueryTime = now
            for ((pkg, _) in limitsByPackage) {
                syncUsageFromSystem(pkg)
            }
        }

        // Enforce curfew notification muting for ALL packages in period block rules
        enforceCurfewNotificationMuting()

        // Time-boxed install window enforcement
        InstallWindowManager.enforce(this)

        // Check if any suspended apps should be unblocked (curfew ended, allowed hours started, etc.)
        checkAndUnblockApps()

        if (currentApp != "unknown") {
            if (isCurrentlyPeriodBlocked(currentApp)) {
                Log.w(TAG, "🌙 BLOCK REASON: curfew period | $currentApp")
                blockApp(currentApp, "curfew")
                return
            }
            AppWatcherService.recheckScreenRules(currentApp)
        }

        if (currentApp in InstallWindowManager.INSTALLER_PACKAGES && InstallWindowManager.isOpen(this)) {
            // Installer app (e.g. Play Store) is permitted while the install window is open
            return
        }

        val limit = limitsByPackage[currentApp] ?: return

        val dayOfWeek = calReset.get(Calendar.DAY_OF_WEEK) - 1
        if (dayOfWeek !in limit.allowedDays) {
            blockApp(currentApp, "day")
            return
        }

        if (!limit.allDay) {
            val cal = Calendar.getInstance()
            val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            if (nowMinutes < limit.allowedHoursStart || nowMinutes >= limit.allowedHoursEnd) {
                blockApp(currentApp, "hour")
                return
            }
        }

        val used = (usageToday[currentApp] ?: 0) + 1
        usageToday[currentApp] = used
        persistStateThrottled()

        val remaining = limit.maxSecondsPerDay - used

        if (remaining <= 0) {
            Log.w(TAG, "🚫 BLOCK REASON: limit reached | $currentApp | used=${used}s / max=${limit.maxSecondsPerDay}s")
            blockApp(currentApp, "quota")
            return
        }

        // Session limit (lower priority than curfew + quota): only evaluated once those pass.
        val session = limit.session
        if (session != null) {
            when (SessionManager.evaluate(this, currentApp, session, now)) {
                SessionManager.Result.BLOCKED_COOLDOWN -> {
                    Log.w(TAG, "🚫 BLOCK REASON: session cooldown | $currentApp")
                    blockApp(currentApp, "session_cooldown")
                    return
                }
                SessionManager.Result.BLOCKED_DAILY -> {
                    Log.w(TAG, "🚫 BLOCK REASON: session daily limit | $currentApp")
                    blockApp(currentApp, "session_daily")
                    return
                }
                SessionManager.Result.IN_SESSION,
                SessionManager.Result.STARTED_SESSION -> { /* allow */ }
            }
        }

        val remainMin = remaining / 60
        val remainSec = remaining % 60
        val parts = mutableListOf("${remainMin}m${remainSec}s daily")
        var overlayText: String? = null
        if (session != null) {
            val status = SessionManager.statusOf(this, currentApp, session, now)
            if (status.state == SessionManager.Status.State.ACTIVE) {
                val sm = status.sessionRemainingSec / 60
                val ss = status.sessionRemainingSec % 60
                parts.add("session ${sm}m${ss}s")
                overlayText = "⏱ ${sm}m${"%02d".format(ss)}s · ${status.sessionsUsed}/${status.maxSessionsPerDay}"
            }
        }
        updateNotification("$currentApp : ${parts.joinToString(" / ")}")
        AppWatcherService.updateSessionOverlay(overlayText)
    }

    /**
     * End active sessions ONLY when the user has actually returned to the home screen / launcher.
     *
     * A session must survive transient foreground changes that happen *inside* the app's own
     * flow — dialogs, custom-tab/webview, share sheets, the soft keyboard, the system UI, the
     * brief window swap while sending a message, etc. Only reaching Home means "I'm done with
     * this app", so that's the single trigger that closes a session and starts its cooldown.
     * (Switching directly to another app without going Home leaves the session active; the
     * session-duration timer still expires it on its own.)
     */
    private fun endSessionsForLeftPackages(currentApp: String, now: Long) {
        if (currentApp !in launcherPackages()) return
        val active = SessionManager.activePackages(this)
        if (active.isEmpty()) return
        EventLog.log(this, "SESSION", "home reached ($currentApp) → ending active sessions: $active")
        for (pkg in active) {
            val cfg = limitsByPackage[pkg]?.session ?: continue
            SessionManager.endSessionIfActive(this, pkg, cfg, now)
        }
    }

    private var launcherPackagesCache: Set<String>? = null

    /** Packages able to act as Home (the launcher). Cached — the default launcher rarely changes. */
    private fun launcherPackages(): Set<String> {
        launcherPackagesCache?.let { return it }
        val pkgs = mutableSetOf<String>()
        try {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            for (ri in packageManager.queryIntentActivities(intent, 0)) {
                pkgs.add(ri.activityInfo.packageName)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Launcher resolve failed: ${e.message}")
        }
        launcherPackagesCache = pkgs
        Log.i(TAG, "🏠 Home/launcher packages: $pkgs")
        return pkgs
    }

    /**
     * Write current state (usage, suspendedApps, logical-day boundary) to disk so a
     * service kill mid-day doesn't lose quota progress or which apps are blocked.
     */
    private fun persistState() {
        lastPersistMs = System.currentTimeMillis()
        UsageStateStore.persist(this, logicalDayStartMs, usageToday, suspendedApps)
    }

    /**
     * Throttled variant for high-frequency calls (per-second usage tick). Block/unblock
     * paths call persistState() directly so suspension changes are durable immediately.
     */
    private fun persistStateThrottled() {
        if (System.currentTimeMillis() - lastPersistMs < PERSIST_THROTTLE_MS) return
        persistState()
    }

    /**
     * Curfew: wall-clock minutes; overrides daily quota (checked before usage increment).
     */
    private fun isCurrentlyPeriodBlocked(packageName: String): Boolean {
        if (periodBlockRules.isEmpty()) return false
        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
        val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        for (rule in periodBlockRules) {
            if (packageName !in rule.packages) continue
            if (dayOfWeek !in rule.allowedDays) continue
            if (ConfigManager.isInBlockedWindow(nowMin, rule.blockedStartMinutes, rule.blockedEndMinutes)) {
                return true
            }
        }
        return false
    }

    private fun shouldStayBlockedForNonCurfewReasons(packageName: String): Boolean {
        if (packageName in InstallWindowManager.INSTALLER_PACKAGES && InstallWindowManager.isOpen(this)) {
            return false
        }
        val nuclear = nuclearState
        if (nuclear != null && nuclear.active && !NuclearManager.isExpired(nuclear) &&
            packageName in nuclear.blockedPackages
        ) {
            return true
        }
        val limit = limitsByPackage[packageName] ?: return false
        val calDw = Calendar.getInstance()
        calDw.add(Calendar.HOUR_OF_DAY, -2)
        val dayOfWeek = calDw.get(Calendar.DAY_OF_WEEK) - 1
        if (dayOfWeek !in limit.allowedDays) return true
        if (!limit.allDay) {
            val cal = Calendar.getInstance()
            val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            if (nowMinutes < limit.allowedHoursStart || nowMinutes >= limit.allowedHoursEnd) {
                return true
            }
        }
        val used = usageToday[packageName] ?: 0
        if (used >= limit.maxSecondsPerDay) return true
        val session = limit.session
        if (session != null && SessionManager.shouldStayBlocked(this, packageName, session, System.currentTimeMillis())) {
            return true
        }
        return false
    }

    private fun isAppBlocked(packageName: String): Boolean {
        if (packageName in InstallWindowManager.INSTALLER_PACKAGES && InstallWindowManager.isOpen(this)) {
            return false
        }
        if (isCurrentlyPeriodBlocked(packageName)) return true
        return shouldStayBlockedForNonCurfewReasons(packageName)
    }

    private fun onAppFullyUnblocked(packageName: String) {
        BlockedNotificationManager.clearPreference(this, packageName)
        promptedBlockedNotificationChoice.remove(packageName)
        Log.i(TAG, "🔄 Notification preference reset for $packageName (app completely unblocked)")
    }

    /**
     * For each active curfew (period block rule), evaluate notification muting per app:
     * - If the user chose "mute" for this app (BlockedNotificationManager): mute it.
     * - If the user chose "keep": keep notifications visible.
     * - If the user hasn't chosen yet: prompt them via an interactive notification.
     * When curfew ends, unmute packages that are no longer restricted.
     */
    private fun enforceCurfewNotificationMuting() {
        if (periodBlockRules.isEmpty()) {
            for (pkg in curfewMutedApps.toList()) {
                ensureNotificationUnmuted(pkg)
                curfewMutedApps.remove(pkg)
            }
            return
        }

        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
        val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        val activeCurfewPackages = mutableSetOf<String>()
        for (rule in periodBlockRules) {
            if (dayOfWeek !in rule.allowedDays) continue
            if (!ConfigManager.isInBlockedWindow(nowMin, rule.blockedStartMinutes, rule.blockedEndMinutes)) continue
            activeCurfewPackages.addAll(rule.packages)
        }

        val shouldBeMuted = mutableSetOf<String>()
        for (pkg in activeCurfewPackages) {
            val pref = BlockedNotificationManager.getMuteWhenBlockedPreference(this, pkg)
            if (pref == null) {
                maybePromptBlockedNotificationChoice(pkg, "curfew")
            } else if (pref) {
                shouldBeMuted.add(pkg)
            }
        }

        // Mute newly curfew-blocked apps
        for (pkg in shouldBeMuted) {
            if (pkg !in curfewMutedApps) {
                ensureNotificationMuted(pkg)
                curfewMutedApps.add(pkg)
                Log.i(TAG, "Curfew: muted notifications for $pkg")
            }
        }

        // Unmute apps whose curfew ended
        for (pkg in curfewMutedApps.toList()) {
            if (pkg !in shouldBeMuted) {
                curfewMutedApps.remove(pkg)
                Log.i(TAG, "Curfew ended: unmuted notifications for $pkg")
                if (!isAppBlocked(pkg)) {
                    ensureNotificationUnmuted(pkg)
                    onAppFullyUnblocked(pkg)
                }
            }
        }
    }

    /**
     * Check all currently suspended apps and unblock any that no longer need blocking.
     * This handles: curfew ended, allowed hours started, day-of-week changed, quota reset, session cooldown ended.
     */
    private fun checkAndUnblockApps() {
        for (pkg in suspendedApps.toList()) {
            if (isAppBlocked(pkg)) continue

            // No reason to keep blocking — app is completely unblocked
            suspendedApps.remove(pkg)
            AppWatcherService.blockedApps.remove(pkg)
            DeviceOwnerHelper.unsuspendApp(this, pkg)
            ensureNotificationUnmuted(pkg)
            curfewMutedApps.remove(pkg)
            nuclearMutedApps.remove(pkg)
            onAppFullyUnblocked(pkg)
            Log.i(TAG, "🔓 Unblocked $pkg (no longer restricted)")
            EventLog.log(this, "UNBLOCK", "$pkg (no longer restricted)")
            persistState()
        }
    }

    private fun blockApp(packageName: String, reason: String) {
        val alreadyTracked = packageName in suspendedApps
        if (!alreadyTracked) {
            EventLog.log(this, "BLOCK", "$packageName reason=$reason (fg=${AppWatcherService.currentForegroundApp})")
        }
        suspendedApps.add(packageName)
        AppWatcherService.blockedApps.add(packageName)
        // OS-level suspension via Device Owner — clean, instant, no UI flash.
        // No-op when not DO; A11Y HOME spam acts as fallback.
        DeviceOwnerHelper.suspendApp(this, packageName)
        if (!alreadyTracked) {
            updateNotification("🚫 $packageName blocked")
            handleBlockedNotificationPolicyOnBlock(packageName, reason)
            persistState()
        }
        // Hide the session overlay — the user is being HOME-spammed.
        AppWatcherService.updateSessionOverlay(null)
        // Force immediate HOME if this is the current foreground app
        // (don't wait for the next AccessibilityEvent window change)
        if (packageName == AppWatcherService.currentForegroundApp) {
            AppWatcherService.forceHomeIfBlocked()
        }
    }

    private fun loadNuclearState() {
        val state = NuclearManager.loadState(this) ?: return
        if (state.active && !NuclearManager.isExpired(state)) {
            nuclearState = state
            for (pkg in state.blockedPackages) {
                AppWatcherService.blockedApps.add(pkg)
            }
            enforceNuclearNotificationMuting()
            Log.i(TAG, "☢️ Nuclear mode restored: ${state.blockedPackages.size} apps blocked")
        } else if (state.active) {
            NuclearManager.clearState(this)
        }
    }

    private fun checkNuclearExpiration() {
        val state = nuclearState ?: return
        if (NuclearManager.isExpired(state)) {
            for (pkg in state.blockedPackages) {
                if (pkg !in suspendedApps) {
                    AppWatcherService.blockedApps.remove(pkg)
                }
            }
            nuclearState = null
            NuclearManager.clearState(this)
            enforceNuclearNotificationMuting()
            updateNotification("Nuclear mode finished")
        }
    }

    private fun checkNuclearCancelReady() {
        val state = nuclearState ?: return
        if (!NuclearManager.isCancelReady(state)) return
        Log.i(TAG, "☢️ Nuclear cancel delay elapsed, ending nuclear mode")
        for (pkg in state.blockedPackages) {
            if (pkg !in suspendedApps) {
                AppWatcherService.blockedApps.remove(pkg)
            }
        }
        nuclearState = null
        NuclearManager.clearState(this)
        enforceNuclearNotificationMuting()
        updateNotification("Nuclear mode cancelled (delay complete)")
    }

    private fun activateNuclearMode(
        packages: List<String>,
        durationMs: Long
    ) {
        val endTime = System.currentTimeMillis() + durationMs
        val state = NuclearManager.NuclearState(
            active = true,
            endTimestamp = endTime,
            blockedPackages = packages
        )
        nuclearState = state
        NuclearManager.saveState(this, state)

        for (pkg in packages) {
            AppWatcherService.blockedApps.add(pkg)
        }
        enforceNuclearNotificationMuting()
        val minutes = durationMs / 60_000
        updateNotification("☢️ Nuclear: ${packages.size} apps blocked (${minutes}min)")
    }

    /**
     * For Nuclear Mode, evaluate notification muting per app (no system-wide DND):
     * - If preference is "mute": mute it.
     * - If preference is "keep": keep notifications visible.
     * - If preference is null: prompt user to choose via notification.
     * When Nuclear ends, unmute packages.
     */
    private fun enforceNuclearNotificationMuting() {
        val state = nuclearState
        if (state == null || !state.active || NuclearManager.isExpired(state)) {
            for (pkg in nuclearMutedApps.toList()) {
                nuclearMutedApps.remove(pkg)
                if (!isAppBlocked(pkg)) {
                    ensureNotificationUnmuted(pkg)
                    onAppFullyUnblocked(pkg)
                }
            }
            return
        }

        val shouldBeMuted = mutableSetOf<String>()
        for (pkg in state.blockedPackages) {
            val pref = BlockedNotificationManager.getMuteWhenBlockedPreference(this, pkg)
            if (pref == null) {
                maybePromptBlockedNotificationChoice(pkg, "nuclear")
            } else if (pref) {
                shouldBeMuted.add(pkg)
            }
        }

        for (pkg in shouldBeMuted) {
            if (pkg !in nuclearMutedApps) {
                ensureNotificationMuted(pkg)
                nuclearMutedApps.add(pkg)
                Log.i(TAG, "☢️ Nuclear: muted notifications for $pkg")
            }
        }

        for (pkg in nuclearMutedApps.toList()) {
            if (pkg !in shouldBeMuted) {
                nuclearMutedApps.remove(pkg)
                Log.i(TAG, "☢️ Nuclear: unmuted notifications for $pkg")
                if (!isAppBlocked(pkg)) {
                    ensureNotificationUnmuted(pkg)
                    onAppFullyUnblocked(pkg)
                }
            }
        }
    }

    /**
     * Periodically (every 30s) re-write the secure setting that lists enabled accessibility
     * services so AppWatcherService stays bound even if the user toggles it off in Settings.
     * No-op when the app is not Device Owner.
     */
    private fun checkA11YReEnable() {
        val now = System.currentTimeMillis()
        if (now - lastA11YReEnableTime < A11Y_REENABLE_INTERVAL_MS) return
        lastA11YReEnableTime = now
        if (DeviceOwnerHelper.isDeviceOwner(this)) {
            DeviceOwnerHelper.enforceA11YReEnable(this)
        }
    }

    fun checkConfigReload(force: Boolean = false) {
        try {
            val newModified = ConfigManager.getConfigLastModified(this)
            if (!force && newModified <= configLastModified) return

            val newConfig = ConfigManager.loadConfig(this)
            configLastModified = newModified

            val newPackages = newConfig.limits.map { it.packageName }.toSet()
            val oldPackages = limitsByPackage.keys.toSet()

            var changed = false
            for (pkg in oldPackages - newPackages) {
                limitsByPackage.remove(pkg)
                usageToday.remove(pkg)
                if (pkg in suspendedApps) {
                    suspendedApps.remove(pkg)
                    AppWatcherService.blockedApps.remove(pkg)
                    DeviceOwnerHelper.unsuspendApp(this, pkg)
                    ensureNotificationUnmuted(pkg)
                    Log.i(TAG, "🔓 Config reload — $pkg removed, unblocked")
                }
                changed = true
            }

            for (limit in newConfig.limits) {
                limitsByPackage[limit.packageName] = limit
            }
            config = newConfig
            periodBlockRules.clear()
            periodBlockRules.addAll(newConfig.periodBlocks)
            if (changed) persistState()

            // Picks up newly added groups AND releases packages whose group's protection timer
            // ran out — this is the path a queued removal takes once DelayManager applies it.
            InstallBlockManager.enforce(this, newConfig)
            WhitelistManager.enforce(this)

            updateNotification("Config reloaded — ${newPackages.size} app(s)")
            Log.i(TAG, "🔄 Config reloaded")
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Error reloading config: ${e.message}")
        }
    }

    private fun requestCancelNuclearModeInternal() {
        val state = nuclearState ?: return
        if (state.cancelExecuteAt > 0L) return
        val delaySec = DelayManager.getCurrentEffectiveDelaySeconds(this)
        val now = System.currentTimeMillis()
        val updated = NuclearManager.withCancelRequested(state, now, now + delaySec * 1000L)
        nuclearState = updated
        NuclearManager.saveState(this, updated)
        updateNotification("Nuclear cancel requested (${delaySec}s delay)")
    }

    private fun cancelPendingNuclearCancelInternal() {
        val state = nuclearState ?: return
        if (state.cancelExecuteAt <= 0L) return
        val updated = NuclearManager.withCancelCleared(state)
        nuclearState = updated
        NuclearManager.saveState(this, updated)
        updateNotification("Nuclear cancel request removed")
    }

    /**
     * Runtime-registered so it is exempt from the Android 8+ restrictions on manifest-declared
     * implicit broadcasts — `ACTION_PACKAGE_ADDED` would otherwise never reach us. The service is
     * a permanent foreground service, so its lifetime is the right scope for this.
     */
    private fun registerPackageAddedReceiver() {
        if (packageAddedReceiver != null) return
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val pkg = intent.data?.schemeSpecificPart ?: return
                InstallBlockManager.onPackageAdded(ctx, pkg)
                WhitelistManager.onPackageAdded(ctx, pkg)
            }
        }
        val filter = android.content.IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        try {
            registerReceiver(receiver, filter)
            packageAddedReceiver = receiver
            Log.i(TAG, "📦 PACKAGE_ADDED receiver registered (install blocklist)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register PACKAGE_ADDED receiver: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        checkLimitsAndDelayHandler.removeCallbacks(enforceRunnable)
        packageAddedReceiver?.let {
            runCatching { unregisterReceiver(it) }
            packageAddedReceiver = null
        }
        Log.i(TAG, "💀 LimitService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_desc)
            setShowBadge(false)
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)

        val choiceChannel = NotificationChannel(
            CHOICE_CHANNEL_ID,
            "Custos choices",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Action prompts when an app becomes blocked"
            setShowBadge(false)
        }
        nm.createNotificationChannel(choiceChannel)
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Custos")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun handleBlockedNotificationPolicyOnBlock(packageName: String, reason: String) {
        val pref = BlockedNotificationManager.getMuteWhenBlockedPreference(this, packageName)
        if (pref == null) {
            maybePromptBlockedNotificationChoice(packageName, reason)
            return
        }
        if (pref) {
            ensureNotificationMuted(packageName)
        } else {
            ensureNotificationUnmuted(packageName)
        }
    }

    private fun maybePromptBlockedNotificationChoice(packageName: String, reason: String) {
        if (packageName in promptedBlockedNotificationChoice) return
        promptedBlockedNotificationChoice.add(packageName)

        val appName = try {
            val ai = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(ai).toString()
        } catch (_: Exception) { packageName }

        val muteIntent = Intent(this, BlockedNotificationChoiceReceiver::class.java).apply {
            action = BlockedNotificationChoiceReceiver.ACTION_BLOCKED_NOTIF_MUTE
            putExtra(BlockedNotificationChoiceReceiver.EXTRA_PACKAGE, packageName)
        }
        val keepIntent = Intent(this, BlockedNotificationChoiceReceiver::class.java).apply {
            action = BlockedNotificationChoiceReceiver.ACTION_BLOCKED_NOTIF_KEEP
            putExtra(BlockedNotificationChoiceReceiver.EXTRA_PACKAGE, packageName)
        }

        val mutePi = PendingIntent.getBroadcast(
            this,
            (packageName + "_mute").hashCode(),
            muteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val keepPi = PendingIntent.getBroadcast(
            this,
            (packageName + "_keep").hashCode(),
            keepIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nm = getSystemService(NotificationManager::class.java)

        // Check that the channel exists and notifications are enabled
        val channel = nm.getNotificationChannel(CHOICE_CHANNEL_ID)
        Log.i(TAG, "Prompting blocked notification choice for $appName ($packageName) reason=$reason")
        Log.i(TAG, "  Choice channel exists=${channel != null}, importance=${channel?.importance}")
        Log.i(TAG, "  areNotificationsEnabled=${nm.areNotificationsEnabled()}")

        val n = NotificationCompat.Builder(this, CHOICE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("$appName is now blocked")
            .setContentText("Mute notifications from $appName while blocked?")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$appName has been blocked ($reason). Do you want to mute its notifications until it is unblocked?"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .setOngoing(true)
            .addAction(0, "Mute notifications", mutePi)
            .addAction(0, "Keep notifications", keepPi)
            .build()

        nm.notify(100000 + packageName.hashCode(), n)
    }

    private fun ensureNotificationMuted(packageName: String) {
        if (packageName in currentlyMutedBySelfControl) return
        val ok = BlockedNotificationManager.applyNotificationMute(this, packageName, true)
        if (ok) {
            currentlyMutedBySelfControl.add(packageName)
            BlockedNotificationManager.markCurrentlyMutedBySelfControl(this, packageName, true)
        }
    }

    private fun ensureNotificationUnmuted(packageName: String) {
        if (packageName !in currentlyMutedBySelfControl) return
        val ok = BlockedNotificationManager.applyNotificationMute(this, packageName, false)
        if (ok) {
            currentlyMutedBySelfControl.remove(packageName)
            BlockedNotificationManager.markCurrentlyMutedBySelfControl(this, packageName, false)
        }
    }

    private fun onBlockedNotificationChoiceApplied(packageName: String, mute: Boolean) {
        // Dismiss the choice notification
        val nm = getSystemService(NotificationManager::class.java)
        nm.cancel(100000 + packageName.hashCode())

        val isCurfew = isCurrentlyPeriodBlocked(packageName)
        val isNuclear = nuclearState?.active == true && !NuclearManager.isExpired(nuclearState!!) && packageName in (nuclearState?.blockedPackages ?: emptyList())
        val isBlocked = packageName in suspendedApps || isCurfew || isNuclear

        if (!isBlocked) return
        if (mute) {
            ensureNotificationMuted(packageName)
            if (isCurfew) curfewMutedApps.add(packageName)
            if (isNuclear) nuclearMutedApps.add(packageName)
            updateNotification("$packageName blocked + notifications muted")
        } else {
            ensureNotificationUnmuted(packageName)
            curfewMutedApps.remove(packageName)
            nuclearMutedApps.remove(packageName)
            updateNotification("$packageName blocked (notifications kept)")
        }
    }
}
