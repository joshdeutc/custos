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

        private val INSTAGRAM_REDIRECT_IDS = listOf(
            "com.instagram.android:id/main_feed_action_bar",          // home feed
            "com.instagram.android:id/explore_action_bar",            // Explore grid
            "com.instagram.android:id/action_bar_search_edit_text",   // people search
            "com.instagram.android:id/clips_viewer_container",
            "com.instagram.android:id/clips_viewer_view_pager",
            "com.instagram.android:id/clips_expanded_touch_view",
            "com.instagram.android:id/root_clips_layout",
            "com.instagram.android:id/clips_swipe_refresh_container",
            "com.instagram.android:id/clips_video_container"
        )

        private val INSTAGRAM_PREFER_DEEPLINK_IDS = setOf(
            "com.instagram.android:id/explore_action_bar",
            "com.instagram.android:id/action_bar_search_edit_text"
        )

        private val INSTAGRAM_INBOX_IDS = listOf(
            "com.instagram.android:id/direct_inbox_action_bar",
            "com.instagram.android:id/direct_thread_header"
        )

        private const val INSTAGRAM_SCAN_INTERVAL_MS = 80L
        private const val INSTAGRAM_DIRECT_TAB_VIEW_ID = "com.instagram.android:id/direct_tab"

        private val INSTAGRAM_TAB_VIEW_IDS = listOf(
            "com.instagram.android:id/feed_tab",
            "com.instagram.android:id/clips_tab",
            INSTAGRAM_DIRECT_TAB_VIEW_ID,
            "com.instagram.android:id/search_tab",
            "com.instagram.android:id/profile_tab"
        )

        private val INSTAGRAM_REDIRECTED_TABS = setOf(
            "com.instagram.android:id/feed_tab",
            "com.instagram.android:id/clips_tab",
            "com.instagram.android:id/search_tab"
        )

        private const val INSTAGRAM_INBOX_URI = "https://www.instagram.com/direct/inbox/"
        private const val INSTAGRAM_REDIRECT_COOLDOWN_MS = 500L

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

    // F1/F3 remediation — Layer 1: the enforcement loop also runs here, in the immortal
    // accessibility service (process :guardian, survives `pm clear` of Custos UI), so dynamic
    // enforcement no longer depends on the UI binding GuardianService.
    private var guardianServiceBound = false
    private val selfServiceConnection = object : android.content.ServiceConnection {
        override fun onServiceConnected(name: android.content.ComponentName?, service: android.os.IBinder?) {
            guardianServiceBound = true
        }
        override fun onServiceDisconnected(name: android.content.ComponentName?) {
            guardianServiceBound = false
        }
    }

    private fun ensureGuardianServiceAlive() {
        runCatching {
            val intent = Intent(applicationContext, GuardianService::class.java)
            applicationContext.startService(intent)
            if (!guardianServiceBound) {
                guardianServiceBound = applicationContext.bindService(
                    intent,
                    selfServiceConnection,
                    Context.BIND_AUTO_CREATE
                )
            }
        }
    }

    private val heartbeatHandler = Handler(Looper.getMainLooper())
    private val heartbeatRunnable = object : Runnable {
        override fun run() {
            try {
                val pm = getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
                if (pm?.isInteractive == false) {
                    FallbackUsageTracker.onScreenOff(applicationContext, System.currentTimeMillis())
                }
                ensureGuardianServiceAlive()
                GuardianEngine.evaluateLimits(applicationContext)
                DelayManager.applyPendingDelayIfReady(applicationContext)
                DelayManager.applyPendingConfigsIfReady(applicationContext)
                ScreenRuleManager.applyPendingRemovalsIfReady(applicationContext)
            } catch (t: Throwable) {
                Log.e(TAG, "Watcher heartbeat error: ${t.message}")
            }
            heartbeatHandler.postDelayed(this, 1000L)
        }
    }

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
        // Start the independent enforcement loop + self-bind GuardianService + arm the alarm backstop (F1/F3).
        ensureGuardianServiceAlive()
        heartbeatHandler.removeCallbacks(heartbeatRunnable)
        heartbeatHandler.post(heartbeatRunnable)
        runCatching { GuardianEngine.scheduleHeartbeat(applicationContext) }
        Log.i(TAG, "GuardianWatcherService connected and active (ime=$imePackage)")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        isBound = false
        if (instance === this) instance = null
        applySessionOverlay(null)
        heartbeatHandler.removeCallbacks(heartbeatRunnable)
        // Best-effort self-heal if someone disabled the a11y service (e.g. via adb). The hard
        // guarantee is GuardianService (still running via startService) + the ACTION_HEARTBEAT alarm.
        runCatching { GuardianEngine.ensureAccessibilityServicesEnabled(applicationContext) }
        runCatching { GuardianEngine.scheduleHeartbeat(applicationContext) }
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        isBound = false
        if (instance === this) instance = null
        applySessionOverlay(null)
        heartbeatHandler.removeCallbacks(heartbeatRunnable)
        if (guardianServiceBound) {
            runCatching { applicationContext.unbindService(selfServiceConnection) }
            guardianServiceBound = false
        }
        runCatching { GuardianEngine.ensureAccessibilityServicesEnabled(applicationContext) }
        runCatching { GuardianEngine.scheduleHeartbeat(applicationContext) }
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
                    // F4: feed the accessibility-driven usage tracker for real foreground app transitions
                    // (including launcher, which closes any limited app's interval when going home, while
                    // ignoring keyboard/IME, SystemUI, and Guardian overlay popups).
                    FallbackUsageTracker.onForeground(applicationContext, pkg, System.currentTimeMillis())
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
        reloadScreenRulesIfStale(now)
        val rules = screenRules.filter { it.packageName == pkg && isRuleActiveNow(it) }
        if (rules.isEmpty()) return false

        if (pkg == INSTAGRAM_PACKAGE) {
            return enforceInstagramRestrictions(rules)
        }

        if (now - lastScreenRuleActionAt < SCREEN_RULE_REDIRECT_COOLDOWN_MS) return false
        if (now - lastScreenRuleScanAt < SCREEN_RULE_SCAN_INTERVAL_MS) return false
        lastScreenRuleScanAt = now

        for (rule in rules) {
            if (rule.allowedIds.isNotEmpty() && rule.allowedIds.any { visibleIdPresent(pkg, it) }) {
                continue
            }
            val hit = rule.blockedIds.firstOrNull { visibleIdPresent(pkg, it) } ?: continue

            lastScreenRuleActionAt = now
            performScreenEscape(rule, hit)
            return true
        }
        return false
    }

    /**
     * Instagram Partial Access enforcement (exact block from feat/me-flavor-and-screen-rules & 47fac43).
     * Redirects restricted Instagram surfaces (feed, Reels, Explore, search) to the Direct inbox.
     * BACK is deliberately not used — it only returns to the previous tab, which is normally the feed.
     */
    private fun enforceInstagramRestrictions(rules: List<ScreenRuleManager.ScreenRule>): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastInstagramBackTime < INSTAGRAM_REDIRECT_COOLDOWN_MS) return false
        if (now - lastInstagramScanTime < INSTAGRAM_SCAN_INTERVAL_MS) return false
        lastInstagramScanTime = now

        val root = rootInActiveWindow ?: return false
        val selectedTab = instagramSelectedTab(root)
        if (selectedTab == INSTAGRAM_DIRECT_TAB_VIEW_ID) return false
        if (INSTAGRAM_INBOX_IDS.any { hasViewId(root, it) }) return false

        var matchedRule: ScreenRuleManager.ScreenRule? = null
        var hitId: String? = null
        for (rule in rules) {
            if (rule.allowedIds.any { hasViewId(root, it) }) continue
            val m = when {
                selectedTab != null && selectedTab in rule.blockedIds -> selectedTab
                else -> rule.blockedIds.firstOrNull { it.contains(":id/") && hasViewId(root, it) }
            }
            if (m != null) {
                matchedRule = rule
                hitId = m
                break
            }
        }
        if (hitId == null) {
            val defaultHit = when (selectedTab) {
                in INSTAGRAM_REDIRECTED_TABS -> selectedTab
                else -> INSTAGRAM_REDIRECT_IDS.firstOrNull { hasViewId(root, it) }
            }
            if (defaultHit != null) {
                matchedRule = rules.first()
                hitId = defaultHit
            }
        }
        val hit = hitId ?: return false
        val rule = matchedRule ?: rules.first()

        lastInstagramBackTime = now
        val preferDeeplink = hit in INSTAGRAM_PREFER_DEEPLINK_IDS
        val how = when {
            rule.escapeTapId != null && tapNavItem(INSTAGRAM_PACKAGE, rule.escapeTapId, rule.escapeTapIndex) -> "tap"
            rule.escapeDeeplink != null && openDeeplink(INSTAGRAM_PACKAGE, rule.escapeDeeplink) -> "deeplink"
            preferDeeplink && openInstagramInbox() -> "deeplink"
            tapInstagramDirectTab() -> "tab"
            openInstagramInbox() -> "deeplink"
            else -> {
                goHome("instagram_fallback")
                "home"
            }
        }
        val shortHit = hit.substringAfter(":id/")
        Log.w(TAG, "Instagram restricted view ($shortHit) -> inbox via $how (rule=${rule.name})")
        EventLog.log(this, "IG_REDIRECT", "$shortHit -> $how")
        return true
    }

    private fun openInstagramInbox(): Boolean = openDeeplink(INSTAGRAM_PACKAGE, INSTAGRAM_INBOX_URI)

    private fun hasViewId(root: AccessibilityNodeInfo, viewId: String): Boolean = try {
        root.findAccessibilityNodeInfosByViewId(viewId).any { it.isVisibleToUser }
    } catch (_: Exception) {
        false
    }

    private fun instagramSelectedTab(root: AccessibilityNodeInfo): String? {
        for (id in INSTAGRAM_TAB_VIEW_IDS) {
            val selected = try {
                root.findAccessibilityNodeInfosByViewId(id)
                    .any { it.isVisibleToUser && (it.isSelected || it.isChecked) }
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
                val found = root.findAccessibilityNodeInfosByViewId(viewId)
                    .firstOrNull { it.isVisibleToUser }
                if (found != null) return found
            }
        } catch (_: Exception) {}
        return try {
            rootInActiveWindow
                ?.findAccessibilityNodeInfosByViewId(viewId)
                ?.firstOrNull { it.isVisibleToUser }
        } catch (_: Exception) {
            null
        }
    }

    private fun tapInstagramDirectTab(): Boolean = try {
        val icon = findNodeInAnyWindow(INSTAGRAM_DIRECT_TAB_VIEW_ID)
        if (icon == null) {
            false
        } else {
            val bounds = Rect().also { icon.getBoundsInScreen(it) }
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
        val isText = viewId.startsWith("text:")
        val isDesc = viewId.startsWith("desc:")
        val query = when {
            isText -> viewId.removePrefix("text:")
            isDesc -> viewId.removePrefix("desc:")
            else -> ""
        }
        fun matchesIn(root: AccessibilityNodeInfo): Boolean {
            if (root.packageName?.toString() != pkg) return false
            return if (isText || isDesc) {
                root.findAccessibilityNodeInfosByText(query).any {
                    it.isVisibleToUser && (
                        normalizeText(it.text).equals(query, ignoreCase = true) ||
                        normalizeText(it.contentDescription).equals(query, ignoreCase = true)
                    )
                }
            } else {
                root.findAccessibilityNodeInfosByViewId(viewId).any { it.isVisibleToUser }
            }
        }
        try {
            var checkedPkgWindow = false
            for (window in windows) {
                val root = window.root ?: continue
                if (root.packageName?.toString() != pkg) continue
                checkedPkgWindow = true
                if (matchesIn(root)) return true
            }
            if (checkedPkgWindow) return false
        } catch (_: Exception) {}
        return try {
            val root = rootInActiveWindow ?: return false
            matchesIn(root)
        } catch (_: Exception) {
            false
        }
    }

    private fun performScreenEscape(rule: ScreenRuleManager.ScreenRule, hit: String) {
        val pkg = rule.packageName
        val escapeTapId = rule.escapeTapId ?: if (pkg == "com.snapchat.android") {
            "com.snapchat.android:id/ngs_chat_icon_container"
        } else null

        val how = when {
            escapeTapId != null && tapNavItem(pkg, escapeTapId, rule.escapeTapIndex) -> "tap"
            rule.escapeDeeplink != null && openDeeplink(pkg, rule.escapeDeeplink) -> "deeplink"
            pkg == "com.snapchat.android" && tapSnapchatChat() -> "snapchat_chat_coords"
            else -> {
                goHome("screen_escape_fallback")
                "home"
            }
        }
        val shortHit = hit.substringAfter(":id/")
        Log.w(TAG, "${rule.name}: $shortHit -> escape ($how)")

        overlayHandler.postDelayed({
            val stillBlocked = rule.blockedIds.any { visibleIdPresent(pkg, it) }
            val reached = !stillBlocked && (
                rule.allowedIds.isEmpty() || rule.allowedIds.any { visibleIdPresent(pkg, it) }
            )
            ScreenRuleManager.recordOutcome(this, rule.name, reached)
            EventLog.log(this, "SCREEN_RULE", "${rule.name} $shortHit -> $how, reached=$reached")
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
        val isText = viewId.startsWith("text:")
        val isDesc = viewId.startsWith("desc:")
        val query = when {
            isText -> viewId.removePrefix("text:")
            isDesc -> viewId.removePrefix("desc:")
            else -> ""
        }
        fun collectFrom(root: AccessibilityNodeInfo) {
            if (isText || isDesc) {
                for (n in root.findAccessibilityNodeInfosByText(query)) {
                    if (n.isVisibleToUser && (
                        normalizeText(n.text).equals(query, ignoreCase = true) ||
                        normalizeText(n.contentDescription).equals(query, ignoreCase = true)
                    )) matches += n
                }
            } else {
                for (n in root.findAccessibilityNodeInfosByViewId(viewId)) {
                    if (n.isVisibleToUser) matches += n
                }
            }
        }
        try {
            for (window in windows) {
                val root = window.root ?: continue
                if (root.packageName?.toString() != pkg) continue
                collectFrom(root)
            }
            if (matches.isEmpty()) {
                val root = rootInActiveWindow
                if (root != null && root.packageName?.toString() == pkg) {
                    collectFrom(root)
                }
            }
        } catch (_: Exception) {
            return false
        }
        val node = matches.getOrNull(index) ?: matches.firstOrNull() ?: return false
        val bounds = Rect().also { node.getBoundsInScreen(it) }
        if (bounds.isEmpty) return false
        return try {
            val path = Path().apply { moveTo(bounds.exactCenterX(), bounds.exactCenterY()) }
            dispatchGesture(
                GestureDescription.Builder()
                    .addStroke(GestureDescription.StrokeDescription(path, 0L, 10L))
                    .build(),
                null, null
            )
        } catch (_: Exception) {
            false
        }
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
