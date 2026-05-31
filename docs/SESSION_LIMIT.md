# Session limit (Discord-style, per-unlock)

Inspired by Discord's per-unlock limit. Lower priority than curfews and the daily quota: if either
of those blocks, the session is not evaluated.

## Semantics

- **Opening the app** (it becomes foreground) while no session is active AND not in cooldown AND
  `sessionsUsed < maxSessionsPerDay` → starts a session: `sessionStartMs = now`, `sessionsUsed += 1`.
- **Returning to home** (the foreground becomes the launcher) → session ends *immediately*, and the
  cooldown starts from the moment you left. ⚠️ This is the **only** "leave" trigger: briefly
  switching to something else *within* the app's own flow (a dialog, webview/custom-tab, share
  sheet, keyboard, system UI, the window swap while sending a message) does **not** close the
  session. Switching directly to another app without going home leaves the session active — the
  `session_duration_sec` timer then expires it on its own.
- **Reaching `sessionDurationSec`** without having left → session ends, cooldown starts from the end
  of the session.
- **In cooldown** or **`sessionsUsed >= max`** → app blocked (`reason="session_cooldown"` or
  `"session_daily"`).
- **Daily rollover** (logical day = 2 AM) → `sessionsUsed = 0`, cooldown reset.

⚠️ **Returning to home = consuming a whole session.** If you open the app, stay 10 s, then go back
home, you've used 1 session out of N and started your cooldown. This is intentional (user spec).

### Why "home" and not "any foreground change"

Originally a session closed as soon as `currentForegroundApp` was no longer the target package. The
problem: during the app's internal flow (e.g. sending a message) an intermediate window — dialog,
webview/custom-tab, share sheet, keyboard, system UI, the window swap while sending — briefly became
foreground and killed the session instantly. So the "leave" trigger now fires **only** when the
foreground becomes the launcher. `LimitService.endSessionsForLeftPackages` closes sessions only if
`currentApp ∈ launcherPackages()` (resolved via `ACTION_MAIN`/`CATEGORY_HOME`, cached).

## Configuration

Optional per package in `limits.json`. Format:

```json
{
  "limits": [
    {
      "package": "com.discord",
      "max_minutes_per_day": 30,
      "allowed_days": "*",
      "allowed_hours": "*",
      "session": {
        "session_duration_sec": 300,
        "cooldown_sec": 14400,
        "max_sessions_per_day": 3
      }
    }
  ]
}
```

- `session_duration_sec`: max duration of one session (e.g. 300 = 5 min)
- `cooldown_sec`: wait after a session ends (e.g. 14400 = 4 h)
- `max_sessions_per_day`: number of sessions allowed before being blocked until the next day (e.g. 3)

Without a `session` block → the limit is disabled for that package (same behavior as before the
feature existed).

## Persisted state

File `<filesDir>/session_state.json`. Survives reboot / kill / reinstall (until the next factory
reset / clear data).

```json
{
  "tracking_day": 138,
  "packages": {
    "com.discord": {
      "session_start_ms": 1715000000000,
      "cooldown_end_ms": 0,
      "sessions_used": 1
    }
  }
}
```

`session_start_ms = 0` → no active session. `cooldown_end_ms = 0` → no cooldown.

## LimitService integration

In `enforceLimit()` (every 1 s):

1. Day rollover → `SessionManager.resetDayIfNeeded()`
2. `endSessionsForLeftPackages(currentApp, now)` — **only if `currentApp` is the launcher**: any
   active session is closed → cooldown
3. Curfew check (if blocking → return)
4. Day/hour/quota check (if blocking → return)
5. **`SessionManager.evaluate(currentApp, session, now)`** — starts or continues/blocks the session

`shouldStayBlockedForNonCurfewReasons()` also consults `SessionManager.shouldStayBlocked()` so that
`checkAndUnblockApps()` doesn't yo-yo between suspend/unsuspend during a cooldown.

## TODO (not done here)

- UI in `MainActivity` to configure the `session` block per app (currently you must edit
  `limits.json` by hand)
- A "X seconds left in this session" notification while a session is active
- Integration tests via `TestAutomationReceiver` (broadcasts to simulate open/leave)
