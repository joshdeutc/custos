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

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = IGuardianService.Stub.asInterface(binder)
            isBound = true
            Log.i(TAG, "Connected to GuardianService IPC (version=${service?.version})")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            isBound = false
            Log.w(TAG, "Disconnected from GuardianService IPC")
        }
    }

    fun init(context: Context) {
        if (isBound && service != null) return
        try {
            val intent = Intent(GUARDIAN_ACTION).apply {
                setPackage(GUARDIAN_PACKAGE)
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
        val s = service ?: return 24 * 3600L
        return try {
            s.quarantineDelaySeconds
        } catch (e: Exception) {
            Log.e(TAG, "IPC error getQuarantineDelaySeconds", e)
            24 * 3600L
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
}
