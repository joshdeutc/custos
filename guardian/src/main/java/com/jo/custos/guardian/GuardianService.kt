package com.jo.custos.guardian

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.os.Binder
import android.os.Process
import android.os.Parcel

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
    private var a11yObserver: android.database.ContentObserver? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "GuardianService created")
        GuardianEngine.init(this)
        GuardianEngine.enforceLockdown(this)
        GuardianEngine.scheduleHeartbeat(this)
        registerPackageReceiver()
        registerA11yObserver()
        loopHandler.removeCallbacks(loopRunnable)
        loopHandler.post(loopRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Keep GuardianService alive as a sticky started service independent of Custos UI binding (F1/F3).
        return START_STICKY
    }

    private fun registerA11yObserver() {
        if (a11yObserver != null) return
        val observer = object : android.database.ContentObserver(loopHandler) {
            override fun onChange(selfChange: Boolean) {
                // Instant self-heal (<10ms) if an attacker attempts `settings put secure enabled_accessibility_services ...` via adb (F3).
                GuardianEngine.ensureAccessibilityServicesEnabled(applicationContext)
            }
        }
        try {
            contentResolver.registerContentObserver(
                android.provider.Settings.Secure.getUriFor(android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
                false,
                observer
            )
            contentResolver.registerContentObserver(
                android.provider.Settings.Secure.getUriFor(android.provider.Settings.Secure.ACCESSIBILITY_ENABLED),
                false,
                observer
            )
            a11yObserver = observer
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register accessibility ContentObserver: ${e.message}")
        }
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
                    GuardianEngine.onPackageAdded(context, pkg)
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
        a11yObserver?.let {
            try { contentResolver.unregisterContentObserver(it) } catch (_: Exception) {}
            a11yObserver = null
        }
        packageReceiver?.let {
            try { unregisterReceiver(it) } catch (_: Exception) {}
            packageReceiver = null
        }
        loopHandler.removeCallbacks(loopRunnable)
        runCatching { GuardianEngine.ensureAccessibilityServicesEnabled(applicationContext) }
        runCatching { GuardianEngine.scheduleHeartbeat(applicationContext) }
        Log.w(TAG, "GuardianService destroyed")
    }

    private fun enforceCallingPermission() {
        val uid = Binder.getCallingUid()
        if (uid == Process.myUid()) return
        val packages = packageManager.getPackagesForUid(uid)
        if (packages == null || CLIENT_UI_PACKAGE !in packages) {
            Log.e(TAG, "Unauthorized AIDL call from UID $uid (${packages?.joinToString()})")
            throw SecurityException("Access denied: UID $uid is not authorized to communicate with GuardianService")
        }
        // F6 (defense in depth): once the signing-certificate allowlist is configured, also verify
        // the caller's certificate so a same-named impostor cannot reach Guardian. While the
        // allowlist is empty, the package-name check above remains the sole gate (behaviour unchanged).
        if (AUTHORIZED_UI_CERT_SHA256.isNotEmpty()) {
            val callerCert = signingCertSha256(CLIENT_UI_PACKAGE)
            if (callerCert == null || callerCert !in AUTHORIZED_UI_CERT_SHA256) {
                Log.e(TAG, "Rejected AIDL call: $CLIENT_UI_PACKAGE cert $callerCert not in allowlist")
                throw SecurityException("Access denied: caller signing certificate is not trusted")
            }
        }
    }

    private fun signingCertSha256(pkg: String): String? {
        return try {
            val pm = packageManager
            val signers: Array<android.content.pm.Signature>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = pm.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES)
                info.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES).signatures
            }
            val cert = signers?.firstOrNull() ?: return null
            java.security.MessageDigest.getInstance("SHA-256")
                .digest(cert.toByteArray())
                .joinToString(":") { "%02X".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "signingCertSha256 failed for $pkg: ${e.message}")
            null
        }
    }

    private val binder = object : IGuardianService.Stub() {

        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            enforceCallingPermission()
            return super.onTransact(code, data, reply, flags)
        }

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
            val state = GuardianEngine.getState(applicationContext)
            if (!suspended && state.enabled && packageName !in state.allowedPackages) {
                Log.w(TAG, "Rejected setPackageSuspended(false) for unauthorized app: $packageName")
                return
            }
            if (!suspended && packageName in GuardianEngine.getBlockedApps()) {
                Log.w(TAG, "Rejected setPackageSuspended(false) for limit-blocked app: $packageName")
                return
            }
            GuardianEngine.setPackageSuspendedInternal(applicationContext, packageName, suspended)
        }

        override fun setPackageHidden(packageName: String, hidden: Boolean) {
            val state = GuardianEngine.getState(applicationContext)
            if (!hidden && state.enabled && packageName !in state.allowedPackages) {
                Log.w(TAG, "Rejected setPackageHidden(false) for unauthorized app: $packageName")
                return
            }
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
            DelayManager.applyPendingDelayIfReady(applicationContext)
            DelayManager.applyPendingConfigsIfReady(applicationContext)
            val delayFile = java.io.File(applicationContext.filesDir, "delay_config.json")
            return if (delayFile.exists()) delayFile.readText() else "{}"
        }

        override fun saveDelayStateJson(json: String): Boolean {
            return DelayManager.saveValidatedDelayStateJson(applicationContext, json)
        }

        override fun cancelPendingConfigChange(): Boolean {
            DelayManager.cancelConfigUpdates(applicationContext)
            return true
        }

        override fun getScreenRulesJson(): String {
            ScreenRuleManager.applyPendingRemovalsIfReady(applicationContext, force = true)
            val file = java.io.File(applicationContext.filesDir, "screen_rules.json")
            return if (file.exists()) file.readText() else "{}"
        }

        override fun saveScreenRulesJson(json: String): Boolean {
            return ScreenRuleManager.saveValidatedScreenRulesJson(applicationContext, json)
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
            if (GuardianEngine.isWhitelistInitialized(applicationContext)) {
                Log.w(TAG, "Rejected saveWhitelistStateJson: Guardian whitelist is already initialized and protected!")
                return false
            }
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

        override fun getSessionStateJson(): String {
            return SessionManager.getSessionStateJson(applicationContext)
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.i(TAG, "Client bound to GuardianService: ${intent?.action}")
        return binder
    }

    companion object {
        private const val TAG = "Custos.Guardian.Service"
        private const val CLIENT_UI_PACKAGE = "com.jo.selfcontrol.ultimate"

        // F6 — TODO(custos): renseigner les 2 empreintes SHA-256 du certificat de signature de
        // com.jo.selfcontrol.ultimate (Release officielle + Debug developpeur), au format
        // "AA:BB:CC:...:FF". Tant que cette liste est VIDE, l'authentification AIDL reste par
        // nom de package seulement (comportement inchange). Une fois remplie, le certificat de
        // l'appelant doit en faire partie, ce qui bloque un impostor homonyme.
        private val AUTHORIZED_UI_CERT_SHA256 = emptySet<String>()
    }
}
