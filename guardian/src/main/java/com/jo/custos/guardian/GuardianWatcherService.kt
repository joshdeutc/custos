package com.jo.custos.guardian

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import java.io.File
import java.util.Calendar

class GuardianWatcherService : AccessibilityService() {

    companion object {
        private const val TAG = "Custos.Guardian.Watcher"
        @Volatile var isBound = false
        @Volatile var instance: GuardianWatcherService? = null
        @Volatile var currentForegroundApp: String = ""

        private val SETTINGS_PACKAGES = setOf(
            "com.android.settings",
            "com.samsung.android.settings",
            "com.samsung.android.app.routines",
            "com.samsung.android.sm",
            "com.samsung.accessibility",
            "com.miui.securitycenter",
            "com.miui.securitycore",
            "com.xiaomi.misettings"
        )

        private val UNINSTALL_PACKAGES = setOf(
            "com.google.android.packageinstaller",
            "com.android.packageinstaller",
            "com.samsung.android.packageinstaller"
        )

        private val PROTECTED_TARGETS = setOf(
            "Custos", "custos", "SelfControl", "selfcontrol", "Self control", "Self Control",
            "Custos Guardian", "Guardian", "guardian",
            "com.jo.selfcontrol.ultimate", "com.jo.custos.guardian"
        )

        private val CANCEL_KEYWORDS = setOf("Cancel", "Annuler", "No", "Non")

        private val DANGER_KEYWORDS = setOf(
            "Disable", "Désactiver",
            "Force stop", "Forcer l'arrêt",
            "Uninstall", "Désinstaller",
            "Clear data", "Clear storage",
            "Supprimer les données", "Effacer les données", "Effacer le stockage",
            "Désactiver cette application", "Désactiver l'administrateur", "Deactivate"
        )

        private const val HOME_COOLDOWN_MS = 500L
        private const val SCREEN_RULE_SCAN_INTERVAL_MS = 80L
        private const val SCREEN_RULE_REDIRECT_COOLDOWN_MS = 500L

        const val INSTAGRAM_PACKAGE = "com.instagram.android"
        private const val INSTAGRAM_SCAN_INTERVAL_MS = 80L
        private const val INSTAGRAM_DIRECT_TAB_VIEW_ID = "com.instagram.android:id/direct_tab"
        private const val INSTAGRAM_INBOX_URI = "https://www.instagram.com/direct/inbox/"
        private const val INSTAGRAM_REDIRECT_COOLDOWN_MS = 500L

        private val INSTAGRAM_PREFER_DEEPLINK_IDS = setOf(
            "com.instagram.android:id/explore_action_bar",
            "com.instagram.android:id/action_bar_search_edit_text"
        )

        private val INSTAGRAM_TAB_VIEW_IDS = listOf(
            "com.instagram.android:id/feed_tab",
            "com.instagram.android:id/clips_tab",
            INSTAGRAM_DIRECT_TAB_VIEW_ID,
            "com.instagram.android:id/search_tab",
            "com.instagram.android:id/profile_tab"
        )

        private var lastHomeMs = 0L

        fun forceHomeIfBlocked() {
            val inst = instance ?: return
            val pkg = currentForegroundApp
            if (pkg in GuardianEngine.getBlockedApps()) {
                inst.goHome("Forced: $pkg blocked")
            }
        }

        fun updateSessionOverlay(text: String?) {
            instance?.applySessionOverlay(text)
        }

        fun recheckScreenRules(pkg: String) {
            if (pkg.isBlank() || pkg == "unknown") return
            instance?.enforceScreenRules(pkg)
        }
    }

    private var imePackage: String? = null
    private var overlayView: TextView? = null
    private val overlayHandler = Handler(Looper.getMainLooper())

