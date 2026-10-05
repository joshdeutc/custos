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
                    // Unauthorized app installed -> immediately quarantine
                    GuardianEngine.setPackageSuspendedInternal(context, pkg, true)
                    GuardianEngine.setPackageHiddenInternal(context, pkg, true)
                    Log.w(TAG, "Guardian locked down newly installed unauthorized app: $pkg")
                }
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
        }
    }

    companion object {
        private const val TAG = "Custos.Guardian.PkgReceiver"
    }
}
