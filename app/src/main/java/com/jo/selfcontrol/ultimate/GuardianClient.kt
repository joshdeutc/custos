package com.jo.selfcontrol.ultimate

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.jo.custos.guardian.IGuardianService

object GuardianClient {

    private const val TAG = "Custos.GuardianClient"
    private const val GUARDIAN_PACKAGE = "com.jo.custos.guardian"
    private const val GUARDIAN_ACTION = "com.jo.custos.guardian.ACTION_BIND_GUARDIAN"

    @Volatile
    private var service: IGuardianService? = null
    @Volatile
    private var isBound = false
    private var appContext: Context? = null
    var onConnectedCallback: (() -> Unit)? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = IGuardianService.Stub.asInterface(binder)
            isBound = true
            Log.i(TAG, "Connected to GuardianService IPC (version=${service?.version})")
            appContext?.let { syncInitialConfigIfNeeded(it) }
            onConnectedCallback?.invoke()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            isBound = false
            Log.w(TAG, "Disconnected from GuardianService IPC")
        }
    }

    private fun syncInitialConfigIfNeeded(ctx: Context) {
        try {
            val remoteJson = service?.configJson
            val isRemoteEmpty = remoteJson.isNullOrBlank() || remoteJson == "{}" ||
                (org.json.JSONObject(remoteJson).optJSONArray("limits")?.length() ?: 0) == 0

            if (isRemoteEmpty) {
                val localFile = java.io.File(ctx.filesDir, "limits.json")
                if (localFile.exists()) {
                    val localText = localFile.readText()
                    val localLimits = org.json.JSONObject(localText).optJSONArray("limits")?.length() ?: 0
                    if (localLimits > 0) {
                        Log.i(TAG, "Migrating $localLimits local app limits to Guardian")
                        service?.requestConfigChange(localText)
                    }
                }
            }

            val remoteRules = service?.screenRulesJson
            val isRemoteRulesEmpty = remoteRules.isNullOrBlank() || remoteRules == "{}" ||
                (org.json.JSONObject(remoteRules).optJSONArray("rules")?.length() ?: 0) == 0
            if (isRemoteRulesEmpty) {
                val localRulesFile = java.io.File(ctx.filesDir, "screen_rules.json")
                if (localRulesFile.exists()) {
                    val localRulesText = localRulesFile.readText()
                    val count = org.json.JSONObject(localRulesText).optJSONArray("rules")?.length() ?: 0
                    if (count > 0) {
                        Log.i(TAG, "Migrating $count local screen rules to Guardian")
                        service?.saveScreenRulesJson(localRulesText)
                    }
                }
            }

            // Sync whitelist state and delay
            val localWhitelistFile = java.io.File(ctx.filesDir, "whitelist_state.json")
            if (localWhitelistFile.exists()) {
                val localWhitelistText = localWhitelistFile.readText()
                val json = org.json.JSONObject(localWhitelistText)
                val hours = json.optInt("quarantine_delay_hours", 72)
                if (hours > 0) {
                    service?.requestChangeQuarantineDelay(hours)
                }
            } else {
                service?.requestChangeQuarantineDelay(72)
            }

            val remoteWhitelist = service?.whitelistStateJson
            val remoteAllowedCount = if (!remoteWhitelist.isNullOrBlank() && remoteWhitelist != "{}") {
                try {
                    val arr = org.json.JSONObject(remoteWhitelist).optJSONArray("allowed_packages")
                    arr?.length() ?: 0
                } catch (e: Exception) { 0 }
            } else 0
            val isRemoteWhitelistEmpty = remoteAllowedCount <= 21

            if (isRemoteWhitelistEmpty) {
                val initialPkgs = WhitelistManager.getInitialAllowedPackages(ctx)
                val initialJson = org.json.JSONObject().apply {
                    put("enabled", true)
                    put("quarantine_delay_hours", 72)
                    put("allowed_packages", org.json.JSONArray(initialPkgs.toList()))
                }
                service?.saveWhitelistStateJson(initialJson.toString())
                try {
                    localWhitelistFile.writeText(initialJson.toString(2))
                } catch (e: Exception) {
                    Log.e(TAG, "Failed writing local whitelist", e)
                }
            } else {
                try {
                    localWhitelistFile.writeText(remoteWhitelist!!)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed caching remote whitelist to local file", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during syncInitialConfigIfNeeded", e)
        }
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        if (isBound && service != null) {
            syncInitialConfigIfNeeded(context.applicationContext)
            return
        }
        try {
            val intent = Intent(GUARDIAN_ACTION).apply {
                component = ComponentName(GUARDIAN_PACKAGE, "com.jo.custos.guardian.GuardianService")
            }
            val ok = context.applicationContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            Log.i(TAG, "Binding to GuardianService: bindResult=$ok")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind to GuardianService", e)
        }
    }

    val isAvailable: Boolean
        get() = service != null

    fun isGuardianDeviceOwner(): Boolean {
        return try {
            service?.isDeviceOwnerActive ?: false
        } catch (e: Exception) {
            Log.e(TAG, "IPC error isDeviceOwnerActive", e)
            false
        }
    }

    fun getQuarantineDelaySeconds(): Long {
        val s = service ?: return 72 * 3600L
        return try {
            s.quarantineDelaySeconds
        } catch (e: Exception) {
            Log.e(TAG, "IPC error getQuarantineDelaySeconds", e)
            72 * 3600L
        }
    }

    fun isWhitelistEnabled(): Boolean {
        val s = service ?: return false
        return try {
            s.isWhitelistEnabled
        } catch (e: Exception) {
            Log.e(TAG, "IPC error isWhitelistEnabled", e)
            false
        }
    }

    fun getAllowedPackages(): List<String> {
        val s = service ?: return emptyList()
        return try {
            s.allowedPackages
        } catch (e: Exception) {
            Log.e(TAG, "IPC error getAllowedPackages", e)
            emptyList()
        }
    }

    fun getPendingPackages(): List<String> {
        val s = service ?: return emptyList()
        return try {
            s.pendingPackages
        } catch (e: Exception) {
            Log.e(TAG, "IPC error getPendingPackages", e)
            emptyList()
        }
    }

    fun getHiddenPackages(): List<String> {
        val s = service ?: return emptyList()
        return try {
            s.hiddenPackages
        } catch (e: Exception) {
            Log.e(TAG, "IPC error getHiddenPackages", e)
            emptyList()
        }
    }

    fun requestAppAddition(pkg: String): Boolean {
        val s = service ?: return false
        return try {
            s.requestAppAddition(pkg)
        } catch (e: Exception) {
            Log.e(TAG, "IPC error requestAppAddition $pkg", e)
            false
        }
    }

    fun cancelPendingRequest(pkg: String): Boolean {
        val s = service ?: return false
        return try {
            s.cancelPendingRequest(pkg)
        } catch (e: Exception) {
            Log.e(TAG, "IPC error cancelPendingRequest $pkg", e)
            false
        }
    }

    fun removeAppFromWhitelist(pkg: String): Boolean {
        val s = service ?: return false
        return try {
            s.removeAppFromWhitelist(pkg)
        } catch (e: Exception) {
            Log.e(TAG, "IPC error removeAppFromWhitelist $pkg", e)
            false
        }
    }

    fun requestChangeQuarantineDelay(hours: Int): Boolean {
        val s = service ?: return false
        return try {
            s.requestChangeQuarantineDelay(hours)
        } catch (e: Exception) {
            Log.e(TAG, "IPC error requestChangeQuarantineDelay $hours", e)
            false
        }
    }

    fun cancelPendingDelayChange(): Boolean {
        val s = service ?: return false
        return try {
            s.cancelPendingDelayChange()
        } catch (e: Exception) {
            Log.e(TAG, "IPC error cancelPendingDelayChange", e)
            false
        }
    }

    fun requestDisableWhitelist(): Boolean {
        val s = service ?: return false
        return try {
            s.requestDisableWhitelist()
        } catch (e: Exception) {
            Log.e(TAG, "IPC error requestDisableWhitelist", e)
            false
        }
    }

    fun cancelPendingDisableWhitelist(): Boolean {
        val s = service ?: return false
        return try {
            s.cancelPendingDisableWhitelist()
        } catch (e: Exception) {
            Log.e(TAG, "IPC error cancelPendingDisableWhitelist", e)
            false
        }
    }

    fun setWhitelistEnabled(enabled: Boolean): Boolean {
        val s = service ?: return false
        return try {
            s.setWhitelistEnabled(enabled)
        } catch (e: Exception) {
            Log.e(TAG, "IPC error setWhitelistEnabled $enabled", e)
            false
        }
    }

    fun enforceLockdown() {
        val s = service ?: return
        try {
            s.enforceLockdown()
        } catch (e: Exception) {
            Log.e(TAG, "IPC error enforceLockdown", e)
        }
    }

    fun setPackageSuspended(pkg: String, suspended: Boolean) {
        val s = service ?: return
        try {
            s.setPackageSuspended(pkg, suspended)
        } catch (e: Exception) {
            Log.e(TAG, "IPC error setPackageSuspended $pkg", e)
        }
    }

    fun setPackageHidden(pkg: String, hidden: Boolean) {
        val s = service ?: return
        try {
            s.setPackageHidden(pkg, hidden)
        } catch (e: Exception) {
            Log.e(TAG, "IPC error setPackageHidden $pkg", e)
        }
    }

    fun setPrivateDnsHost(hostname: String?): Boolean {
        val s = service ?: return false
        return try {
            s.setPrivateDnsHost(hostname)
        } catch (e: Exception) {
            Log.e(TAG, "IPC error setPrivateDnsHost $hostname", e)
            false
        }
    }

    fun getPrivateDnsHost(): String? {
        val s = service ?: return null
        return try {
            s.privateDnsHost
        } catch (e: Exception) {
            Log.e(TAG, "IPC error getPrivateDnsHost", e)
            null
        }
    }

    fun getConfigJson(): String? {
        val s = service ?: return null
        return try {
            s.configJson
        } catch (e: Exception) {
            Log.e(TAG, "IPC error getConfigJson", e)
            null
        }
    }

    fun requestConfigChange(candidateJson: String): Boolean {
        val s = service ?: return false
        return try {
            s.requestConfigChange(candidateJson)
        } catch (e: Exception) {
            Log.e(TAG, "IPC error requestConfigChange", e)
            false
        }
    }

    fun getDelayStateJson(): String? {
        val s = service ?: return null
        return try {
            s.delayStateJson
        } catch (e: Exception) {
            Log.e(TAG, "IPC error getDelayStateJson", e)
            null
        }
    }

    fun cancelPendingConfigChange(): Boolean {
        val s = service ?: return false
        return try {
            s.cancelPendingConfigChange()
        } catch (e: Exception) {
            Log.e(TAG, "IPC error cancelPendingConfigChange", e)
            false
        }
    }

    fun getScreenRulesJson(): String? {
        val s = service ?: return null
        return try {
            s.screenRulesJson
        } catch (e: Exception) {
            Log.e(TAG, "IPC error getScreenRulesJson", e)
            null
        }
    }

    fun saveScreenRulesJson(json: String): Boolean {
        val s = service ?: return false
        return try {
            s.saveScreenRulesJson(json)
        } catch (e: Exception) {
            Log.e(TAG, "IPC error saveScreenRulesJson", e)
            false
        }
    }

    fun getBlockedApps(): List<String> {
        val s = service ?: return emptyList()
        return try {
            s.blockedApps
        } catch (e: Exception) {
            Log.e(TAG, "IPC error getBlockedApps", e)
            emptyList()
        }
    }

    fun getUsageStatsJson(): String? {
        val s = service ?: return null
        return try {
            s.usageStatsJson
        } catch (e: Exception) {
            Log.e(TAG, "IPC error getUsageStatsJson", e)
            null
        }
    }

    fun getWhitelistStateJson(): String? {
        val s = service ?: return null
        return try {
            s.whitelistStateJson
        } catch (e: Exception) {
            Log.e(TAG, "IPC error getWhitelistStateJson", e)
            null
        }
    }

    fun saveWhitelistStateJson(json: String): Boolean {
        val s = service ?: return false
        return try {
            s.saveWhitelistStateJson(json)
        } catch (e: Exception) {
            Log.e(TAG, "IPC error saveWhitelistStateJson", e)
            false
        }
    }

    fun setNotificationMutePreference(packageName: String, mute: Boolean): Boolean {
        val s = service ?: return false
        return try {
            s.setNotificationMutePreference(packageName, mute)
        } catch (e: Exception) {
            Log.e(TAG, "IPC error setNotificationMutePreference", e)
            false
        }
    }

    fun getNotificationMutePreference(packageName: String): Boolean {
        val s = service ?: return false
        return try {
            s.getNotificationMutePreference(packageName)
        } catch (e: Exception) {
            Log.e(TAG, "IPC error getNotificationMutePreference", e)
            false
        }
    }
}