    private var screenRules: List<ScreenRuleManager.ScreenRule> = emptyList()
    private var screenRulesStamp = 0L
    private var screenRulesCheckedAt = 0L
    private var lastScreenRuleActionAt = 0L
    private var lastScreenRuleScanAt = 0L
    private var lastInstagramBackTime = 0L
    private var lastInstagramScanTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        isBound = true
        instance = this
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
        Log.i(TAG, "GuardianWatcherService connected and active (ime=$imePackage)")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        isBound = false
        if (instance === this) instance = null
        applySessionOverlay(null)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        isBound = false
        if (instance === this) instance = null
        applySessionOverlay(null)
        Log.w(TAG, "GuardianWatcherService destroyed")
    }

    internal fun goHome(reason: String): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastHomeMs < HOME_COOLDOWN_MS) return false
        lastHomeMs = now
        Log.w(TAG, "Guardian protection activated: $reason -> forcing HOME")
        EventLog.log(this, "HOME", "forced home: $reason (fg=$currentForegroundApp)")
        performGlobalAction(GLOBAL_ACTION_HOME)
        return true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        try {
            val pkg = event.packageName?.toString() ?: return

            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                if (!pkg.startsWith("com.android.systemui") && pkg != imePackage && pkg != "com.jo.custos.guardian") {
                    if (pkg != currentForegroundApp) {
                        currentForegroundApp = pkg
                        applySessionOverlay(null)
                    }
                }
            }

            // 1. HOME spam for blocked apps
            if (pkg in GuardianEngine.getBlockedApps()) {
                goHome("Blocked app in foreground: $pkg")
                return
            }

            // 2. Settings & Uninstaller protection
            if (!DelayManager.isSettingsUnlocked(this)) {
                val cls = event.className?.toString() ?: ""
                val isDialog = cls.contains("Dialog", ignoreCase = true) || cls.contains("Popup", ignoreCase = true)
                if (pkg in SETTINGS_PACKAGES || pkg in UNINSTALL_PACKAGES || (pkg == "android" && isDialog)) {
                    if (isSensitiveSettingsScreen(cls) || pkg in UNINSTALL_PACKAGES) {
                        val root = rootInActiveWindow
                        if (root != null) {
                            try {
                                if (isProtectedScreen(root, requireDangerKeyword = (isDialog || pkg in UNINSTALL_PACKAGES))) {
                                    clickCancelIfPresent(root)
                                    goHome("Sensitive settings opened ($cls)")
                                    return
                                }
                            } finally {
                                root.recycle()
                            }
                        }
                    }
                }
            }

            // 3. Partial Screen Rules (Reels, WhatsApp Status, Shorts, etc.)
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            ) {
                enforceScreenRules(pkg)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error in GuardianWatcherService.onAccessibilityEvent", t)
        }
    }

    private fun isSensitiveSettingsScreen(className: CharSequence?): Boolean {
        val cls = className?.toString() ?: return false
        return cls.contains("AppInfo", ignoreCase = true)
            || cls.contains("InstalledAppDetails", ignoreCase = true)
            || cls.contains("ToggleAccessibilityService", ignoreCase = true)
            || cls.contains("AccessibilityDetailsSettings", ignoreCase = true)
            || cls.contains("InstalledAccessibilityService", ignoreCase = true)
            || cls.contains("ToggleFeaturePreference", ignoreCase = true)
            || cls.contains("UninstallerActivity", ignoreCase = true)
            || cls.contains("UninstallUninstalling", ignoreCase = true)
            || cls.contains("UninstallAppProgress", ignoreCase = true)
            || cls.contains("DeletePackageActivity", ignoreCase = true)
            || cls.contains("DeviceAdminAdd", ignoreCase = true)
            || cls.contains("DeviceAdminSettings", ignoreCase = true)
            || cls.contains("ToggleUsageAccess", ignoreCase = true)
            || cls.contains("ToggleNotificationAccess", ignoreCase = true)
            || cls.contains("Dialog", ignoreCase = true)
            || cls.contains("Popup", ignoreCase = true)
    }

    private fun isProtectedScreen(rootNode: AccessibilityNodeInfo, requireDangerKeyword: Boolean): Boolean {
        var isOurAppOnScreen = false
        for (appName in PROTECTED_TARGETS) {
            val appNodes = rootNode.findAccessibilityNodeInfosByText(appName)
            if (!appNodes.isNullOrEmpty()) {
                for (node in appNodes) {
                    val nodePkg = node.packageName?.toString() ?: ""
                    if (nodePkg == "com.android.systemui") continue
                    val text = node.text?.toString()?.trim() ?: ""
                    val desc = node.contentDescription?.toString()?.trim() ?: ""
                    val isMatch = text.equals(appName, ignoreCase = true) ||
                        desc.equals(appName, ignoreCase = true) ||
                        Regex("\\b${Regex.escape(appName)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(text) ||
                        Regex("\\b${Regex.escape(appName)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(desc)
                    if (isMatch) {
                        isOurAppOnScreen = true
                        break
                    }
                }
                appNodes.forEach { it.recycle() }
                if (isOurAppOnScreen) break
            }
        }
        if (!isOurAppOnScreen) return false
        if (!requireDangerKeyword) return true

        var dangerFound = false
        for (danger in DANGER_KEYWORDS) {
            val dangerNodes = rootNode.findAccessibilityNodeInfosByText(danger)
            if (!dangerNodes.isNullOrEmpty()) {
                for (node in dangerNodes) {
                    val nodePkg = node.packageName?.toString() ?: ""
                    if (nodePkg == "com.android.systemui") continue
                    val text = node.text?.toString()?.trim() ?: ""
                    val desc = node.contentDescription?.toString()?.trim() ?: ""
                    val isMatch = text.equals(danger, ignoreCase = true) ||
                        desc.equals(danger, ignoreCase = true) ||
                        Regex("\\b${Regex.escape(danger)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(text) ||
                        Regex("\\b${Regex.escape(danger)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(desc)
                    if (isMatch) {
                        dangerFound = true
                        break
                    }
                }
                dangerNodes.forEach { it.recycle() }
                if (dangerFound) break
            }
        }
        return dangerFound
    }

    private fun clickCancelIfPresent(rootNode: AccessibilityNodeInfo) {
        for (cancel in CANCEL_KEYWORDS) {
            val cancelNodes = rootNode.findAccessibilityNodeInfosByText(cancel)
            if (!cancelNodes.isNullOrEmpty()) {
                var clicked = false
                for (node in cancelNodes) {
                    val text = node.text?.toString()?.trim() ?: ""
                    val desc = node.contentDescription?.toString()?.trim() ?: ""
                    if ((text.equals(cancel, ignoreCase = true) || desc.equals(cancel, ignoreCase = true)) && node.isClickable) {
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        clicked = true
                        break
                    }
                }
                cancelNodes.forEach { it.recycle() }
                if (clicked) break
            }
        }
    }

    // ──────────────────────────────────────
    //  Learned screen rules — full enforcement
    // ──────────────────────────────────────

    internal fun enforceScreenRules(pkg: String): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastScreenRuleActionAt < SCREEN_RULE_REDIRECT_COOLDOWN_MS) return false
        if (now - lastScreenRuleScanAt < SCREEN_RULE_SCAN_INTERVAL_MS) return false
        lastScreenRuleScanAt = now

        reloadScreenRulesIfStale(now)
        val rules = screenRules.filter { it.packageName == pkg }
        if (rules.isEmpty()) return false

        val activeRules = rules.filter { isRuleActiveNow(it) }
        if (activeRules.isEmpty()) return false

        if (pkg == INSTAGRAM_PACKAGE) {
            val root = rootInActiveWindow
            if (root != null) {
                try {
                    val selectedTab = instagramSelectedTab(root)
                    if (selectedTab == INSTAGRAM_DIRECT_TAB_VIEW_ID) return false
                } finally {
                    root.recycle()
                }
            }
        }

        for (rule in activeRules) {
            if (rule.allowedIds.isNotEmpty() && rule.allowedIds.any { visibleIdPresent(pkg, it) }) {
                continue
            }

            var hit: String? = null
            if (pkg == INSTAGRAM_PACKAGE) {
                val root = rootInActiveWindow
                if (root != null) {
                    try {
                        val selectedTab = instagramSelectedTab(root)
                        if (selectedTab != null && (selectedTab in rule.blockedIds || rule.blockedIds.any { selectedTab.endsWith(it) })) {
                            hit = selectedTab
                        }
                    } finally {
                        root.recycle()
                    }
                }
            }

            if (hit == null) {
                hit = rule.blockedIds.firstOrNull { marker -> visibleIdPresent(pkg, marker) }
            }
            if (hit == null) continue

            lastScreenRuleActionAt = now
            Log.w(TAG, "Enforcing screen rule '${rule.name}' for $pkg (hit=$hit)")
            performScreenEscape(rule, hit)
            return true
        }
        return false
    }

    private fun openInstagramInbox(): Boolean = openDeeplink(INSTAGRAM_PACKAGE, INSTAGRAM_INBOX_URI)

    private fun instagramSelectedTab(root: AccessibilityNodeInfo): String? {
        for (id in INSTAGRAM_TAB_VIEW_IDS) {
            val selected = try {
                val nodes = root.findAccessibilityNodeInfosByViewId(id)
                val res = nodes.any { it.isVisibleToUser && (it.isSelected || it.isChecked) }
                nodes.forEach { it.recycle() }
                res
            } catch (_: Exception) {
                false
            }
            if (selected) return id
        }
        return null
    }

    private fun findNodeInAnyWindow(viewId: String): AccessibilityNodeInfo? {
        try {
            for (window in windows) {
                val root = window.root ?: continue
                val found = root.findAccessibilityNodeInfosByViewId(viewId).firstOrNull { it.isVisibleToUser }
                root.recycle()
                if (found != null) return found
            }
        } catch (_: Exception) {}
        return try {
            rootInActiveWindow?.findAccessibilityNodeInfosByViewId(viewId)?.firstOrNull { it.isVisibleToUser }
        } catch (_: Exception) {
            null
        }
    }

    private fun tapInstagramDirectTab(): Boolean = try {
        val icon = findNodeInAnyWindow(INSTAGRAM_DIRECT_TAB_VIEW_ID)
        if (icon == null) {
            false
        } else {
            val bounds = Rect()
            icon.getBoundsInScreen(bounds)
            icon.recycle()
            if (bounds.isEmpty) {
                false
            } else {
                val path = Path().apply { moveTo(bounds.exactCenterX(), bounds.exactCenterY()) }
                val gesture = GestureDescription.Builder()
                    .addStroke(GestureDescription.StrokeDescription(path, 0L, 10L))
                    .build()
                dispatchGesture(gesture, null, null)
            }
        }
    } catch (_: Exception) {
        false
    }

    private fun isRuleActiveNow(rule: ScreenRuleManager.ScreenRule): Boolean {
        val hours = rule.blockedHours ?: return true
        if (hours == "*" || hours.isBlank()) return true
        val parts = hours.split("-")
        if (parts.size != 2) return true
        return try {
            val start = ConfigManager.parseTimeToMinutes(parts[0])
            val end = ConfigManager.parseTimeToMinutes(parts[1])
            val cal = Calendar.getInstance()
            val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            ConfigManager.isInBlockedWindow(nowMin, start, end)
        } catch (_: Exception) {
            true
        }
    }

    private fun reloadScreenRulesIfStale(now: Long) {
        if (now - screenRulesCheckedAt < 2_000L) return
        screenRulesCheckedAt = now
        val stamp = try {
            File(filesDir, "screen_rules.json").lastModified()
        } catch (_: Exception) {
            0L
        }
        if (stamp == screenRulesStamp) return
        screenRulesStamp = stamp
        screenRules = ScreenRuleManager.load(this)
    }

    private fun normalizeText(s: CharSequence?): String = ScreenRuleManager.normalizeText(s)

    private fun visibleIdPresent(pkg: String, viewId: String): Boolean {
        try {
            val activeRoot = rootInActiveWindow
            if (activeRoot != null) {
                try {
                    if (activeRoot.packageName?.toString() == pkg && checkNodeMatch(activeRoot, viewId)) {
                        return true
                    }
                } finally {
                    activeRoot.recycle()
                }
            }
        } catch (_: Exception) {}

        try {
            val currentWindows = windows ?: return false
            for (window in currentWindows) {
                val root = window.root ?: continue
                try {
                    if (root.packageName?.toString() == pkg && checkNodeMatch(root, viewId)) {
                        return true
                    }
                } finally {
                    root.recycle()
                }
            }
        } catch (_: Exception) {}
        return false
    }

    private fun checkNodeMatch(root: AccessibilityNodeInfo, targetId: String): Boolean {
        val dm = resources.displayMetrics
        val screenBounds = Rect()
        var budget = 400

        val isText = targetId.startsWith("text:")
        val isDesc = targetId.startsWith("desc:")
        val isId = !isText && !isDesc

        val targetVal = when {
            isText -> normalizeText(targetId.removePrefix("text:"))
            isDesc -> normalizeText(targetId.removePrefix("desc:"))
            else -> targetId
        }
        val cleanWord = targetVal.filter { it.isLetterOrDigit() || it == ' ' }.trim()

        fun isNodeOnScreen(node: AccessibilityNodeInfo): Boolean {
            if (node.isVisibleToUser) return true
            node.getBoundsInScreen(screenBounds)
            return screenBounds.width() > 0 && screenBounds.height() > 0 &&
                screenBounds.intersects(0, 0, dm.widthPixels, dm.heightPixels)
        }

        fun matchesNode(node: AccessibilityNodeInfo): Boolean {
            if (!isNodeOnScreen(node)) return false
            if (isId) return node.viewIdResourceName == targetVal
            val nodeText = normalizeText(node.text)
            val nodeDesc = normalizeText(node.contentDescription)
            val maxLen = kotlin.math.max(50, targetVal.length * 3)
            val textLenOk = nodeText.length <= maxLen
            val descLenOk = nodeDesc.length <= maxLen

            if (isText) {
                if (nodeText.equals(targetVal, ignoreCase = true) || nodeDesc.equals(targetVal, ignoreCase = true)) return true
                if (textLenOk && nodeText.contains(targetVal, ignoreCase = true)) return true
                if (descLenOk && nodeDesc.contains(targetVal, ignoreCase = true)) return true
                if (cleanWord.length >= 4) {
                    if (textLenOk && nodeText.contains(cleanWord, ignoreCase = true)) return true
                    if (descLenOk && nodeDesc.contains(cleanWord, ignoreCase = true)) return true
                }
            } else if (isDesc) {
                if (nodeDesc.equals(targetVal, ignoreCase = true) || nodeText.equals(targetVal, ignoreCase = true)) return true
                if (descLenOk && nodeDesc.contains(targetVal, ignoreCase = true)) return true
                if (textLenOk && nodeText.contains(targetVal, ignoreCase = true)) return true
                if (cleanWord.length >= 4) {
                    if (descLenOk && nodeDesc.contains(cleanWord, ignoreCase = true)) return true
                    if (textLenOk && nodeText.contains(cleanWord, ignoreCase = true)) return true
                }
            }
            return false
        }

        fun dfs(node: AccessibilityNodeInfo): Boolean {
            if (budget <= 0) return false
            budget--
            if (matchesNode(node)) return true
            val count = node.childCount
            for (i in 0 until count) {
                if (budget <= 0) break
                val child = try { node.getChild(i) } catch (_: Exception) { null } ?: continue
                try {
                    if (dfs(child)) return true
                } finally {
                    child.recycle()
                }
            }
            return false
        }

        try {
            if (isId) {
                val nodes = root.findAccessibilityNodeInfosByViewId(targetVal)
                if (!nodes.isNullOrEmpty()) {
                    val hit = nodes.any { isNodeOnScreen(it) }
                    nodes.forEach { it.recycle() }
                    if (hit) return true
                }
            } else {
                val searchWord = if (cleanWord.length >= 3) cleanWord else targetVal
                val nodes = root.findAccessibilityNodeInfosByText(searchWord)
                if (!nodes.isNullOrEmpty()) {
                    val hit = nodes.any { matchesNode(it) }
                    nodes.forEach { it.recycle() }
                    if (hit) return true
                }
            }
        } catch (_: Exception) {}

        return dfs(root)
    }

    private fun performScreenEscape(rule: ScreenRuleManager.ScreenRule, hit: String) {
        val pkg = rule.packageName
        val escapeTapId = rule.escapeTapId ?: if (pkg == "com.snapchat.android") {
            "com.snapchat.android:id/ngs_chat_icon_container"
        } else null

        var how = "failed"
        if (escapeTapId != null && tapNavItem(pkg, escapeTapId, rule.escapeTapIndex)) {
            how = "tap"
        } else if (rule.escapeDeeplink != null && openDeeplink(pkg, rule.escapeDeeplink)) {
            how = "deeplink"
        } else if (pkg == INSTAGRAM_PACKAGE && hit in INSTAGRAM_PREFER_DEEPLINK_IDS && openInstagramInbox()) {
            how = "instagram_deeplink"
        } else if (pkg == "com.snapchat.android" && tapSnapchatChat()) {
            how = "snapchat_chat_coords"
        } else if (pkg == INSTAGRAM_PACKAGE && (tapInstagramDirectTab() || openInstagramInbox())) {
            how = "instagram_direct_fallback"
        } else {
            val fallbackTapped = when (pkg) {
                "com.android.vending" -> {
                    tapNavItem(pkg, "text:Jeux", 0) ||
                        tapNavItem(pkg, "desc:Jeux", 0) ||
                        tapNavItem(pkg, "text:Applis", 0) ||
                        tapNavItem(pkg, "desc:Applis", 0)
                }
                else -> false
            }

            if (fallbackTapped) {
                how = "fallback_nav_tap"
            } else {
                val navAllowed = rule.allowedIds.firstOrNull { it.startsWith("text:") || it.startsWith("desc:") }
                if (navAllowed != null && tapNavItem(pkg, navAllowed, 0)) {
                    how = "allowed_marker_tap"
                } else if (performGlobalAction(GLOBAL_ACTION_BACK)) {
                    how = "back"
                } else {
                    val launchIntent = packageManager.getLaunchIntentForPackage(pkg)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    if (launchIntent != null) {
                        startActivity(launchIntent)
                        how = "clear_top_relaunch"
                    } else {
                        goHome("screen_escape_fallback")
                        how = "home_fallback"
                    }
                }
            }
        }
        val shortHit = hit.substringAfter(":id/")
        Log.w(TAG, "${rule.name}: $shortHit -> escape ($how)")

        overlayHandler.postDelayed({
            val stillBlocked = rule.blockedIds.any { visibleIdPresent(pkg, it) }
            val reached = !stillBlocked && (rule.allowedIds.isEmpty() || rule.allowedIds.any { visibleIdPresent(pkg, it) })
            ScreenRuleManager.recordOutcome(this, rule.name, reached)
            EventLog.log(this, "SCREEN_RULE", "${rule.name} $shortHit -> $how, reached=$reached")

            if (stillBlocked) {
                val launchIntent = packageManager.getLaunchIntentForPackage(pkg)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                if (launchIntent != null) {
                    startActivity(launchIntent)
                } else {
                    goHome("screen_escape_stuck")
                }
            }
        }, 900L)
    }

    private fun tapSnapchatChat(): Boolean = try {
        val dm = resources.displayMetrics
        val x = dm.widthPixels * 0.316f
        val y = dm.heightPixels * 0.909f
        val path = Path().apply { moveTo(x, y) }
        dispatchGesture(
            GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0L, 10L))
                .build(),
            null, null
        )
    } catch (_: Exception) {
        false
    }

    private fun openDeeplink(pkg: String, uri: String): Boolean = try {
        startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
                setPackage(pkg)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
        true
    } catch (_: Exception) {
        false
    }

    private fun tapNavItem(pkg: String, viewId: String, index: Int): Boolean {
        val matches = mutableListOf<AccessibilityNodeInfo>()
        val dm = resources.displayMetrics
        val screenBounds = Rect()

        fun isNodeOnScreen(node: AccessibilityNodeInfo): Boolean {
            if (node.isVisibleToUser) return true
            node.getBoundsInScreen(screenBounds)
            return screenBounds.width() > 0 && screenBounds.height() > 0 &&
                screenBounds.intersects(0, 0, dm.widthPixels, dm.heightPixels)
        }

        fun findInRoot(root: AccessibilityNodeInfo) {
            val isText = viewId.startsWith("text:")
            val isDesc = viewId.startsWith("desc:")
            val isId = !isText && !isDesc

            val targetVal = when {
                isText -> normalizeText(viewId.removePrefix("text:"))
                isDesc -> normalizeText(viewId.removePrefix("desc:"))
                else -> viewId
            }
            val cleanWord = targetVal.filter { it.isLetterOrDigit() || it == ' ' }.trim()

            fun matchesNode(node: AccessibilityNodeInfo): Boolean {
                if (!isNodeOnScreen(node)) return false
                if (isId) return node.viewIdResourceName == targetVal
                val nodeText = normalizeText(node.text)
                val nodeDesc = normalizeText(node.contentDescription)
                val maxLen = kotlin.math.max(50, targetVal.length * 3)
                val textLenOk = nodeText.length <= maxLen
                val descLenOk = nodeDesc.length <= maxLen

                if (isText) {
                    if (nodeText.equals(targetVal, ignoreCase = true) || nodeDesc.equals(targetVal, ignoreCase = true)) return true
                    if (textLenOk && nodeText.contains(targetVal, ignoreCase = true)) return true
                    if (descLenOk && nodeDesc.contains(targetVal, ignoreCase = true)) return true
                    if (cleanWord.length >= 4) {
                        if (textLenOk && nodeText.contains(cleanWord, ignoreCase = true)) return true
                        if (descLenOk && nodeDesc.contains(cleanWord, ignoreCase = true)) return true
                    }
                } else if (isDesc) {
                    if (nodeDesc.equals(targetVal, ignoreCase = true) || nodeText.equals(targetVal, ignoreCase = true)) return true
                    if (descLenOk && nodeDesc.contains(targetVal, ignoreCase = true)) return true
                    if (textLenOk && nodeText.contains(targetVal, ignoreCase = true)) return true
                    if (cleanWord.length >= 4) {
                        if (descLenOk && nodeDesc.contains(cleanWord, ignoreCase = true)) return true
                        if (textLenOk && nodeText.contains(cleanWord, ignoreCase = true)) return true
                    }
                }
                return false
            }

            try {
                if (isId) {
                    val nodes = root.findAccessibilityNodeInfosByViewId(targetVal)
                    if (nodes != null) {
                        for (node in nodes) {
                            if (isNodeOnScreen(node)) matches.add(node) else node.recycle()
                        }
                    }
                } else {
                    val searchWord = if (cleanWord.length >= 3) cleanWord else targetVal
                    val nodes = root.findAccessibilityNodeInfosByText(searchWord)
                    if (nodes != null) {
                        for (node in nodes) {
                            if (matchesNode(node)) matches.add(node) else node.recycle()
                        }
                    }
                }
            } catch (_: Exception) {}

            if (matches.isEmpty()) {
                var budget = 400
                fun dfs(node: AccessibilityNodeInfo) {
                    if (budget <= 0) return
                    budget--
                    if (matchesNode(node)) {
                        matches.add(node)
                        return
                    }
                    val count = node.childCount
                    for (i in 0 until count) {
                        if (budget <= 0 || matches.isNotEmpty()) break
                        val child = try { node.getChild(i) } catch (_: Exception) { null } ?: continue
                        dfs(child)
                        if (!matches.contains(child)) child.recycle()
                    }
                }
                dfs(root)
            }
        }

        try {
            var found = false
            for (window in windows) {
                val root = window.root ?: continue
                if (root.packageName?.toString() == pkg) {
                    found = true
                    try {
                        findInRoot(root)
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
                        if (root.packageName?.toString() == pkg) findInRoot(root)
                    } finally {
                        root.recycle()
                    }
                }
            }
        } catch (_: Exception) {
            matches.forEach { it.recycle() }
            return false
        }

        val node = matches.getOrNull(index) ?: matches.firstOrNull() ?: run {
            matches.forEach { it.recycle() }
            return false
        }

        var curr: AccessibilityNodeInfo? = node
        var clicked = false
        while (curr != null) {
            if (curr.isClickable) {
                clicked = curr.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (clicked) break
            }
            val parent = curr.parent
            if (curr != node) curr.recycle()
            curr = parent
        }

        val bounds = Rect().also { node.getBoundsInScreen(it) }
        var gestured = false
        if (!bounds.isEmpty) {
            try {
                val path = Path().apply { moveTo(bounds.exactCenterX(), bounds.exactCenterY()) }
                gestured = dispatchGesture(
                    GestureDescription.Builder()
                        .addStroke(GestureDescription.StrokeDescription(path, 0L, 10L))
                        .build(),
                    null, null
                )
            } catch (_: Exception) {}
        }
        matches.forEach { it.recycle() }
        return clicked || gestured
    }

    // ──────────────────────────────────────
    //  Session overlay (owned by Guardian)
    // ──────────────────────────────────────

    private fun applySessionOverlay(text: String?) {
        overlayHandler.post {
            try {
                if (text == null) {
                    removeOverlayView()
                } else {
                    ensureOverlayView()
                    overlayView?.text = text
                }
            } catch (_: Exception) {}
        }
    }

    private fun ensureOverlayView() {
        if (overlayView != null) return
        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        val bg = GradientDrawable().apply {
            setColor(Color.parseColor("#CC000000"))
            cornerRadius = dp(18).toFloat()
        }
        val tv = TextView(this).apply {
            background = bg
            setTextColor(Color.parseColor("#80CBC4"))
            setPadding(dp(16), dp(8), dp(16), dp(8))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.DEFAULT_BOLD
            text = ""
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(40)
        }
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        try {
            wm.addView(tv, params)
            overlayView = tv
        } catch (_: Exception) {}
    }

    private fun removeOverlayView() {
        val v = overlayView ?: return
        overlayView = null
        try {
            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            wm.removeView(v)
        } catch (_: Exception) {}
    }

    override fun onInterrupt() {
        Log.w(TAG, "GuardianWatcherService interrupted")
    }
}
