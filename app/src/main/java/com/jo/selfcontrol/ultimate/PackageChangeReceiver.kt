package com.jo.selfcontrol.ultimate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Manifest-declared receiver for package changes.
 * ACTION_PACKAGE_REMOVED and ACTION_PACKAGE_FULLY_REMOVED are exempt from Android 8+
 * implicit broadcast restrictions, ensuring uninstalled apps are immediately purged
 * from the whitelist even if LimitService was temporarily stopped.
 */
class PackageChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pkg = intent.data?.schemeSpecificPart ?: return
        val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)

        when (action) {
            Intent.ACTION_PACKAGE_REMOVED, Intent.ACTION_PACKAGE_FULLY_REMOVED -> {
                if (!isReplacing) {
                    Log.i(TAG, "📦 Package uninstalled detected by manifest receiver: $pkg")
                    WhitelistManager.onPackageRemoved(context, pkg)
                }
            }
        }
    }

    companion object {
        private const val TAG = "SelfControl.PkgReceiver"
    }
}
