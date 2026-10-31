package com.jo.custos.guardian

import android.app.Service
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

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "GuardianService created")
        GuardianEngine.init(this)
        GuardianEngine.enforceLockdown(this)
        loopHandler.post(loopRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
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
            val currentConfig = ConfigManager.loadConfig(applicationContext)
            val globalDelay = DelayManager.getCurrentEffectiveDelaySeconds(applicationContext)
            val defer = ConfigManager.requiredDefer(currentConfig, newConfig, globalDelay)
            val unlockApplies = !defer.fromExplicitTimer && DelayManager.isSettingsUnlocked(applicationContext)
            val mustDefer = defer.mustDefer && !unlockApplies
            return if (mustDefer) {
                DelayManager.requestConfigUpdate(applicationContext, candidateJson, defer.reason, defer.seconds)
                Log.w(TAG, "Config change deferred (${defer.seconds}s): ${defer.reason}")
                false
            } else {
                ConfigManager.saveConfig(applicationContext, newConfig)
                GuardianEngine.evaluateLimits(applicationContext)
                Log.i(TAG, "Config change applied immediately")
                true
            }
        }

        override fun getDelayStateJson(): String {
            val file = java.io.File(applicationContext.filesDir, "delay_state.json")
            return if (file.exists()) file.readText() else "{}"
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
            val dayStart = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
            val nowMs = System.currentTimeMillis()
            for (limit in config.limits) {
                val usedSec = UsageQuery.foregroundSeconds(applicationContext, limit.packageName, dayStart, nowMs)
                obj.put(limit.packageName, usedSec)
            }
            return obj.toString()
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
