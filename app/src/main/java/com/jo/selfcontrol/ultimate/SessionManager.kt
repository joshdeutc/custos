package com.jo.selfcontrol.ultimate

import android.content.Context
import org.json.JSONObject

/**
 * Read-only UI adapter for Guardian-owned session limits (ConfigManager.SessionConfig).
 * All session tracking, cooldown enforcement, and floating overlays live exclusively in Guardian.
 */
object SessionManager {

    data class Status(
        val state: State,
        val sessionsUsed: Int,
        val maxSessionsPerDay: Int,
        val sessionRemainingSec: Int,
        val cooldownEndsAtMs: Long
    ) {
        enum class State { ACTIVE, COOLDOWN, READY, DAILY_EXHAUSTED }
    }

    fun statusOf(ctx: Context, pkg: String, cfg: ConfigManager.SessionConfig, nowMs: Long): Status {
        val rawJson = GuardianClient.getSessionStateJson()
        var sessionStartMs = 0L
        var cooldownEndMs = 0L
        var sessionsUsed = 0

        if (!rawJson.isNullOrBlank() && rawJson != "{}") {
            try {
                val root = JSONObject(rawJson)
                val o = root.optJSONObject("packages")?.optJSONObject(pkg)
                if (o != null) {
                    sessionStartMs = o.optLong("session_start_ms", 0L)
                    cooldownEndMs = o.optLong("cooldown_end_ms", 0L)
                    sessionsUsed = o.optInt("sessions_used", 0)
                }
            } catch (_: Exception) {}
        }

        return when {
            sessionStartMs > 0L -> {
                val endsAt = sessionStartMs + cfg.sessionDurationSec * 1000L
                val leftSec = ((endsAt - nowMs) / 1000L).coerceAtLeast(0L).toInt()
                Status(Status.State.ACTIVE, sessionsUsed, cfg.maxSessionsPerDay, leftSec, 0L)
            }
            nowMs < cooldownEndMs ->
                Status(Status.State.COOLDOWN, sessionsUsed, cfg.maxSessionsPerDay, 0, cooldownEndMs)
            sessionsUsed >= cfg.maxSessionsPerDay ->
                Status(Status.State.DAILY_EXHAUSTED, sessionsUsed, cfg.maxSessionsPerDay, 0, 0L)
            else ->
                Status(Status.State.READY, sessionsUsed, cfg.maxSessionsPerDay, 0, 0L)
        }
    }
}
