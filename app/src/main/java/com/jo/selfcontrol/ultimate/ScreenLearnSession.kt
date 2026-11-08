package com.jo.selfcontrol.ultimate

import android.content.Context
import android.util.Log

/**
 * The guided conversation that teaches Custos which part of an app to close.
 *
 * The Instagram rule was written the other way round: I read view ids over adb and guessed which ones
 * meant "the feed". Five of those guesses were wrong, and not one of the mistakes needed a developer
 * to fix — each needed somebody looking at the screen. So the roles are swapped here. Custos asks, the
 * person answers by navigating, and [ScreenRuleManager.deriveMarkers] does the part a human eye is bad
 * at: noticing that an id is present on both screens and therefore useless.
 *
 * The flow, driven entirely from the floating overlay so the user never leaves the app being learned:
 *
 *  1. [Step.AWAIT_TARGET]  — "open the screen to block", capture its visible ids
 *  2. [Step.AWAIT_SCROLL]  — "now scroll", capture again and keep only ids that survived
 *  3. [Step.AWAIT_ALLOWED] — "open the screen to be sent back to", capture ids + the selected tab
 *  4. [Step.CONFIRM]       — markers derived; the rule is saved and announced
 *
 * Step 2 exists because of a specific failure: the Instagram feed rule keyed on `main_feed_action_bar`,
 * which collapses out of sight as soon as the feed is scrolled. The block worked until the user swiped,
 * then silently stopped. Intersecting the at-rest and scrolled captures removes that whole class of
 * marker before it can be chosen.
 */
object ScreenLearnSession {

    private const val TAG = "SelfControl.Learn"

    /** Gap between the two readings of a capture. Long enough for a tab transition to finish. */
    private const val STABILISE_MS = 700L

    enum class Step { IDLE, AWAIT_TARGET, AWAIT_SCROLL, AWAIT_ALLOWED, DONE }

    @Volatile
    private var step: Step = Step.IDLE

    private val backgroundExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    private var packageName: String = ""
    private var ruleName: String = ""
    private var targetIds: Set<String> = emptySet()
    private val allowedSets = mutableListOf<Set<String>>()
    private var escapeTapId: String? = null
    private var escapeTapIndex: Int = 0
    private var protectionDelaySec: Int = 0
    private var blockedHours: String? = null

    val isActive: Boolean get() = step != Step.IDLE && step != Step.DONE

    // ──────────────────────────────────────
    //  Flow
    // ──────────────────────────────────────

    fun start(
        svc: AppWatcherService,
        pkg: String,
        name: String,
        protectionDelaySec: Int = 0,
        blockedHours: String? = null
    ) {
        packageName = pkg
        ruleName = name
        this.protectionDelaySec = protectionDelaySec
        this.blockedHours = blockedHours
        targetIds = emptySet()
        allowedSets.clear()
        escapeTapId = null
        escapeTapIndex = 0
        step = Step.AWAIT_TARGET
        Log.w(TAG, "Learning started for $pkg as '$name'")
        EventLog.log(svc, "LEARN", "started pkg=$pkg name=$name")
        render(svc)
    }

    fun cancel(svc: AppWatcherService) {
        step = Step.IDLE
        svc.hideLearnOverlay()
        Log.w(TAG, "Learning cancelled")
        EventLog.log(svc, "LEARN", "cancelled")
    }

    /**
     * Read the screen twice, [STABILISE_MS] apart, and keep only what appeared in both.
     * Runs completely on a background thread to prevent accessibility looper freezes.
     */
    private fun captureStable(svc: AppWatcherService, then: (Set<String>) -> Unit) {
        backgroundExecutor.execute {
            val first = svc.visibleIdsFor(packageName)
            Log.i(TAG, "captureStable: first reading = ${first.size} ids")
            try {
                Thread.sleep(STABILISE_MS)
            } catch (_: InterruptedException) {}
            val second = svc.visibleIdsFor(packageName)
            Log.i(TAG, "captureStable: second reading = ${second.size} ids")
            val common = first intersect second
            Log.i(TAG, "captureStable: common = ${common.size} ids")
            val finalIds = if (common.isNotEmpty()) common else second.ifEmpty { first }
            mainHandler.post {
                then(finalIds)
            }
        }
    }

