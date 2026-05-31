# Configuration — `limits.json`

All rules are stored in a `limits.json` file kept in the app's private internal storage
(`filesDir`). **In normal use you don't edit this file by hand** — the app's configuration screen
reads and writes it for you. This document describes the format for those who want to understand it
or prepare a config (see [`../examples/limits.example.json`](../examples/limits.example.json)).

## Overall structure

```json
{
  "limits": [ /* per-application rules */ ],
  "period_blocks": [ /* forbidden time windows (curfews) */ ]
}
```

## `limits[]` — per-application rules

```json
{
  "package": "com.instagram.android",
  "max_minutes_per_day": 30,
  "allowed_days": "*",
  "allowed_hours": "*",
  "session": { ... }
}
```

| Field | Type | Description |
|---|---|---|
| `package` | string | The app's package name (e.g. `com.instagram.android`). |
| `max_minutes_per_day` | int | Daily quota in minutes. `0` = app fully blocked. |
| `max_seconds_per_day` | int *(optional)* | Quota in seconds; takes priority over minutes when present. |
| `allowed_days` | `"*"` or `[int]` | Allowed days. `"*"` = all. Otherwise a list where **0 = Sunday, 1 = Monday, … 6 = Saturday**. |
| `allowed_hours` | `"*"` or `"HH:mm-HH:mm"` | Allowed time window. `"*"` = all day. |
| `session` | object *(optional)* | Per-opening session limit (see below). |

Outside the allowed days/hours, or once the quota is reached, the app is blocked until the next
allowed window (or until the 2:00 AM daily reset).

### `session` block (Discord-style)

```json
"session": {
  "session_duration_sec": 300,
  "cooldown_sec": 14400,
  "max_sessions_per_day": 3
}
```

| Field | Description |
|---|---|
| `session_duration_sec` | Maximum duration of one opening (here 5 min). |
| `cooldown_sec` | Enforced wait after a session ends (here 4 h). |
| `max_sessions_per_day` | Number of openings allowed per day. |

Detailed semantics (what ends a session, etc.): [`SESSION_LIMIT.md`](SESSION_LIMIT.md).

## `period_blocks[]` — curfews

Time windows during which a set of apps is blocked **with priority over the quota**.

```json
{
  "packages": ["com.google.android.youtube", "com.zhiliaoapp.musically"],
  "blocked_hours": "22:00-07:00",
  "blocked_days": "*",
  "mute_notifications": true
}
```

| Field | Description |
|---|---|
| `packages` | The apps affected by this window. |
| `blocked_hours` | Forbidden window `"HH:mm-HH:mm"`. If the end time is before the start time, the window crosses midnight (e.g. `22:00-07:00`). |
| `blocked_days` | `"*"` or a list of days (0 = Sunday … 6 = Saturday). |
| `mute_notifications` | `true` → also mutes the apps' notifications during the window. |

## Anti-cheat delay

Any change that **loosens** a rule (raising a quota, deleting a limit, widening a session) only
applies after a configurable delay. Changes that **tighten** are immediate. This prevents disabling
your own limits on a whim. See the corresponding section in
[`ARCHITECTURE.md`](ARCHITECTURE.md).
