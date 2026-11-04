package com.jo.selfcontrol.ultimate

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * UI-side Accessibility Service for Custos UI (:app).
 *
 * All enforcement (blocked apps HOME spam, Settings/Uninstaller protection,
 * ScreenRule / Instagram redirection, and Session floating countdown overlay)
 * is owned and executed exclusively by GuardianWatcherService (:guardian).
 *
 * This service retains only:
 * 1. Interactive Screen-Learning Wizard overlay & marker capture (ScreenLearnSession)
 * 2. Heads-up notification dismissal for muted apps (SelfControlNotificationListener)
 */
class AppWatcherService : AccessibilityService() {

    companion object {
        private const val TAG = "SelfControl.Watcher"

        @Volatile
        var currentForegroundApp: String = "unknown"

        val blockedApps = java.util.concurrent.ConcurrentSkipListSet<String>()

        @Volatile
        private var instance: AppWatcherService? = null

        val serviceInstance: AppWatcherService? get() = instance

        @Volatile
        var isBound: Boolean = false
            private set

        @Volatile
        var lastBoundTimestamp: Long = 0L
            private set

        fun forceHomeIfBlocked() {
            // Delegated to GuardianWatcherService
        }
    }

    private var imePackage: String? = null
    private val overlayHandler = Handler(Looper.getMainLooper())
    private var learnOverlay: View? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        isBound = true
        lastBoundTimestamp = System.currentTimeMillis()
        CustosApplication.clearCrashFlag(this)
        LimitService.dismissA11yAlert(this)
        imePackage = try {
            Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
                ?.substringBefore('/')
                ?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
        try {
            val info = serviceInfo ?: AccessibilityServiceInfo()
            info.flags = info.flags or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            serviceInfo = info
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update serviceInfo flags: ${e.message}")
        }
        Log.i(TAG, "AccessibilityService connected (UI helper for ScreenLearnSession)")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        isBound = false
        if (instance === this) {
            instance = null
        }
        hideLearnOverlay()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        isBound = false
        if (instance === this) {
            instance = null
        }
        hideLearnOverlay()
        currentForegroundApp = "unknown"
    }

    override fun onInterrupt() {
        Log.w(TAG, "AccessibilityService interrupted")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        try {
            val packageName = event.packageName?.toString() ?: return
            when (event.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    if (!packageName.startsWith("com.android.systemui") &&
                        packageName != imePackage &&
                        packageName != "com.jo.selfcontrol.ultimate"
                    ) {
                        currentForegroundApp = packageName
                    }
                }
                AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                    if (packageName in SelfControlNotificationListener.mutedPackages &&
                        packageName != "com.jo.selfcontrol.ultimate"
                    ) {
                        performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
                        dismissHeadsUpNotification(packageName)
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error in onAccessibilityEvent", t)
        }
    }

    private fun dismissHeadsUpNotification(fromPackage: String) {
        try {
            val rootNode = rootInActiveWindow ?: return
            try {
                findAndDismissNotificationNodes(rootNode, fromPackage)
            } finally {
                rootNode.recycle()
            }
        } catch (_: Exception) {}
    }

    private fun findAndDismissNotificationNodes(node: AccessibilityNodeInfo, targetPackage: String) {
        if (node.isDismissable) {
            try {
                node.performAction(AccessibilityNodeInfo.ACTION_DISMISS)
                return
            } catch (_: Exception) {}
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                findAndDismissNotificationNodes(child, targetPackage)
            } finally {
                child.recycle()
            }
        }
    }

    // ──────────────────────────────────────
    //  Screen learning — guidance overlay
    // ──────────────────────────────────────

