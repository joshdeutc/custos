package com.jo.custos.guardian

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class GuardianWatcherService : AccessibilityService() {

    companion object {
        private const val TAG = "Custos.Guardian.Watcher"
        @Volatile var isBound = false
        @Volatile var instance: GuardianWatcherService? = null
        @Volatile var currentForegroundApp: String = ""

        private val SETTINGS_PACKAGES = setOf(
            "com.android.settings",
            "com.samsung.android.settings",
            "com.samsung.accessibility",
            "com.miui.securitycenter",
            "com.xiaomi.misettings"
        )

        private val PROTECTED_TARGETS = setOf(
            "Custos", "custos", "Custos Guardian", "Guardian", "guardian",
            "com.jo.selfcontrol.ultimate", "com.jo.custos.guardian"
        )

        private const val HOME_COOLDOWN_MS = 600L
        private var lastHomeMs = 0L
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isBound = true
        instance = this
        Log.i(TAG, "GuardianWatcherService connected and active")
    }

    override fun onDestroy() {
        super.onDestroy()
        isBound = false
        instance = null
        Log.w(TAG, "GuardianWatcherService destroyed")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            currentForegroundApp = pkg
        }

        // 1. HOME spam for blocked apps
        if (pkg in GuardianEngine.getBlockedApps()) {
            val now = System.currentTimeMillis()
            if (now - lastHomeMs > HOME_COOLDOWN_MS) {
                lastHomeMs = now
                Log.w(TAG, "Blocked app in foreground: $pkg -> forcing HOME")
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
            return
        }

        // 2. Settings protection
        if (pkg in SETTINGS_PACKAGES && !DelayManager.isSettingsUnlocked(this)) {
            val root = rootInActiveWindow ?: return
            if (isProtectedScreen(root)) {
                val now = System.currentTimeMillis()
                if (now - lastHomeMs > HOME_COOLDOWN_MS) {
                    lastHomeMs = now
                    Log.w(TAG, "Sensitive settings opened -> forcing HOME")
                    performGlobalAction(GLOBAL_ACTION_HOME)
                }
            }
        }

        // 3. Partial Screen Rules (e.g. Reels, WhatsApp status)
        val screenRules = ScreenRuleManager.load(this).filter { it.packageName == pkg }
        if (screenRules.isNotEmpty()) {
            val root = rootInActiveWindow ?: return
            try {
                for (rule in screenRules) {
                    val allowedHit = rule.allowedIds.any { id ->
                        root.findAccessibilityNodeInfosByViewId(id).isNotEmpty()
                    }
                    if (allowedHit) continue

                    val blockedHit = rule.blockedIds.firstOrNull { id ->
                        root.findAccessibilityNodeInfosByViewId(id).isNotEmpty()
                    }
                    if (blockedHit != null) {
                        val now = System.currentTimeMillis()
                        if (now - lastHomeMs > HOME_COOLDOWN_MS) {
                            lastHomeMs = now
                            Log.w(TAG, "Screen rule triggered: ${rule.name} ($blockedHit) -> forcing BACK")
                            performGlobalAction(GLOBAL_ACTION_BACK)
                        }
                        break
                    }
                }
            } finally {
                root.recycle()
            }
        }
    }

    private fun isProtectedScreen(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""

        for (target in PROTECTED_TARGETS) {
            if (text.contains(target, ignoreCase = true) || desc.contains(target, ignoreCase = true)) {
                return true
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = isProtectedScreen(child)
            child.recycle()
            if (found) return true
        }
        return false
    }

    override fun onInterrupt() {
        Log.w(TAG, "GuardianWatcherService interrupted")
    }
}
