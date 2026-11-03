package com.jo.custos.guardian

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class GuardianPackageReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        when (action) {
            Intent.ACTION_PACKAGE_ADDED, Intent.ACTION_PACKAGE_REPLACED -> {
                val pkg = intent.data?.schemeSpecificPart ?: return
                if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false) && action == Intent.ACTION_PACKAGE_ADDED) {
                    return
                }
                Log.i(TAG, "Package detected on device: $pkg")
                GuardianEngine.init(context)
                if (GuardianEngine.isGuarded(context, pkg)) {
                    Log.i(TAG, "Package $pkg is guarded or system app. Skipping quarantine.")
                    return
                }
                val state = GuardianEngine.getState(context)
                if (state.enabled && pkg !in state.allowedPackages) {
                    if (state.quarantineDelayHours <= 0) {
                        GuardianEngine.requestAppAddition(context, pkg)
                        Log.i(TAG, "Quarantine delay is 0: Guardian auto-allowed newly installed app: $pkg")
                    } else {
                        // Unauthorized app installed -> immediately quarantine
                        GuardianEngine.setPackageSuspendedInternal(context, pkg, true)
                        GuardianEngine.setPackageHiddenInternal(context, pkg, true)
                        Log.w(TAG, "Guardian locked down newly installed unauthorized app: $pkg")
                    }
                }
            }
            Intent.ACTION_PACKAGE_REMOVED, Intent.ACTION_PACKAGE_FULLY_REMOVED -> {
                val pkg = intent.data?.schemeSpecificPart ?: return
                if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                    // App is being updated/replaced, not uninstalled
                    return
                }
                Log.i(TAG, "Package uninstalled from device: $pkg")
                GuardianEngine.init(context)
                GuardianEngine.onPackageRemoved(context, pkg)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.i(TAG, "Boot completed -> Guardian enforcing lockdown")
                GuardianEngine.init(context)
                GuardianEngine.enforceLockdown(context)
            }
            GuardianEngine.ACTION_CHECK_EXPIRATION -> {
                Log.i(TAG, "Guardian expiration alarm triggered -> Checking pending requests")
                GuardianEngine.init(context)
                GuardianEngine.checkAndPromotePendingRequests(context)
            }
            "com.jo.custos.guardian.RESET_WHITELIST_DELAY" -> {
                Log.w(TAG, "Guardian received RESET_WHITELIST_DELAY")
                GuardianEngine.init(context)
                GuardianEngine.resetQuarantineDelay(context)
            }
            "com.jo.custos.guardian.RESET_PENDING_CONFIG" -> {
                Log.w(TAG, "Guardian received RESET_PENDING_CONFIG")
                GuardianEngine.init(context)
                DelayManager.cancelConfigUpdates(context)
            }
            "com.jo.custos.guardian.FORCE_SET_CONFIG" -> {
                Log.w(TAG, "Guardian received FORCE_SET_CONFIG")
                GuardianEngine.init(context)
                val path = intent.getStringExtra("path")
                val json = if (!path.isNullOrBlank()) {
                    val f = java.io.File(path)
                    if (f.exists()) f.readText() else null
                } else {
                    intent.getStringExtra("config")
                }
                if (!json.isNullOrBlank()) {
                    val parsed = ConfigManager.fromJsonString(json)
                    if (parsed != null) {
                        ConfigManager.saveConfig(context, parsed)
                        DelayManager.cancelConfigUpdates(context)
                        GuardianEngine.evaluateLimits(context)
                        Log.i(TAG, "Guardian force set config applied")
                    }
                }
            }
            "com.jo.custos.guardian.UNBLOCK_CLIENT_UNINSTALL" -> {
                Log.w(TAG, "Guardian received UNBLOCK_CLIENT_UNINSTALL")
                GuardianEngine.init(context)
                GuardianEngine.setClientUninstallBlocked(context, false)
            }
            "com.jo.custos.guardian.BLOCK_CLIENT_UNINSTALL" -> {
                Log.w(TAG, "Guardian received BLOCK_CLIENT_UNINSTALL")
                GuardianEngine.init(context)
                GuardianEngine.setClientUninstallBlocked(context, true)
            }
        }
    }

    companion object {
        private const val TAG = "Custos.Guardian.PkgReceiver"
    }
}