    internal fun showLearnOverlay(
        text: String,
        primaryLabel: String,
        onPrimary: () -> Unit,
        onCancel: () -> Unit
    ) {
        overlayHandler.post {
            try {
                hideLearnOverlayInternal()
                val density = resources.displayMetrics.density
                fun dp(v: Int) = (v * density).toInt()

                val panel = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(16), dp(12), dp(16), dp(12))
                    background = GradientDrawable().apply {
                        setColor(Color.parseColor("#F21B1B2E"))
                        cornerRadius = dp(16).toFloat()
                    }
                }
                panel.addView(
                    TextView(this).apply {
                        this.text = text
                        setTextColor(Color.WHITE)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                        typeface = Typeface.DEFAULT_BOLD
                    }
                )
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, dp(8), 0, 0)
                }
                row.addView(
                    Button(this).apply {
                        this.text = primaryLabel
                        setOnClickListener { onPrimary() }
                    }
                )
                row.addView(
                    Button(this).apply {
                        this.text = "Cancel"
                        setOnClickListener { onCancel() }
                    }
                )
                panel.addView(row)

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    y = dp(48)
                }
                (getSystemService(Context.WINDOW_SERVICE) as WindowManager).addView(panel, params)
                learnOverlay = panel
            } catch (e: Exception) {
                Log.e(TAG, "learn overlay failed: ${e.message}")
            }
        }
    }

    internal fun hideLearnOverlay() {
        overlayHandler.post { hideLearnOverlayInternal() }
    }

    private fun hideLearnOverlayInternal() {
        val v = learnOverlay ?: return
        learnOverlay = null
        try {
            (getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(v)
        } catch (_: Exception) {}
    }

    // ──────────────────────────────────────
    //  Screen learning — capture primitives
    // ──────────────────────────────────────

    private fun normalizeText(s: CharSequence?): String = ScreenRuleManager.normalizeText(s)

    private fun visibleIdPresent(pkg: String, viewId: String): Boolean {
        val ids = visibleIdsFor(pkg)
        return viewId in ids
    }

    internal fun visibleIdsFor(pkg: String): Set<String> {
        val ids = LinkedHashSet<String>()
        val prefix = "$pkg:id/"
        var budget = 800

        val dm = resources.displayMetrics
        val screenBounds = Rect()
        fun walk(node: AccessibilityNodeInfo?) {
            if (node == null || budget <= 0) return
            budget--
            val id = node.viewIdResourceName
            node.getBoundsInScreen(screenBounds)
            val onScreen = node.isVisibleToUser || (screenBounds.width() > 0 && screenBounds.height() > 0 &&
                screenBounds.intersects(0, 0, dm.widthPixels, dm.heightPixels))

            if (onScreen) {
                if (id != null && id.startsWith(prefix)) {
                    ids.add(id)
                }
                val txt = normalizeText(node.text)
                if (txt.length in 3..35 && !txt.all { it.isDigit() || it == ':' || it == ' ' }) {
                    ids.add("text:$txt")
                }
                val desc = normalizeText(node.contentDescription)
                if (desc.length in 3..40 && !desc.all { it.isDigit() || it == ':' || it == ' ' } && desc != txt) {
                    ids.add("desc:$desc")
                }
            }
            val count = node.childCount
            for (i in 0 until count) {
                if (budget <= 0) break
                val child = try { node.getChild(i) } catch (_: Exception) { null } ?: continue
                try {
                    walk(child)
                } finally {
                    child.recycle()
                }
            }
        }

        try {
            var foundInWindows = false
            for (window in windows) {
                val root = window.root ?: continue
                if (root.packageName?.toString() == pkg) {
                    foundInWindows = true
                    try {
                        walk(root)
                    } finally {
                        root.recycle()
                    }
                } else {
                    root.recycle()
                }
            }
            if (!foundInWindows) {
                rootInActiveWindow?.let { root ->
                    try {
                        if (root.packageName?.toString() == pkg) {
                            walk(root)
                        }
                    } finally {
                        root.recycle()
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "visibleIdsFor($pkg) failed: ${e.message}")
        }
        return ids
    }

    internal fun selectedNavItem(pkg: String): Pair<String, Int>? {
        if (pkg == "com.snapchat.android") {
            if (visibleIdPresent(pkg, "com.snapchat.android:id/ff_item") || visibleIdPresent(pkg, "text:Chat")) {
                return "com.snapchat.android:id/ngs_chat_icon_container" to 0
            }
            if (visibleIdPresent(pkg, "com.snapchat.android:id/camera_page") || visibleIdPresent(pkg, "com.snapchat.android:id/camera_capture_button")) {
                return "com.snapchat.android:id/ngs_camera_icon_container" to 0
            }
            if (visibleIdPresent(pkg, "com.snapchat.android:id/df_large_story") || visibleIdPresent(pkg, "com.snapchat.android:id/friend_card_frame") || visibleIdPresent(pkg, "text:Stories")) {
                return "com.snapchat.android:id/ngs_community_icon_container" to 0
            }
            if (visibleIdPresent(pkg, "com.snapchat.android:id/spotlight_container") || visibleIdPresent(pkg, "text:Spotlight")) {
                return "com.snapchat.android:id/ngs_spotlight_icon_container" to 0
            }
            return "com.snapchat.android:id/ngs_chat_icon_container" to 0
        }

        val prefix = "$pkg:id/"
        val byId = LinkedHashMap<String, MutableList<Boolean>>()
        var budget = 800
        val dm = resources.displayMetrics
        val screenBounds = Rect()
        var selectedItemTarget: String? = null

        fun walk(node: AccessibilityNodeInfo?) {
            if (node == null || budget <= 0) return
            budget--
            val id = node.viewIdResourceName
            val isSel = node.isSelected || node.isChecked
            node.getBoundsInScreen(screenBounds)
            val isBottomBar = screenBounds.bottom >= dm.heightPixels * 0.70f

            if (id != null && id.startsWith(prefix) && node.isVisibleToUser) {
                byId.getOrPut(id) { mutableListOf() }.add(isSel)
            }

            if (isSel && isBottomBar && selectedItemTarget == null) {
                val txt = normalizeText(node.text)
                val desc = normalizeText(node.contentDescription)
                when {
                    txt.isNotBlank() && txt.length in 2..25 -> selectedItemTarget = "text:$txt"
                    desc.isNotBlank() && desc.length in 2..35 -> selectedItemTarget = "desc:$desc"
                    id != null && id.startsWith(prefix) -> selectedItemTarget = id
                }
            }

            val count = node.childCount
            for (i in 0 until count) {
                if (budget <= 0) break
                val child = try { node.getChild(i) } catch (_: Exception) { null } ?: continue
                try {
                    walk(child)
                } finally {
                    child.recycle()
                }
            }
        }

        try {
            var found = false
            for (window in windows) {
                val root = window.root ?: continue
                if (root.packageName?.toString() == pkg) {
                    found = true
                    try {
                        walk(root)
                    } finally {
                        root.recycle()
                    }
                } else {
                    root.recycle()
                }
            }
            if (!found) {
                rootInActiveWindow?.let { root ->
                    try {
                        if (root.packageName?.toString() == pkg) walk(root)
                    } finally {
                        root.recycle()
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "selectedNavItem($pkg) failed: ${e.message}")
            return null
        }

        val tabLike = byId.entries
            .filter { it.value.size in 2..8 && it.value.count { sel -> sel } == 1 }
            .minByOrNull { it.value.size }
        if (tabLike != null) {
            return tabLike.key to tabLike.value.indexOfFirst { it }
        }
        val single = byId.entries.firstOrNull { it.value.size == 1 && it.value[0] }
        if (single != null) {
            return single.key to 0
        }
        val target = selectedItemTarget
        if (target != null) {
            return target to 0
        }
        return null
    }
}
