package com.jo.custos.guardian

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class GuardianAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i(TAG, "Guardian Device Admin enabled")
        GuardianEngine.init(context)
        GuardianEngine.enforceLockdown(context)
    }

    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        super.onProfileProvisioningComplete(context, intent)
        Log.i(TAG, "Guardian Device Owner provisioning complete")
        GuardianEngine.init(context)
        GuardianEngine.enforceLockdown(context)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.w(TAG, "Guardian Device Admin disabled")
    }

    companion object {
        private const val TAG = "Custos.Guardian.Admin"
    }
}
