# Architecture

SelfControl is an Android screen-time app built **entirely on an AccessibilityService** — no root,
no Device Owner, no special privileges. It installs and uninstalls like any other app.

The idea: a service continuously watches the foreground application, and the moment an app breaks a
rule you defined, the user is immediately sent back to the home screen.

## The two components

### 1. `AppWatcherService` — the AccessibilityService

This is the foundation of the app. By listening to the system's accessibility events, it always
knows which application is in the foreground (`currentForegroundApp`).

Its responsibilities:

- **Foreground detection** — feeds the decision engine.
- **Return to home ("HOME spam")** — when an app is in the `blockedApps` set, the service forces an
  immediate return to home every time that app comes back to the foreground. This is the actual
  blocking mechanism.
- **Settings protection** — intercepts the opening of sensitive pages (the Accessibility page, the
  app's own details page) to discourage disabling the service. Samsung OneUI 5 / OneUI 7
  compatibility is detailed in [`APPWATCHER_SAMSUNG_COMPAT.md`](APPWATCHER_SAMSUNG_COMPAT.md).

### 2. `LimitService` — the enforcement engine (foreground service)

A foreground service that runs continuously and, **every second**, evaluates the rules and updates
the `AppWatcherService.blockedApps` set. It also handles usage tracking, persistence and
notifications.

The two services complement each other: `AppWatcherService` observes and acts (HOME), while
`LimitService` decides (which apps must be blocked right now).

## Enforcement rules

`LimitService` combines several rule types, evaluated in priority order:

1. **Curfews / period blocks** — forbidden time windows for a set of apps, taking priority over the
   quota. They can also mute the notifications of the affected apps
   (`SelfControlNotificationListener`).
2. **Daily quota** (`max_minutes_per_day` / `max_seconds_per_day`) — cumulative foreground time per
   app, measured via `UsageStatsManager`. The logical day resets at **2:00 AM**.
3. **Allowed days / hours** — restriction by day of week and time window.
4. **Session limit** (Discord-style) — duration per opening + cooldown + number of sessions per
   day. Lowest priority. Details in [`SESSION_LIMIT.md`](SESSION_LIMIT.md).
5. **Nuclear Mode** — reinforced temporary block of a set of apps for a given duration, with named
   presets. Details in [`NUCLEAR_PRESETS.md`](NUCLEAR_PRESETS.md).

All of this is configured in `limits.json` — see [`CONFIGURATION.md`](CONFIGURATION.md).

## Usage measurement

`UsageStatsManager` (`UsageQuery.kt`) provides foreground time per package. On some devices
(Samsung Android 15 in particular) `queryAndAggregateUsageStats` returns unreliable values, so the
engine relies on `queryEvents` + a manual sum, complemented by an in-memory counter incremented
every second while the watched app is in the foreground.

## Restart robustness

- `BootReceiver` restarts `LimitService` when the device boots.
- `WatchdogReceiver` schedules alarms to restart the service if it gets killed.
- State (today's usage, blocked apps, logical-day boundary) is persisted to disk via
  `UsageStateStore` (key: `logicalDayStartMs`), which lets the app resume exactly where it left off
  after a reboot or kill — without starting from scratch or losing quota progress.

## The anti-cheat delay (`DelayManager`)

Loosening a limit (raising a quota, deleting a rule) is intentionally subject to a **delay**: the
change only takes effect after a cooldown, to prevent circumventing your own rules in a moment of
weakness. Tightening a limit, on the other hand, is immediate. The "loosening vs tightening"
detection lives in `ConfigManager` (`shouldDeferLimitsChange`, `shouldDeferPeriodBlocksChange`).

## Intrinsic limitation

Without system privileges, blocking relies on the AccessibilityService. A determined user can work
around it (disable accessibility in Settings, or uninstall the app after a reboot before the service
restarts). It's a **self-discipline tool**, not a tamper-proof lock: it adds just enough friction to
break the "open the app by reflex" habit.

## File map

| File | Role |
|---|---|
| `AppWatcherService.kt` | AccessibilityService: foreground detection + HOME spam + Settings protection |
| `LimitService.kt` | Foreground service: enforcement engine (1 s tick), usage, persistence, notifications |
| `ConfigManager.kt` | Reads/writes `limits.json` + delay logic |
| `MainActivity.kt` | UI: permissions, dashboard, limit editing, Nuclear Mode |
| `SessionManager.kt` | Per-opening sessions (duration / cooldown / quota per day) |
| `NuclearManager.kt` / `NuclearPresetsManager.kt` | Nuclear Mode + presets |
| `UsageQuery.kt` | `UsageStatsManager` queries |
| `UsageStateStore.kt` | Daily-state persistence |
| `DelayManager.kt` | Anti-cheat delay on loosenings |
| `BlockedNotificationManager.kt` / `SelfControlNotificationListener.kt` | Muting notifications of blocked apps |
| `BootReceiver.kt` / `WatchdogReceiver.kt` / `DailyResetReceiver.kt` | Restart on boot, watchdog, daily reset |
| `PermissionHelper.kt` | Checking/requesting permissions (accessibility, usage stats, notifications) |

## Package

`com.jo.selfcontrol.ultimate`
