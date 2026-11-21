package com.jo.custos.guardian

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log

/**
 * v1.26: Local Blackhole VPN Service for Total Internet Curfew (Wi-Fi + Mobile Data).
 *
 * Enforced via DevicePolicyManager.setAlwaysOnVpnPackage(admin, pkg, lockdownEnabled = true)
 * so the Android kernel drops 100% of IPv4/IPv6 traffic across Wi-Fi and 4G/5G without
 * suspending any app (offline apps, notes, clock, calls & SMS remain fully usable).
 * Consumes 0% CPU (no packet forwarding loop — packets written to tun0 are silently dropped).
 */
class GuardianBlackholeVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopBlackhole()
            stopSelf()
            return START_NOT_STICKY
        }
        startBlackhole()
        return START_STICKY
    }

    private fun startBlackhole() {
        if (vpnInterface != null) return
        try {
            val builder = Builder()
                .setSession("Custos Internet Curfew")
                .addAddress("10.255.255.2", 32)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("10.255.255.1")
                .setBlocking(false)
            runCatching {
                builder.addAddress("fd00:c057::2", 128)
                builder.addRoute("::", 0)
            }
            vpnInterface = builder.establish()
            if (vpnInterface != null) {
                Log.w(TAG, "🌐 Blackhole VPN established -> 100% of Wi-Fi & Mobile Data IP traffic blocked")
            } else {
                Log.e(TAG, "Failed to establish Blackhole VPN (vpnInterface=null)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error establishing Blackhole VPN: ${e.message}", e)
        }
    }

    private fun stopBlackhole() {
        try {
            vpnInterface?.close()
        } catch (_: Exception) {
        } finally {
            vpnInterface = null
            Log.i(TAG, "🌐 Blackhole VPN closed -> Internet traffic restored")
        }
    }

    override fun onRevoke() {
        stopBlackhole()
        super.onRevoke()
    }

    override fun onDestroy() {
        stopBlackhole()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "Custos.Guardian.VPN"
        const val ACTION_STOP = "com.jo.custos.guardian.ACTION_STOP_BLACKHOLE_VPN"
    }
}
