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
                GuardianEngine.onPackageAdded(context, pkg)
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
                GuardianEngine.scheduleHeartbeat(context)
            }
            GuardianEngine.ACTION_CHECK_EXPIRATION -> {
                Log.i(TAG, "Guardian expiration alarm triggered -> Checking pending requests")
                GuardianEngine.init(context)
                GuardianEngine.checkAndPromotePendingRequests(context)
            }
            GuardianEngine.ACTION_HEARTBEAT -> {
                // Backstop enforcement tick (F1/F3): survives UI clear + a11y kill + process death.
                GuardianEngine.onHeartbeat(context)
            }
        }
    }

    companion object {
        private const val TAG = "Custos.Guardian.PkgReceiver"
    }
}
