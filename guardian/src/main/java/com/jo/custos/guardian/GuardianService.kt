package com.jo.custos.guardian

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class GuardianService : Service() {

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "GuardianService created")
        GuardianEngine.init(this)
        GuardianEngine.enforceLockdown(this)
    }

    private val binder = object : IGuardianService.Stub() {

        override fun getVersion(): Int = 1

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
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.i(TAG, "Client bound to GuardianService: ${intent?.action}")
        return binder
    }

    companion object {
        private const val TAG = "Custos.Guardian.Service"
    }
}
