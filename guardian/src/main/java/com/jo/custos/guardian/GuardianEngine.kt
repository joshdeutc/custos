package com.jo.custos.guardian

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.UserManager
import android.util.Log
import android.view.inputmethod.InputMethodManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object GuardianEngine {

    private const val TAG = "Custos.Guardian.Engine"
    private const val STATE_FILE = "guardian_state.json"
    private const val DEFAULT_DELAY_HOURS = 24
    const val DEFAULT_PRIVATE_DNS = "b91912.dns.nextdns.io"
    const val ACTION_CHECK_EXPIRATION = "com.jo.custos.guardian.ACTION_CHECK_EXPIRATION"

    private val HARD_GUARDS = setOf(
        "android",
        "com.android.systemui",
        "com.android.settings",
        "com.google.android.gms",
        "com.android.permissioncontroller",
        "com.android.vending",
        "com.google.android.packageinstaller",
        "com.android.packageinstaller",
        "com.samsung.android.packageinstaller",
        "com.android.phone",
        "com.android.server.telecom",
        "com.android.providers.telephony",
        "com.google.android.dialer",
        "com.google.android.apps.messaging",
        "com.android.mms",
        "com.google.android.inputmethod.latin",
        "com.google.android.deskclock",
        "com.google.android.calculator",
        "com.tailscale.ipn",
        "com.jo.custos.guardian",
        "com.jo.selfcontrol.ultimate" // Client UI
    )

    data class PendingRequest(
        val packageName: String,
        val requestedAt: Long,
        val availableAt: Long
    )

    data class GuardianState(
        val enabled: Boolean = true,
        val quarantineDelayHours: Int = DEFAULT_DELAY_HOURS,
        val allowedPackages: Set<String> = emptySet(),
        val pendingRequests: List<PendingRequest> = emptyList(),
        val pendingDelayHours: Int? = null,
        val pendingDelayExecuteAt: Long = 0L,
        val pendingDisableExecuteAt: Long = 0L,
        val privateDnsHost: String? = DEFAULT_PRIVATE_DNS
    )

    private var currentState: GuardianState? = null
    private val lock = Any()

    fun getAdminComponent(ctx: Context): ComponentName {
        return ComponentName(ctx, GuardianAdminReceiver::class.java)
    }

    fun isDeviceOwner(ctx: Context): Boolean {
        val dpm = ctx.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager ?: return false
        return dpm.isDeviceOwnerApp(ctx.packageName)
    }

    fun init(ctx: Context) {
        synchronized(lock) {
            if (currentState == null) {
                currentState = loadState(ctx)
            }
        }
    }

    fun getState(ctx: Context): GuardianState {
        synchronized(lock) {
            if (currentState == null) {
                currentState = loadState(ctx)
            }
            return currentState!!
        }
    }

    fun isGuarded(ctx: Context, pkg: String): Boolean {
        if (pkg in HARD_GUARDS) return true
        if (pkg == ctx.packageName) return true
        val pm = ctx.packageManager
        val isSys = try {
            val appInfo = pm.getApplicationInfo(pkg, PackageManager.MATCH_UNINSTALLED_PACKAGES)
            ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) ||
            ((appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0)
        } catch (e: Exception) {
            false
        }
        if (isSys) return true
        if (pkg in launcherPackages(ctx) || pkg in inputMethodPackages(ctx)) return true
        return false
    }

    private fun launcherPackages(ctx: Context): Set<String> {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            ctx.packageManager.queryIntentActivities(intent, 0)
                .map { it.activityInfo.packageName }
                .toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    private fun inputMethodPackages(ctx: Context): Set<String> {
        return try {
            val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.enabledInputMethodList?.map { it.packageName }?.toSet() ?: emptySet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    private fun loadState(ctx: Context): GuardianState {
        val file = File(ctx.filesDir, STATE_FILE)
        if (!file.exists()) {
            val initial = GuardianState(
                enabled = true,
                quarantineDelayHours = DEFAULT_DELAY_HOURS,
                allowedPackages = HARD_GUARDS
            )
            saveStateInternal(ctx, initial)
            return initial
        }
        return try {
            val json = JSONObject(file.readText())
            val enabled = json.optBoolean("enabled", true)
            val delayHours = json.optInt("quarantine_delay_hours", DEFAULT_DELAY_HOURS)
            val allowedArr = json.optJSONArray("allowed_packages") ?: JSONArray()
            val allowed = mutableSetOf<String>()
            for (i in 0 until allowedArr.length()) {
                allowed.add(allowedArr.getString(i))
            }
            allowed.addAll(HARD_GUARDS)

            val reqsArr = json.optJSONArray("pending_requests") ?: JSONArray()
            val reqs = mutableListOf<PendingRequest>()
            for (i in 0 until reqsArr.length()) {
                val obj = reqsArr.getJSONObject(i)
                reqs.add(
                    PendingRequest(
                        packageName = obj.getString("package"),
                        requestedAt = obj.getLong("requested_at"),
                        availableAt = obj.getLong("available_at")
                    )
                )
            }

            val pendingDelayHours = if (json.has("pending_delay_hours") && !json.isNull("pending_delay_hours")) {
                json.getInt("pending_delay_hours")
            } else null
            val pendingDelayExec = json.optLong("pending_delay_execute_at", 0L)
            val pendingDisableExec = json.optLong("pending_disable_execute_at", 0L)
            val dnsHost = if (json.has("private_dns_host")) {
                if (json.isNull("private_dns_host")) null else json.getString("private_dns_host")
            } else DEFAULT_PRIVATE_DNS

            GuardianState(
                enabled = enabled,
                quarantineDelayHours = delayHours,
                allowedPackages = allowed,
                pendingRequests = reqs,
                pendingDelayHours = pendingDelayHours,
                pendingDelayExecuteAt = pendingDelayExec,
                pendingDisableExecuteAt = pendingDisableExec,
                privateDnsHost = dnsHost
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error loading state, restoring defaults", e)
            GuardianState(enabled = true, quarantineDelayHours = DEFAULT_DELAY_HOURS, allowedPackages = HARD_GUARDS, privateDnsHost = DEFAULT_PRIVATE_DNS)
        }
    }

    private fun saveStateInternal(ctx: Context, state: GuardianState) {
        currentState = state
        try {
            val json = JSONObject().apply {
                put("enabled", state.enabled)
                put("quarantine_delay_hours", state.quarantineDelayHours)
                put("allowed_packages", JSONArray(state.allowedPackages.toList()))
                put("private_dns_host", state.privateDnsHost ?: JSONObject.NULL)
                put("pending_requests", JSONArray().apply {
                    for (req in state.pendingRequests) {
                        put(JSONObject().apply {
                            put("package", req.packageName)
                            put("requested_at", req.requestedAt)
                            put("available_at", req.availableAt)
                        })
                    }
                })
                put("pending_delay_hours", state.pendingDelayHours ?: JSONObject.NULL)
                put("pending_delay_execute_at", state.pendingDelayExecuteAt)
                put("pending_disable_execute_at", state.pendingDisableExecuteAt)
            }
            File(ctx.filesDir, STATE_FILE).writeText(json.toString(2))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist state", e)
        }
    }

    fun checkAndPromotePendingRequests(ctx: Context) {
        synchronized(lock) {
            val state = getState(ctx)
            val now = System.currentTimeMillis()
            var stateChanged = false

            // 1. Promote pending apps
            val ready = state.pendingRequests.filter { it.availableAt <= now }
            val stillPending = state.pendingRequests.filter { it.availableAt > now }

            val updatedAllowed = state.allowedPackages.toMutableSet()
            if (ready.isNotEmpty()) {
                for (r in ready) {
                    updatedAllowed.add(r.packageName)
                    setPackageSuspendedInternal(ctx, r.packageName, false)
                    setPackageHiddenInternal(ctx, r.packageName, false)
                    Log.i(TAG, "Quarantine elapsed: released ${r.packageName}")
                }
                stateChanged = true
            }

            // 2. Pending delay change
            var activeDelay = state.quarantineDelayHours
            var pDelayHours = state.pendingDelayHours
            var pDelayExec = state.pendingDelayExecuteAt
            if (pDelayExec in 1..now && pDelayHours != null) {
                activeDelay = pDelayHours
                pDelayHours = null
                pDelayExec = 0L
                stateChanged = true
                Log.i(TAG, "Applied new quarantine delay: ${activeDelay}h")
            }

            // 3. Pending whitelist disable
            var activeEnabled = state.enabled
            var pDisableExec = state.pendingDisableExecuteAt
            if (pDisableExec in 1..now) {
                activeEnabled = false
                pDisableExec = 0L
                stateChanged = true
                Log.w(TAG, "Whitelist deactivation executed after delay -> releasing all apps")
                releaseAll(ctx)
            }

            if (stateChanged) {
                val newState = state.copy(
                    enabled = activeEnabled,
                    quarantineDelayHours = activeDelay,
                    allowedPackages = updatedAllowed,
                    pendingRequests = stillPending,
                    pendingDelayHours = pDelayHours,
                    pendingDelayExecuteAt = pDelayExec,
                    pendingDisableExecuteAt = pDisableExec
                )
                saveStateInternal(ctx, newState)
            }

            scheduleNextUnlockAlarm(ctx)
        }
    }

    fun requestAppAddition(ctx: Context, packageName: String): Boolean {
        synchronized(lock) {
            checkAndPromotePendingRequests(ctx)
            val state = getState(ctx)
            val cleanPkg = packageName.trim()

            if (isGuarded(ctx, cleanPkg) || cleanPkg in state.allowedPackages) {
                setPackageSuspendedInternal(ctx, cleanPkg, false)
                setPackageHiddenInternal(ctx, cleanPkg, false)
                return true
            }

            // Check if already pending
            if (state.pendingRequests.any { it.packageName == cleanPkg }) {
                return true
            }

            val now = System.currentTimeMillis()
            val delaySec = state.quarantineDelayHours * 3600L
            val availableAt = now + (delaySec * 1000L)

            val updatedRequests = state.pendingRequests + PendingRequest(cleanPkg, now, availableAt)
            saveStateInternal(ctx, state.copy(pendingRequests = updatedRequests))

            // Enforce quarantine: hide and suspend
            setPackageSuspendedInternal(ctx, cleanPkg, true)
            setPackageHiddenInternal(ctx, cleanPkg, true)

            Log.i(TAG, "App $cleanPkg queued for quarantine ($delaySec seconds)")
            scheduleNextUnlockAlarm(ctx)
            return true
        }
    }

    fun cancelPendingRequest(ctx: Context, packageName: String): Boolean {
        synchronized(lock) {
            val state = getState(ctx)
            val cleanPkg = packageName.trim()
            if (state.pendingRequests.none { it.packageName == cleanPkg }) return false
            val updated = state.pendingRequests.filter { it.packageName != cleanPkg }
            saveStateInternal(ctx, state.copy(pendingRequests = updated))
            Log.i(TAG, "Canceled pending quarantine request for $cleanPkg")
            scheduleNextUnlockAlarm(ctx)
            return true
        }
    }

    fun removeAppFromWhitelist(ctx: Context, packageName: String): Boolean {
        synchronized(lock) {
            val state = getState(ctx)
            val cleanPkg = packageName.trim()
            if (isGuarded(ctx, cleanPkg)) return false
            if (cleanPkg !in state.allowedPackages) return false

            val updated = state.allowedPackages - cleanPkg
            saveStateInternal(ctx, state.copy(allowedPackages = updated))
            setPackageSuspendedInternal(ctx, cleanPkg, true)
            setPackageHiddenInternal(ctx, cleanPkg, true)
            Log.w(TAG, "Removed $cleanPkg from whitelist -> locked down immediately")
            return true
        }
    }

    fun requestChangeQuarantineDelay(ctx: Context, newHours: Int): Boolean {
        if (newHours <= 0) return false
        synchronized(lock) {
            checkAndPromotePendingRequests(ctx)
            val state = getState(ctx)
            val current = state.quarantineDelayHours

            if (newHours >= current) {
                // Tightening / Increasing protection -> Instantaneous!
                saveStateInternal(ctx, state.copy(
                    quarantineDelayHours = newHours,
                    pendingDelayHours = null,
                    pendingDelayExecuteAt = 0L
                ))
                Log.i(TAG, "Quarantine delay increased immediately to ${newHours}h")
                scheduleNextUnlockAlarm(ctx)
                return true
            } else {
                // Relaxing / Decreasing protection -> Constitutional delay mandatory!
                val executeAt = System.currentTimeMillis() + (current * 3600_000L)
                saveStateInternal(ctx, state.copy(
                    pendingDelayHours = newHours,
                    pendingDelayExecuteAt = executeAt
                ))
                Log.w(TAG, "Delay reduction to ${newHours}h scheduled in ${current}h")
                scheduleNextUnlockAlarm(ctx)
                return true
            }
        }
    }

    fun cancelPendingDelayChange(ctx: Context): Boolean {
        synchronized(lock) {
            val state = getState(ctx)
            if (state.pendingDelayExecuteAt <= 0L) return false
            saveStateInternal(ctx, state.copy(
                pendingDelayHours = null,
                pendingDelayExecuteAt = 0L
            ))
            Log.i(TAG, "Canceled pending delay change")
            scheduleNextUnlockAlarm(ctx)
            return true
        }
    }

    fun requestDisableWhitelist(ctx: Context): Boolean {
        synchronized(lock) {
            checkAndPromotePendingRequests(ctx)
            val state = getState(ctx)
            if (!state.enabled) return true

            val current = state.quarantineDelayHours
            val executeAt = System.currentTimeMillis() + (current * 3600_000L)
            saveStateInternal(ctx, state.copy(pendingDisableExecuteAt = executeAt))
            Log.w(TAG, "Whitelist deactivation scheduled in ${current}h")
            scheduleNextUnlockAlarm(ctx)
            return true
        }
    }

    fun cancelPendingDisableWhitelist(ctx: Context): Boolean {
        synchronized(lock) {
            val state = getState(ctx)
            if (state.pendingDisableExecuteAt <= 0L) return false
            saveStateInternal(ctx, state.copy(pendingDisableExecuteAt = 0L))
            Log.i(TAG, "Canceled pending whitelist deactivation")
            scheduleNextUnlockAlarm(ctx)
            return true
        }
    }

    fun setWhitelistEnabled(ctx: Context, enabled: Boolean): Boolean {
        synchronized(lock) {
            checkAndPromotePendingRequests(ctx)
            val state = getState(ctx)
            if (enabled) {
                // Hardening: Immediate! Clear pending disable and enforce lockdown
                saveStateInternal(ctx, state.copy(
                    enabled = true,
                    pendingDisableExecuteAt = 0L
                ))
                enforceLockdown(ctx)
                Log.i(TAG, "Whitelist enabled immediately")
                return true
            } else {
                // Relaxation: MUST undergo constitutional delay!
                return requestDisableWhitelist(ctx)
            }
        }
    }

    fun enforceLockdown(ctx: Context) {
        checkAndPromotePendingRequests(ctx)
        val state = getState(ctx)
        // Always enforce configured Private DNS (DNS-over-TLS) and lockdown
        applyPrivateDnsInternal(ctx, state.privateDnsHost)

        if (!state.enabled) return

        val pm = ctx.packageManager
        val installed = try {
            pm.getInstalledPackages(PackageManager.MATCH_UNINSTALLED_PACKAGES)
        } catch (e: Exception) {
            emptyList()
        }

        for (pkgInfo in installed) {
            val pkg = pkgInfo.packageName
            if (isGuarded(ctx, pkg) || pkg in state.allowedPackages) {
                // Allowed app -> ensure unhidden and unsuspended
                setPackageSuspendedInternal(ctx, pkg, false)
                setPackageHiddenInternal(ctx, pkg, false)
            } else {
                // Unauthorized app -> hide and suspend
                setPackageSuspendedInternal(ctx, pkg, true)
                setPackageHiddenInternal(ctx, pkg, true)
            }
        }
    }

    fun releaseAll(ctx: Context) {
        val pm = ctx.packageManager
        val installed = try {
            pm.getInstalledPackages(PackageManager.MATCH_UNINSTALLED_PACKAGES)
        } catch (e: Exception) {
            emptyList()
        }
        for (pkgInfo in installed) {
            setPackageSuspendedInternal(ctx, pkgInfo.packageName, false)
            setPackageHiddenInternal(ctx, pkgInfo.packageName, false)
        }
    }

    fun scheduleNextUnlockAlarm(ctx: Context) {
        val state = getState(ctx)
        val now = System.currentTimeMillis()
        val candidateTimes = mutableListOf<Long>()

        for (r in state.pendingRequests) {
            if (r.availableAt > now) candidateTimes.add(r.availableAt)
        }
        if (state.pendingDelayExecuteAt > now) {
            candidateTimes.add(state.pendingDelayExecuteAt)
        }
        if (state.pendingDisableExecuteAt > now) {
            candidateTimes.add(state.pendingDisableExecuteAt)
        }

        if (candidateTimes.isEmpty()) return
        val targetTime = candidateTimes.minOrNull() ?: return

        try {
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(ACTION_CHECK_EXPIRATION).apply {
                setPackage(ctx.packageName)
            }
            val pi = PendingIntent.getBroadcast(
                ctx,
                9991,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetTime, pi)
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, targetTime, pi)
            }
            Log.i(TAG, "Scheduled Guardian expiration alarm for ${java.util.Date(targetTime)}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule Guardian alarm", e)
        }
    }

    fun setPackageSuspendedInternal(ctx: Context, packageName: String, suspended: Boolean) {
        if (isGuarded(ctx, packageName) && suspended) return
        val dpm = ctx.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager ?: return
        val admin = getAdminComponent(ctx)
        try {
            if (dpm.isDeviceOwnerApp(ctx.packageName)) {
                dpm.setPackagesSuspended(admin, arrayOf(packageName), suspended)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set package suspended: $packageName", e)
        }
    }

    fun setPackageHiddenInternal(ctx: Context, packageName: String, hidden: Boolean) {
        if (isGuarded(ctx, packageName) && hidden) return
        val dpm = ctx.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager ?: return
        val admin = getAdminComponent(ctx)
        try {
            if (dpm.isDeviceOwnerApp(ctx.packageName)) {
                dpm.setApplicationHidden(admin, packageName, hidden)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set package hidden: $packageName", e)
        }
    }

    fun setPrivateDnsHost(ctx: Context, host: String?): Boolean {
        synchronized(lock) {
            val cleanHost = host?.trim()?.takeIf { it.isNotEmpty() }
            val state = getState(ctx)
            saveStateInternal(ctx, state.copy(privateDnsHost = cleanHost))
            return applyPrivateDnsInternal(ctx, cleanHost)
        }
    }

    fun getPrivateDnsHost(ctx: Context): String? {
        return getState(ctx).privateDnsHost
    }

    fun applyPrivateDnsInternal(ctx: Context, host: String?): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val dpm = ctx.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager ?: return false
        val admin = getAdminComponent(ctx)
        if (!dpm.isDeviceOwnerApp(ctx.packageName)) {
            Log.w(TAG, "Cannot apply private DNS: Guardian is not Device Owner")
            return false
        }
        return runCatching {
            if (!host.isNullOrBlank()) {
                val res = dpm.setGlobalPrivateDnsModeSpecifiedHost(admin, host)
                dpm.addUserRestriction(admin, UserManager.DISALLOW_CONFIG_PRIVATE_DNS)
                Log.i(TAG, "Guardian enforced Global Private DNS: $host (res=$res)")
                res == DevicePolicyManager.PRIVATE_DNS_SET_NO_ERROR
            } else {
                dpm.setGlobalPrivateDnsModeOpportunistic(admin)
                dpm.clearUserRestriction(admin, UserManager.DISALLOW_CONFIG_PRIVATE_DNS)
                Log.i(TAG, "Guardian cleared Global Private DNS (opportunistic mode)")
                true
            }
        }.getOrElse {
            Log.e(TAG, "applyPrivateDnsInternal failed: ${it.message}", it)
            false
        }
    }
}