    /** The overlay's primary button. Each press captures the current screen and advances. */
    fun onPrimary(svc: AppWatcherService) {
        when (step) {
            Step.AWAIT_TARGET -> {
                toast(svc, "Reading target screen...")
                captureStable(svc) { ids ->
                    if (ids.isEmpty()) {
                        toast(svc, "Nothing stable read here. Stay on the screen and retry.")
                    } else {
                        targetIds = ids
                        Log.i(TAG, "Target captured: ${ids.size} stable id(s)")
                        step = Step.AWAIT_SCROLL
                        render(svc)
                    }
                }
            }

            Step.AWAIT_SCROLL -> {
                toast(svc, "Checking scrolled screen...")
                captureStable(svc) { scrolled ->
                    onScrollCaptured(svc, scrolled)
                }
            }

            Step.AWAIT_ALLOWED -> {
                toast(svc, "Saving rule, analyzing screens...")
                captureStable(svc) { ids ->
                    if (ids.isEmpty()) {
                        toast(svc, "Nothing stable read here.")
                    } else {
                        allowedSets.add(ids)
                        backgroundExecutor.execute {
                            svc.selectedNavItem(packageName)?.let { (id, index) ->
                                escapeTapId = id
                                escapeTapIndex = index
                                Log.i(TAG, "Escape target: $id [#$index]")
                            }
                            finishAsync(svc)
                        }
                    }
                }
            }

            else -> Unit
        }
    }

    /**
     * Keep only the markers that were still visible after scrolling.
     *
     * Refusing outright when nothing survives is deliberate. The Instagram feed rule keyed on an action
     * bar that collapses on scroll: it worked until the user swiped, then stopped without a word. A rule
     * that cannot survive a scroll is worse than no rule, because it looks like protection.
     */
    private fun onScrollCaptured(svc: AppWatcherService, scrolled: Set<String>) {
        val survived = targetIds intersect scrolled
        if (survived.isEmpty()) {
            toast(svc, "No marker survives scrolling on this screen.")
            step = Step.IDLE
            svc.hideLearnOverlay()
            EventLog.log(svc, "LEARN", "failed: no marker survived scrolling")
            return
        }
        targetIds = survived
        Log.i(TAG, "After scroll: ${survived.size} id(s) survived")
        step = Step.AWAIT_ALLOWED
        render(svc)
    }

    private fun finishAsync(svc: AppWatcherService) {
        val markers = ScreenRuleManager.deriveMarkers(targetIds, allowedSets)
        if (markers.isEmpty()) {
            mainHandler.post {
                step = Step.IDLE
                svc.hideLearnOverlay()
                toast(svc, "These two screens are indistinguishable — no rule possible.")
                EventLog.log(svc, "LEARN", "failed: no discriminating marker")
            }
            return
        }

        // Keep a handful: one is enough to fire, several give resilience when the app renames one.
        val chosen = markers.take(4)

        // The destination markers need the *same* differential, in reverse.
        val allowedMarkers = ScreenRuleManager
            .deriveMarkers(allowedSets.flatten().toSet(), listOf(targetIds))
            .filter { ScreenRuleManager.specificity(it) > 0 }
            .take(4)

        val rule = ScreenRuleManager.ScreenRule(
            name = ruleName,
            packageName = packageName,
            blockedIds = chosen,
            allowedIds = allowedMarkers,
            escapeTapId = escapeTapId,
            escapeTapIndex = escapeTapIndex,
            protectionDelaySec = protectionDelaySec.takeIf { it > 0 },
            blockedHours = blockedHours?.takeIf { it.isNotBlank() && it != "*" }
        )
        val saved = ScreenRuleManager.upsert(svc, rule)
        mainHandler.post {
            step = Step.DONE
            svc.hideLearnOverlay()
            val shortMarkers = chosen.joinToString(", ") { it.substringAfter(":id/") }
            toast(svc, if (saved) "Rule saved: $shortMarkers" else "Save failed")
            Log.w(TAG, "Learning finished: $shortMarkers")
        }
    }

    // ──────────────────────────────────────
    //  Presentation
    // ──────────────────────────────────────

    private fun render(svc: AppWatcherService) {
        val (text, button) = when (step) {
            Step.AWAIT_TARGET ->
                "Open the screen you want to block,\nthen tap Capture." to "Capture"
            Step.AWAIT_SCROLL ->
                "Scroll this screen a little,\nthen tap Check." to "Check"
            Step.AWAIT_ALLOWED ->
                "Go to the screen you want to be sent back to,\nthen tap Finish." to "Finish"
            else -> return
        }
        svc.showLearnOverlay(
            text = text,
            primaryLabel = button,
            onPrimary = { onPrimary(svc) },
            onCancel = { cancel(svc) }
        )
    }

    private fun toast(ctx: Context, msg: String) {
        Log.w(TAG, msg)
        mainHandler.post {
            runCatching {
                android.widget.Toast.makeText(ctx.applicationContext, msg, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}
