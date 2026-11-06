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
    private const val DEFAULT_DELAY_HOURS = 0
    const val DEFAULT_PRIVATE_DNS = "b91912.dns.nextdns.io"
    const val ACTION_CHECK_EXPIRATION = "com.jo.custos.guardian.ACTION_CHECK_EXPIRATION"

    val INITIAL_ALLOWED_PACKAGES = setOf(
        "com.jo.lycee",
        "com.waze",
        "fr.gouv.franceidentite",
        "com.shazam.android",
        "com.app.tgtg",
        "com.azure.authenticator",
        "com.memrise.android.memrisecompanion",
        "ch.protonmail.android",
        "com.google.android.apps.docs.editors.docs",
        "com.enki.Enki750g",
        "com.tribab.tricount.android",
        "com.jo.animesama",
        "com.jo.bdsmsutra",
        "com.jo.entutc",
        "com.jo.fetlife",
        "com.jo.novethic",
        "com.jo.tapology",
        "com.jo.notubeplayer",
        "com.jo.linktrash",
        "air.com.unit9.bkFrApp",
        "com.jo.selfcontrol.ultimate",
        "com.guardian",
        "com.vsct.vsc.mobile.horaireetresa.android",
        "cc.dreamspark.intervaltimer",
        "com.lexilize.fc",
        "com.jo.discordpersonal",
        "com.microsoft.skydrive",
        "com.sportigoaccess",
        "com.ttxapps.autosync",
        "fr.epiconcept.pdfnoteskiosk",
        "com.jo.community",
        "com.google.android.apps.translate",
        "fr.iledefrance.labaz",
        "com.passbolt.mobile.android",
        "com.eurotalk.uTalk",
        "fr.bouyguestelecom.ecm.android",
        "com.voxsquare.voxpay.android",
        "coop.up.beneficiaire",
        "co.feeld",
        "tv.arte.plus7",
        "com.whatsapp",
        "com.intsig.camscanner",
        "fr.doctolib.www",
        "com.google.android.contactkeys",
        "com.bambuna.podcastaddict",
        "in.krosbits.musicolet",
        "mobi.societegenerale.mobile.lappli",
        "fr.icdc.sl6.app",
        "com.boursorama.android.clients",
        "com.shotguntheapp.android",
        "com.completude.professeur",
        "com.chyrpe.chyrpe",
        "com.google.android.keep",
        "com.english.progress.learn",
        "com.google.android.safetycore",
        "com.ttxapps.drivesync",
        "com.google.android.apps.authenticator2",
        "com.chess",
        "com.deepl.mobiletranslator",
        "com.letterboxd.letterboxd",
        "gymworkout.gym.gymlog.gymtrainer",
        "livio.pack.lang.fr_FR",
        "oracle.idm.mobile.authenticator",
        "com.google.android.apps.youtube.music",
        "com.google.android.apps.tachyon",
        "com.google.android.apps.docs",
        "com.google.android.apps.magazines",
        "com.google.android.apps.podcasts",
        "com.google.android.videos",
        "com.tailscale.ipn",
        "com.discord",
        "com.snapchat.android",
        "deezer.android.app",
        "fr.leboncoin",
        "fr.vinted",
        "com.facebook.katana",
        "com.duolingo",
        "com.amazon.mShop.android.shopping",
        "com.booking",
        "com.linkedin.android",
        "com.audible.application",
        "com.agoda.mobile.consumer",
        "cn.wps.xiaomi.abroad.lite",
        "com.xiaomi.midrop",
        "com.xiaomi.scanner",
        "com.xiaomi.smarthome",
        "com.miui.calculator",
        "com.miui.weather2",
        "com.miui.notes",
        "com.miui.screenrecorder",
        "com.miui.mediaeditor",
        "com.miui.compass",
        "com.miui.android.fashiongallery",
        "com.duokan.phone.remotecontroller",
        "com.amazon.appmanager",
        "com.amazon.avod.thirdpartyclient",
        "com.google.ar.core",
        "com.android.soundrecorder",
        "com.opera.preinstall",
        "com.mi.global.shop",
        "com.mi.global.bbs",
        "com.android.vending",
        "com.google.android.packageinstaller",
        "com.android.packageinstaller",
        "com.samsung.android.packageinstaller",
        "com.google.android.dialer",
        "com.google.android.apps.messaging",
        "com.android.mms",
        "com.google.android.inputmethod.latin",
        "com.google.android.deskclock",
        "com.google.android.calculator"
    )

    fun getInitialAllowedPackages(ctx: Context): Set<String> {
        val result = mutableSetOf<String>()
        try {
            val pm = ctx.packageManager
            val installed = pm.getInstalledPackages(PackageManager.MATCH_UNINSTALLED_PACKAGES)
            for (info in installed) {
                if (info.packageName == ctx.packageName) continue
                if (isGuarded(ctx, info.packageName)) continue
                val ai = info.applicationInfo
                val isSys = if (ai != null) {
                    ((ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0) || ((ai.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0)
                } else false
                if (!isSys) {
                    result.add(info.packageName)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting initial installed packages: ${e.message}")
        }
        return result
    }

    private val CRITICAL_PACKAGES = setOf(
        "android",
        "com.android.systemui",
        "com.android.settings",
        "com.samsung.android.settings",
        "com.google.android.gms",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.android.phone",
        "com.android.server.telecom",
        "com.android.providers.telephony",
        "com.tailscale.ipn",
        "com.jo.custos.guardian",
        "com.jo.selfcontrol.ultimate" // Client UI
    )

    private val HARD_GUARDS = CRITICAL_PACKAGES

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

    fun isWhitelistInitialized(ctx: Context): Boolean {
        synchronized(lock) {
            val state = getState(ctx)
            return state.allowedPackages.any { it !in HARD_GUARDS }
        }
    }

    fun isGuarded(ctx: Context, pkg: String): Boolean {
        if (pkg in HARD_GUARDS) return true
        if (pkg == ctx.packageName) return true
        // SAFETY: never hide/suspend a system app. Hiding a core system package
        // (e.g. com.google.android.permissioncontroller) makes PackageManagerService
        // crash at boot -> unrecoverable bootloop (incident 2026-11-01).
        if (isSystemPackage(ctx, pkg)) return true
        if (pkg in launcherPackages(ctx) || pkg in inputMethodPackages(ctx)) return true
        return false
    }

    fun isPackageInstalled(ctx: Context, pkg: String): Boolean {
        return try {
            ctx.packageManager.getApplicationInfo(pkg, PackageManager.MATCH_UNINSTALLED_PACKAGES)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun isSystemPackage(ctx: Context, pkg: String): Boolean {
        return try {
            val ai = ctx.packageManager.getApplicationInfo(pkg, PackageManager.MATCH_UNINSTALLED_PACKAGES)
            (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                (ai.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        } catch (e: Exception) {
            false
        }
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
                allowedPackages = HARD_GUARDS + getInitialAllowedPackages(ctx)
            )
            saveStateInternal(ctx, initial)
            return initial
        }
        return try {
            val json = JSONObject(file.readText())
            val enabled = json.optBoolean("enabled", true)
            var delayHours = json.optInt("quarantine_delay_hours", DEFAULT_DELAY_HOURS)
            val allowedArr = json.optJSONArray("allowed_packages") ?: JSONArray()
            val allowed = mutableSetOf<String>()
            for (i in 0 until allowedArr.length()) {
                allowed.add(allowedArr.getString(i))
            }
            allowed.addAll(HARD_GUARDS)
            val nonGuards = allowed.filterNot { it in HARD_GUARDS }
            if (nonGuards.isEmpty()) {
                allowed.addAll(getInitialAllowedPackages(ctx))
            }

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

            var pendingDelayHours = if (json.has("pending_delay_hours") && !json.isNull("pending_delay_hours")) {
                json.getInt("pending_delay_hours")
            } else null
            var pendingDelayExec = json.optLong("pending_delay_execute_at", 0L)
            val pendingDisableExec = json.optLong("pending_disable_execute_at", 0L)

            if (delayHours >= 100) {
                Log.w(TAG, "Guardian auto-reset excessive quarantine delay ($delayHours h -> 0 h)")
                delayHours = 0
                pendingDelayHours = null
                pendingDelayExec = 0L
            }
            val dnsHost = if (json.has("private_dns_host")) {
                if (json.isNull("private_dns_host")) null else json.getString("private_dns_host")
            } else DEFAULT_PRIVATE_DNS

            val state = GuardianState(
                enabled = enabled,
                quarantineDelayHours = delayHours,
                allowedPackages = allowed,
                pendingRequests = reqs,
                pendingDelayHours = pendingDelayHours,
                pendingDelayExecuteAt = pendingDelayExec,
                pendingDisableExecuteAt = pendingDisableExec,
                privateDnsHost = dnsHost
            )
            if (nonGuards.isEmpty() || delayHours != json.optInt("quarantine_delay_hours", 0) || pendingDelayExec != json.optLong("pending_delay_execute_at", 0L)) {
                saveStateInternal(ctx, state)
            }
            state
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

    fun getWhitelistStateJson(ctx: Context): String {
        checkAndPromotePendingRequests(ctx)
        val file = File(ctx.filesDir, STATE_FILE)
        return if (file.exists()) file.readText() else "{}"
    }

    fun importWhitelistState(ctx: Context, jsonStr: String): Boolean {
        return synchronized(lock) {
            try {
                val json = JSONObject(jsonStr)
                val enabled = json.optBoolean("enabled", true)
                var delayHours = json.optInt("quarantine_delay_hours", DEFAULT_DELAY_HOURS)
                val allowedArr = json.optJSONArray("allowed_packages") ?: JSONArray()
                val allowed = mutableSetOf<String>()
                for (i in 0 until allowedArr.length()) {
                    allowed.add(allowedArr.getString(i))
                }
                allowed.addAll(HARD_GUARDS)
                if (allowed.none { it !in HARD_GUARDS }) {
                    allowed.addAll(getInitialAllowedPackages(ctx))
                }

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

                val newState = GuardianState(
                    enabled = enabled,
                    quarantineDelayHours = delayHours,
                    allowedPackages = allowed,
                    pendingRequests = reqs,
                    pendingDelayHours = pendingDelayHours,
                    pendingDelayExecuteAt = pendingDelayExec,
                    pendingDisableExecuteAt = pendingDisableExec
                )
                saveStateInternal(ctx, newState)
                enforceLockdown(ctx)
                Log.i(TAG, "Imported whitelist state: ${allowed.size} apps, delay=${delayHours}h")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed importing whitelist state: ${e.message}", e)
                false
            }
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

            if (state.quarantineDelayHours == 0) {
                val updatedAllowed = state.allowedPackages + cleanPkg
                saveStateInternal(ctx, state.copy(allowedPackages = updatedAllowed))
                setPackageSuspendedInternal(ctx, cleanPkg, false)
                setPackageHiddenInternal(ctx, cleanPkg, false)
                Log.i(TAG, "Quarantine delay is 0: app $cleanPkg allowed immediately")
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

    fun onPackageRemoved(ctx: Context, packageName: String) {
        val isStillInstalledOnDisk = try {
            val ai = ctx.packageManager.getApplicationInfo(packageName, PackageManager.MATCH_UNINSTALLED_PACKAGES)
            !ai.sourceDir.isNullOrBlank() && File(ai.sourceDir).exists()
        } catch (e: Exception) {
            false
        }
        if (isStillInstalledOnDisk) return
        if (isGuarded(ctx, packageName) || packageName == ctx.packageName) return
        synchronized(lock) {
            val state = getState(ctx)
            val cleanPkg = packageName.trim()
            val wasAllowed = cleanPkg in state.allowedPackages
            val wasPending = state.pendingRequests.any { it.packageName == cleanPkg }
            if (!wasAllowed && !wasPending) return

            val updatedAllowed = state.allowedPackages - cleanPkg
            val updatedPending = state.pendingRequests.filterNot { it.packageName == cleanPkg }
            saveStateInternal(ctx, state.copy(allowedPackages = updatedAllowed, pendingRequests = updatedPending))
            Log.w(TAG, "Guardian removed uninstalled package $cleanPkg from whitelist")
        }
    }

    fun requestChangeQuarantineDelay(ctx: Context, newHours: Int): Boolean {
        if (newHours < 0) return false
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
        var state = getState(ctx)
        // Always enforce configured Private DNS (DNS-over-TLS) and lockdown
        applyPrivateDnsInternal(ctx, state.privateDnsHost)

        // Enforce anti-uninstall on Guardian and Client UI when Guardian is Device Owner
        if (isDeviceOwner(ctx)) {
            val dpm = ctx.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            val admin = getAdminComponent(ctx)
            if (dpm != null) {
                runCatching {
                    dpm.setUninstallBlocked(admin, ctx.packageName, true)
                    dpm.setUninstallBlocked(admin, "com.jo.selfcontrol.ultimate", true)
                    dpm.clearUserRestriction(admin, UserManager.DISALLOW_FACTORY_RESET)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        runCatching { dpm.addUserRestriction(admin, UserManager.DISALLOW_CONFIG_DATE_TIME) }
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        runCatching { dpm.setAutoTimeEnabled(admin, true) }
                        runCatching { dpm.setAutoTimeZoneEnabled(admin, true) }
                    }
                }.onFailure { Log.e(TAG, "Failed to apply DO uninstall protection: ${it.message}") }
            }
        }

        if (!state.enabled) return

        val pm = ctx.packageManager
        val installed = try {
            pm.getInstalledPackages(PackageManager.MATCH_UNINSTALLED_PACKAGES)
        } catch (e: Exception) {
            emptyList()
        }

        val installedSet = installed.map { it.packageName }.toSet()
        val ghostAllowed = state.allowedPackages.filter {
            it !in installedSet && !isGuarded(ctx, it) && it != ctx.packageName && it !in HARD_GUARDS
        }.toSet()
        if (ghostAllowed.isNotEmpty()) {
            val prunedAllowed = state.allowedPackages - ghostAllowed
            state = state.copy(allowedPackages = prunedAllowed)
            saveStateInternal(ctx, state)
            Log.w(TAG, "Guardian pruned ${ghostAllowed.size} ghost app(s) from whitelist: $ghostAllowed")
        }

        if (state.quarantineDelayHours <= 0) {
            val newlyAllowed = installed.map { it.packageName }.filterNot { isGuarded(ctx, it) }.filter { pkg ->
                pkg in state.allowedPackages || pkg in INITIAL_ALLOWED_PACKAGES || isInstalledFromPlayStore(ctx, pkg)
            }
            val merged = state.allowedPackages + newlyAllowed
            if (merged != state.allowedPackages) {
                state = state.copy(allowedPackages = merged)
                saveStateInternal(ctx, state)
            }
            for (pkgInfo in installed) {
                val pkg = pkgInfo.packageName
                if (isGuarded(ctx, pkg) || pkg in state.allowedPackages) {
                    setPackageSuspendedInternal(ctx, pkg, false)
                    setPackageHiddenInternal(ctx, pkg, false)
                } else {
                    setPackageSuspendedInternal(ctx, pkg, true)
                    setPackageHiddenInternal(ctx, pkg, true)
                    uninstallPackageSilently(ctx, pkg)
                }
            }
            return
        }

        for (pkgInfo in installed) {
            val pkg = pkgInfo.packageName
            if (isGuarded(ctx, pkg) || pkg in state.allowedPackages) {
                // Allowed app -> ensure unhidden and unsuspended
                setPackageSuspendedInternal(ctx, pkg, false)
                setPackageHiddenInternal(ctx, pkg, false)
            } else {
                // Unauthorized app -> hide and suspend (and uninstall if from PC/ADB)
                setPackageSuspendedInternal(ctx, pkg, true)
                setPackageHiddenInternal(ctx, pkg, true)
                if (!isInstalledFromPlayStore(ctx, pkg)) {
                    uninstallPackageSilently(ctx, pkg)
                }
            }
        }
    }

    fun isInstalledFromPlayStore(ctx: Context, pkg: String): Boolean {
        return try {
            val pm = ctx.packageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val info = pm.getInstallSourceInfo(pkg)
                info.installingPackageName == "com.android.vending" ||
                    info.initiatingPackageName == "com.android.vending"
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(pkg) == "com.android.vending"
            }
        } catch (e: Exception) {
            false
        }
    }

    fun uninstallPackageSilently(ctx: Context, pkg: String): Boolean {
        if (isGuarded(ctx, pkg)) return false
        if (!isDeviceOwner(ctx)) return false
        return try {
            val packageInstaller = ctx.packageManager.packageInstaller
            val intent = Intent(ctx, GuardianPackageReceiver::class.java).apply {
                action = "com.jo.custos.guardian.ACTION_UNINSTALL_RESULT"
                putExtra("pkg", pkg)
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pi = PendingIntent.getBroadcast(ctx, pkg.hashCode(), intent, flags)
            packageInstaller.uninstall(pkg, pi.intentSender)
            Log.w(TAG, "🚨 Guardian triggered silent uninstallation of unauthorized PC/ADB app: $pkg")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed silent uninstall of $pkg: ${e.message}", e)
            false
        }
    }

    fun onPackageAdded(ctx: Context, pkg: String) {
        init(ctx)
        if (isGuarded(ctx, pkg)) {
            Log.i(TAG, "Package $pkg is guarded or system app. Skipping quarantine.")
            return
        }
        val state = getState(ctx)
        if (!state.enabled || pkg in state.allowedPackages) return

        if (!isInstalledFromPlayStore(ctx, pkg)) {
            setPackageSuspendedInternal(ctx, pkg, true)
            setPackageHiddenInternal(ctx, pkg, true)
            uninstallPackageSilently(ctx, pkg)
            Log.w(TAG, "🚨 Unauthorized PC/ADB install detected ($pkg) -> Hidden, Suspended & Uninstalled!")
            return
        }

        if (state.quarantineDelayHours <= 0) {
            requestAppAddition(ctx, pkg)
            Log.i(TAG, "Quarantine delay is 0: Guardian auto-allowed Play Store app: $pkg")
        } else {
            setPackageSuspendedInternal(ctx, pkg, true)
            setPackageHiddenInternal(ctx, pkg, true)
            Log.w(TAG, "Guardian locked down newly installed unauthorized Play Store app: $pkg")
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
                if (dpm.isPackageSuspended(admin, packageName) != suspended) {
                    dpm.setPackagesSuspended(admin, arrayOf(packageName), suspended)
                }
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
                if (dpm.isApplicationHidden(admin, packageName) != hidden) {
                    dpm.setApplicationHidden(admin, packageName, hidden)
                }
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
        val oldPolicy = android.os.StrictMode.getThreadPolicy()
        return try {
            android.os.StrictMode.setThreadPolicy(android.os.StrictMode.ThreadPolicy.Builder().permitAll().build())
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
        } catch (e: Exception) {
            Log.e(TAG, "applyPrivateDnsInternal failed: ${e.message}", e)
            false
        } finally {
            android.os.StrictMode.setThreadPolicy(oldPolicy)
        }
    }

    private val activeBlockedApps = mutableSetOf<String>()

    fun getBlockedApps(): Set<String> = synchronized(lock) {
        activeBlockedApps.toSet()
    }

    fun notifyConfigChanged(ctx: Context) {
        evaluateLimits(ctx)
    }

    fun dayStartMillis(): Long {
        val cal = java.util.Calendar.getInstance()
        if (cal.get(java.util.Calendar.HOUR_OF_DAY) < 2) {
            cal.add(java.util.Calendar.DATE, -1)
        }
        cal.apply {
            set(java.util.Calendar.HOUR_OF_DAY, 2)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private var lastA11yHealMs = 0L

    fun ensureAccessibilityServicesEnabled(ctx: Context) {
        if (!isDeviceOwner(ctx)) return
        val dpm = ctx.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager ?: return
        val admin = getAdminComponent(ctx)
        val guardianSvc = "com.jo.custos.guardian/com.jo.custos.guardian.GuardianWatcherService"
        val guardianShort = "com.jo.custos.guardian/.GuardianWatcherService"
        val uiSvc = "com.jo.selfcontrol.ultimate/com.jo.selfcontrol.ultimate.AppWatcherService"
        val uiShort = "com.jo.selfcontrol.ultimate/.AppWatcherService"

        runCatching {
            val current = android.provider.Settings.Secure.getString(
                ctx.contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""
            val entries = current.split(':').filter { it.isNotBlank() }.toMutableList()
            var changed = false
            if (entries.none { it == guardianSvc || it == guardianShort }) {
                entries.add(guardianSvc)
                changed = true
            }
            if (isPackageInstalled(ctx, "com.jo.selfcontrol.ultimate") &&
                entries.none { it == uiSvc || it == uiShort }
            ) {
                entries.add(uiSvc)
                changed = true
            }
            if (changed) {
                val merged = entries.joinToString(":")
                dpm.setSecureSetting(admin, android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, merged)
                dpm.setSecureSetting(admin, android.provider.Settings.Secure.ACCESSIBILITY_ENABLED, "1")
                Log.w(TAG, "Auto-healed AccessibilityServices via Device Owner: $merged")
            }
        }
    }

    fun evaluateLimits(ctx: Context) {
        synchronized(lock) {
            val nowMs = System.currentTimeMillis()
            if (nowMs - lastA11yHealMs >= 5_000L) {
                lastA11yHealMs = nowMs
                ensureAccessibilityServicesEnabled(ctx)
            }

            val config = ConfigManager.loadConfig(ctx)
            val now = java.util.Calendar.getInstance()
            val dayOfWeek = now.get(java.util.Calendar.DAY_OF_WEEK) - 1 // 0=Dimanche, 1=Lundi, ..., 6=Samedi
            val currentMinutes = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE)

            // Logical quota day resets at 02:00 AM (aligned with LimitService and DailyResetReceiver)
            val calQuota = java.util.Calendar.getInstance().apply {
                add(java.util.Calendar.HOUR_OF_DAY, -2)
            }
            val quotaDayOfWeek = calQuota.get(java.util.Calendar.DAY_OF_WEEK) - 1
            val logicalDayId = calQuota.get(java.util.Calendar.YEAR) * 1000 + calQuota.get(java.util.Calendar.DAY_OF_YEAR)
            SessionManager.resetDayIfNeeded(ctx, logicalDayId)

            val currentApp = GuardianWatcherService.currentForegroundApp
            val limitsByPkg = config.limits.associateBy { it.packageName }

            // End active sessions ONLY when the user has returned to the launcher / home screen
            if (currentApp.isNotBlank() && currentApp in launcherPackages(ctx)) {
                val activeSessions = SessionManager.activePackages(ctx)
                for (pkg in activeSessions) {
                    val sessionCfg = limitsByPkg[pkg]?.session ?: continue
                    SessionManager.endSessionIfActive(ctx, pkg, sessionCfg, nowMs)
                }
                GuardianWatcherService.updateSessionOverlay(null)
            }

            if (currentApp.isNotBlank() && currentApp != "unknown") {
                GuardianWatcherService.recheckScreenRules(currentApp)
            }

            val newlyBlocked = mutableSetOf<String>()

            // 1. Period Blocks (Couvre-feux) - uses wall-clock time and ConfigManager.isInBlockedWindow
            for (period in config.periodBlocks) {
                if (dayOfWeek in period.allowedDays) {
                    if (ConfigManager.isInBlockedWindow(currentMinutes, period.blockedStartMinutes, period.blockedEndMinutes)) {
                        newlyBlocked.addAll(period.packages)
                    }
                }
            }

            var activeOverlayText: String? = null

            // 2. Daily Quota & Session Limits
            for (limit in config.limits) {
                val pkg = limit.packageName
                if (pkg in newlyBlocked) continue

                // Check allowed days (logical day, 2:00 AM reset)
                if (quotaDayOfWeek !in limit.allowedDays) {
                    newlyBlocked.add(pkg)
                    continue
                }

                // Check allowed hours
                if (!limit.allDay) {
                    if (!ConfigManager.isInAllowedWindow(currentMinutes, limit.allowedHoursStart, limit.allowedHoursEnd)) {
                        newlyBlocked.add(pkg)
                        continue
                    }
                }

                // Check daily usage quota (calculated from 02:00 AM logical day start)
                val maxSec = if (limit.maxSecondsPerDay > 0) limit.maxSecondsPerDay else limit.maxMinutesPerDay * 60
                if (maxSec == 0) {
                    // Quota 0 min/sec = completely blocked 24/7
                    newlyBlocked.add(pkg)
                    continue
                } else if (maxSec > 0) {
                    val dayStart = dayStartMillis()
                    val usedSec = UsageQuery.foregroundSeconds(ctx, pkg, dayStart, nowMs)
                    if (usedSec >= maxSec) {
                        newlyBlocked.add(pkg)
                        continue
                    }
                }

                // 3. Session Limit (Discord-style per-unlock session & cooldown)
                val session = limit.session
                if (session != null) {
                    if (pkg == currentApp) {
                        when (SessionManager.evaluate(ctx, pkg, session, nowMs)) {
                            SessionManager.Result.BLOCKED_COOLDOWN,
                            SessionManager.Result.BLOCKED_DAILY -> {
                                newlyBlocked.add(pkg)
                            }
                            SessionManager.Result.IN_SESSION,
                            SessionManager.Result.STARTED_SESSION -> {
                                val st = SessionManager.statusOf(ctx, pkg, session, nowMs)
                                if (st.state == SessionManager.Status.State.ACTIVE) {
                                    val sm = st.sessionRemainingSec / 60
                                    val ss = st.sessionRemainingSec % 60
                                    activeOverlayText = "⏱ ${sm}m${"%02d".format(ss)}s · ${st.sessionsUsed}/${st.maxSessionsPerDay}"
                                }
                            }
                        }
                    } else if (SessionManager.shouldStayBlocked(ctx, pkg, session, nowMs)) {
                        newlyBlocked.add(pkg)
                    }
                }
            }

            GuardianWatcherService.updateSessionOverlay(activeOverlayText)

            // Apply newly blocked / unblocked transitions
            val toBlock = newlyBlocked - activeBlockedApps
            val toUnblock = activeBlockedApps - newlyBlocked

            for (pkg in toBlock) {
                activeBlockedApps.add(pkg)
                setPackageSuspendedInternal(ctx, pkg, true)
                val mute = BlockedNotificationManager.getMuteWhenBlockedPreference(ctx, pkg) ?: false
                if (mute) {
                    BlockedNotificationManager.applyNotificationMute(ctx, pkg, true)
                }
                if (pkg == currentApp) {
                    GuardianWatcherService.forceHomeIfBlocked()
                }
                Log.w(TAG, "Quota/Session/Curfew reached -> Blocked & Suspended: $pkg")
            }

            for (pkg in toUnblock) {
                activeBlockedApps.remove(pkg)
                setPackageSuspendedInternal(ctx, pkg, false)
                BlockedNotificationManager.applyNotificationMute(ctx, pkg, false)
                Log.i(TAG, "Quota/Session/Curfew lifted -> Unblocked & Unsuspended: $pkg")
            }
        }
    }
}
