package com.jo.custos.guardian

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log

class GuardianService : Service() {

    private val loopHandler = Handler(Looper.getMainLooper())
    private val loopRunnable = object : Runnable {
        override fun run() {
            try {
                GuardianEngine.evaluateLimits(applicationContext)
                DelayManager.applyPendingDelayIfReady(applicationContext)
                DelayManager.applyPendingConfigsIfReady(applicationContext)
                ScreenRuleManager.applyPendingRemovalsIfReady(applicationContext)
            } catch (e: Exception) {
                Log.e(TAG, "Error in evaluation loop: ${e.message}")
            }
            loopHandler.postDelayed(this, 1000L)
        }
    }

    private var packageReceiver: android.content.BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "GuardianService created")
        GuardianEngine.init(this)
        GuardianEngine.enforceLockdown(this)
        registerPackageReceiver()
        loopHandler.post(loopRunnable)
    }

    private fun registerPackageReceiver() {
        if (packageReceiver != null) return
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action ?: return
                if (action == Intent.ACTION_PACKAGE_ADDED || action == Intent.ACTION_PACKAGE_REPLACED) {
                    val pkg = intent.data?.schemeSpecificPart ?: return
                    if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false) && action == Intent.ACTION_PACKAGE_ADDED) {
                        return
                    }
                    Log.i(TAG, "Package detected dynamically in GuardianService: $pkg")
                    GuardianEngine.init(context)
                    if (GuardianEngine.isGuarded(context, pkg)) return
                    val state = GuardianEngine.getState(context)
                    if (state.enabled && pkg !in state.allowedPackages) {
                        GuardianEngine.setPackageSuspendedInternal(context, pkg, true)
                        GuardianEngine.setPackageHiddenInternal(context, pkg, true)
                        Log.w(TAG, "Guardian dynamically locked down newly installed app: $pkg")
                    }
                }
            }
        }
        val filter = android.content.IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED)
            addDataScheme("package")
        }
        try {
            registerReceiver(receiver, filter)
            packageReceiver = receiver
            Log.i(TAG, "Registered dynamic package receiver in GuardianService")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register dynamic package receiver in GuardianService: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        packageReceiver?.let {
            try { unregisterReceiver(it) } catch (_: Exception) {}
            packageReceiver = null
        }
        loopHandler.removeCallbacks(loopRunnable)
        Log.w(TAG, "GuardianService destroyed")
    }

    private val binder = object : IGuardianService.Stub() {

        override fun getVersion(): Int = 2

        override fun isDeviceOwnerActive(): Boolean = GuardianEngine.isDeviceOwner(applicationContext)

        override fun getQuarantineDelaySeconds(): Long {
            return GuardianEngine.getState(applicationContext).quarantineDelayHours * 3600L
        }

        override fun isWhitelistEnabled(): Boolean {
            return GuardianEngine.getState(applicationContext).enabled
        }

        override fun getAllowedPackages(): List<String> {
            return GuardianEngine.getState(applicationContext).allowedPackages.toList()
        }

        override fun getPendingPackages(): List<String> {
            return GuardianEngine.getState(applicationContext).pendingRequests.map { it.packageName }
        }

        override fun getHiddenPackages(): List<String> {
            val state = GuardianEngine.getState(applicationContext)
            val pm = packageManager
            val installed = pm.getInstalledPackages(0)
            return installed.map { it.packageName }.filter {
                it !in state.allowedPackages
            }
        }

        override fun requestAppAddition(packageName: String): Boolean {
            Log.i(TAG, "IPC requestAppAddition: $packageName")
            return GuardianEngine.requestAppAddition(applicationContext, packageName)
        }

        override fun cancelPendingRequest(packageName: String): Boolean {
            Log.i(TAG, "IPC cancelPendingRequest: $packageName")
            return GuardianEngine.cancelPendingRequest(applicationContext, packageName)
        }

        override fun removeAppFromWhitelist(packageName: String): Boolean {
            Log.w(TAG, "IPC removeAppFromWhitelist: $packageName")
            return GuardianEngine.removeAppFromWhitelist(applicationContext, packageName)
        }

        override fun requestChangeQuarantineDelay(hours: Int): Boolean {
            Log.i(TAG, "IPC requestChangeQuarantineDelay: ${hours}h")
            return GuardianEngine.requestChangeQuarantineDelay(applicationContext, hours)
        }

        override fun cancelPendingDelayChange(): Boolean {
            Log.i(TAG, "IPC cancelPendingDelayChange")
            return GuardianEngine.cancelPendingDelayChange(applicationContext)
        }

        override fun requestDisableWhitelist(): Boolean {
            Log.w(TAG, "IPC requestDisableWhitelist")
            return GuardianEngine.requestDisableWhitelist(applicationContext)
        }

        override fun cancelPendingDisableWhitelist(): Boolean {
            Log.i(TAG, "IPC cancelPendingDisableWhitelist")
            return GuardianEngine.cancelPendingDisableWhitelist(applicationContext)
        }

        override fun setWhitelistEnabled(enabled: Boolean): Boolean {
            Log.i(TAG, "IPC setWhitelistEnabled: $enabled")
            return GuardianEngine.setWhitelistEnabled(applicationContext, enabled)
        }

        override fun enforceLockdown() {
            Log.i(TAG, "IPC enforceLockdown")
            GuardianEngine.enforceLockdown(applicationContext)
        }

        override fun setPackageSuspended(packageName: String, suspended: Boolean) {
            GuardianEngine.setPackageSuspendedInternal(applicationContext, packageName, suspended)
        }

        override fun setPackageHidden(packageName: String, hidden: Boolean) {
            GuardianEngine.setPackageHiddenInternal(applicationContext, packageName, hidden)
        }

        override fun setPrivateDnsHost(hostname: String?): Boolean {
            Log.i(TAG, "IPC setPrivateDnsHost: $hostname")
            return GuardianEngine.setPrivateDnsHost(applicationContext, hostname)
        }

        override fun getPrivateDnsHost(): String? {
            return GuardianEngine.getPrivateDnsHost(applicationContext)
        }

        override fun getConfigJson(): String {
            val config = ConfigManager.loadConfig(applicationContext)
            return ConfigManager.configToJsonString(config)
        }

        override fun requestConfigChange(candidateJson: String): Boolean {
            val newConfig = ConfigManager.fromJsonString(candidateJson) ?: return false
            DelayManager.applyPendingConfigsIfReady(applicationContext)
            var currentConfig = ConfigManager.loadConfig(applicationContext)
            if (candidateJson == ConfigManager.configToJsonString(currentConfig)) {
                return true
            }

            // Immediately merge newly added curfew rules (hardenings)
            val currentCurfewSigs = currentConfig.periodBlocks.map { it.scheduleSignature() }.toSet()
            val addedCurfewRules = newConfig.periodBlocks.filter { it.scheduleSignature() !in currentCurfewSigs }
            if (addedCurfewRules.isNotEmpty()) {
                currentConfig = currentConfig.copy(periodBlocks = currentConfig.periodBlocks + addedCurfewRules)
                ConfigManager.saveConfig(applicationContext, currentConfig)
                GuardianEngine.evaluateLimits(applicationContext)
                Log.i(TAG, "Added ${addedCurfewRules.size} new curfew rule(s) immediately to Guardian")
            }

            // Immediately merge newly added app limits (hardenings)
            val currentLimitPkgs = currentConfig.limits.map { it.packageName }.toSet()
            val addedAppLimits = newConfig.limits.filter { it.packageName !in currentLimitPkgs }
            if (addedAppLimits.isNotEmpty()) {
                currentConfig = currentConfig.copy(limits = currentConfig.limits + addedAppLimits)
                ConfigManager.saveConfig(applicationContext, currentConfig)
                GuardianEngine.evaluateLimits(applicationContext)
                Log.i(TAG, "Added ${addedAppLimits.size} new app limit(s) immediately to Guardian")
            }

            if (ConfigManager.configToJsonString(currentConfig) == ConfigManager.configToJsonString(newConfig)) {
                return true
            }

            val globalDelay = DelayManager.getCurrentEffectiveDelaySeconds(applicationContext)
            val defer = ConfigManager.requiredDefer(currentConfig, newConfig, globalDelay)
            val unlockApplies = !defer.fromExplicitTimer && DelayManager.isSettingsUnlocked(applicationContext)
            val mustDefer = defer.mustDefer && !unlockApplies
            return if (mustDefer) {
                recordPendingConfigChange(applicationContext, currentConfig, newConfig, candidateJson, defer)
                Log.w(TAG, "Config change deferred (${defer.seconds}s): ${defer.reason}")
                false
            } else {
                ConfigManager.saveConfig(applicationContext, newConfig)
                GuardianEngine.evaluateLimits(applicationContext)
                Log.i(TAG, "Config change applied immediately")
                true
            }
        }

        private fun recordPendingConfigChange(
            context: android.content.Context,
            old: ConfigManager.Config,
            new: ConfigManager.Config,
            candidateJson: String,
            defer: ConfigManager.DeferRequirement
        ) {
            val oldLimits = old.limits.associateBy { it.packageName }
            val newLimits = new.limits.associateBy { it.packageName }
            val allPkgs = (oldLimits.keys + newLimits.keys).toSet()

            val limitChanges = mutableListOf<Triple<String, ConfigManager.AppLimit?, Boolean>>()
            for (pkg in allPkgs) {
                val o = oldLimits[pkg]
                val n = newLimits[pkg]
                if (o != null && n == null) {
                    limitChanges.add(Triple(pkg, null, true))
                } else if (n != null && n != o) {
                    limitChanges.add(Triple(pkg, n, false))
                }
            }

            val curfewChanges = mutableListOf<Pair<ConfigManager.PeriodBlockRule, Boolean>>()
            val oldCurfewSigs = old.periodBlocks.associateBy { it.scheduleSignature() }
            val newCurfewSigs = new.periodBlocks.associateBy { it.scheduleSignature() }
            for ((sig, r) in oldCurfewSigs) {
                if (sig !in newCurfewSigs) curfewChanges.add(r to true)
            }
            for ((sig, r) in newCurfewSigs) {
                if (sig !in oldCurfewSigs || r != oldCurfewSigs[sig]) curfewChanges.add(r to false)
            }

            val installChanges = mutableListOf<Pair<ConfigManager.InstallBlockGroup, Boolean>>()
            val oldInstallNames = old.installBlocks.associateBy { it.name }
            val newInstallNames = new.installBlocks.associateBy { it.name }
            for ((name, g) in oldInstallNames) {
                if (name !in newInstallNames) installChanges.add(g to true)
            }
            for ((name, g) in newInstallNames) {
                if (name !in oldInstallNames || g != oldInstallNames[name]) installChanges.add(g to false)
            }

            var singleHandled = false
            if (limitChanges.size == 1 && curfewChanges.isEmpty() && installChanges.isEmpty()) {
                val (pkg, newLimit, isDelete) = limitChanges.first()
                if (isDelete) {
                    DelayManager.requestAppLimitDelete(context, pkg, defer.reason, defer.seconds)
                } else if (newLimit != null) {
                    DelayManager.requestAppLimitUpdate(context, pkg, newLimit, defer.reason, defer.seconds)
                }
                singleHandled = true
            } else if (limitChanges.isEmpty() && curfewChanges.size == 1 && installChanges.isEmpty()) {
                val (rule, isDelete) = curfewChanges.first()
                if (isDelete) {
                    DelayManager.requestCurfewDelete(context, rule.scheduleSignature(), defer.reason, defer.seconds)
                } else {
                    DelayManager.requestCurfewUpdate(context, rule.scheduleSignature(), rule, defer.reason, defer.seconds)
                }
                singleHandled = true
            } else if (limitChanges.isEmpty() && curfewChanges.isEmpty() && installChanges.size == 1) {
                val (group, isDelete) = installChanges.first()
                if (isDelete) {
                    DelayManager.requestInstallBlockDelete(context, group.name, defer.reason, defer.seconds)
                } else {
                    DelayManager.requestInstallBlockUpdate(context, group.name, group, defer.reason, defer.seconds)
                }
                singleHandled = true
            }

            if (!singleHandled) {
                DelayManager.requestConfigUpdate(context, candidateJson, defer.reason, defer.seconds)
            }
        }

        override fun getDelayStateJson(): String {
            val file = java.io.File(applicationContext.filesDir, "delay_config.json")
            return if (file.exists()) file.readText() else "{}"
        }

        override fun saveDelayStateJson(json: String): Boolean {
            val file = java.io.File(applicationContext.filesDir, "delay_config.json")
            file.writeText(json)
            DelayManager.loadState(applicationContext)
            return true
        }

        override fun cancelPendingConfigChange(): Boolean {
            DelayManager.cancelConfigUpdates(applicationContext)
            return true
        }

        override fun getScreenRulesJson(): String {
            val file = java.io.File(applicationContext.filesDir, "screen_rules.json")
            return if (file.exists()) file.readText() else "{}"
        }

        override fun saveScreenRulesJson(json: String): Boolean {
            val file = java.io.File(applicationContext.filesDir, "screen_rules.json")
            file.writeText(json)
            return true
        }

        override fun getBlockedApps(): List<String> {
            return GuardianEngine.getBlockedApps().toList()
        }

        override fun getUsageStatsJson(): String {
            val config = ConfigManager.loadConfig(applicationContext)
            val obj = org.json.JSONObject()
            val dayStart = GuardianEngine.dayStartMillis()
            val nowMs = System.currentTimeMillis()
            for (limit in config.limits) {
                val usedSec = UsageQuery.foregroundSeconds(applicationContext, limit.packageName, dayStart, nowMs)
                obj.put(limit.packageName, usedSec)
            }
            return obj.toString()
        }

        override fun getWhitelistStateJson(): String {
            return GuardianEngine.getWhitelistStateJson(applicationContext)
        }

        override fun saveWhitelistStateJson(json: String): Boolean {
            return GuardianEngine.importWhitelistState(applicationContext, json)
        }

        override fun setNotificationMutePreference(packageName: String, mute: Boolean): Boolean {
            BlockedNotificationManager.setMuteWhenBlockedPreference(applicationContext, packageName, mute)
            if (packageName in GuardianEngine.getBlockedApps()) {
                BlockedNotificationManager.applyNotificationMute(applicationContext, packageName, mute)
            }
            return true
        }

        override fun getNotificationMutePreference(packageName: String): Boolean {
            return BlockedNotificationManager.getMuteWhenBlockedPreference(applicationContext, packageName) ?: false
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.i(TAG, "Client bound to GuardianService: ${intent?.action}")
        return binder
    }

    companion object {
        private const val TAG = "Custos.Guardian.Service"
    }
}
