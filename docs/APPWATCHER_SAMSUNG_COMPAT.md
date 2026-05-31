# AppWatcherService — Samsung OneUI compatibility

## Initial problem

On **Samsung Android 13 / OneUI 5** (SM-T970), the protection against disabling accessibility didn't
trigger, whereas it worked correctly on **Samsung Android 15 / OneUI 7** (SM-A536B).

## Root cause

`AppWatcherService` protects the app by intercepting `AccessibilityEvent`s when the user opens the
accessibility screen in Settings, then sends the user back home (`goHome()`) if the app is visible
in the list.

The service filters events by **package name**. Samsung changed the layout between OneUI versions:

| Version | Accessibility settings package | Class |
|---------|-------------------------------|-------|
| OneUI 7 / Android 15 | `com.android.settings` | `Settings$AccessibilitySettingsActivity` |
| OneUI 5 / Android 13 | `com.samsung.accessibility` | `AccessibilityHomepageActivity` |

On OneUI 5, Samsung pulled accessibility out of the standard Settings package and put it in **its own
dedicated package** `com.samsung.accessibility`. The initial code only filtered `com.android.settings`
→ events coming from `com.samsung.accessibility` were silently ignored → no protection triggered.

## Second bug: requireDangerKeyword

On the Samsung accessibility page (all versions), the ON/OFF toggle is a plain switch with no
"Disable" text. The initial logic passed `requireDangerKeyword = true`, so it looked for those
keywords in the visible nodes, didn't find them, and gave up.

Fix: the accessibility page passes `requireDangerKeyword = false`. The mere fact that our app is
visible in the list is enough to trigger the protection.

## Applied fix

In `AppWatcherService.kt`:

```kotlin
// SETTINGS_PACKAGES — packages that host the settings pages
private val SETTINGS_PACKAGES = setOf(
    "com.android.settings",
    "com.samsung.android.app.routines",
    "com.samsung.android.sm",
    "com.samsung.accessibility"      // OneUI 5 / Android 13: dedicated package
)

// Accessibility page detection — covers AOSP and OneUI 5
val isAccessibilityPage =
    className.contains("AccessibilitySettings")
    || className.contains("ToggleAccessibilityService")
    || className.contains("InstalledAccessibilityService")
    || className.contains("AccessibilityHomepageActivity")  // OneUI 5
    || packageName == "com.samsung.accessibility"           // OneUI 5 fallback

if (isAccessibilityPage && packageName in SETTINGS_PACKAGES) {
    checkSettingsForSelfControl(requireDangerKeyword = false)  // no keyword required
}
```

## Final behavior

| Situation | Trigger |
|-----------|---------|
| Settings locked + user opens accessibility (AOSP or Samsung) | immediate `goHome()` |
| Settings unlocked | bypass via `DelayManager.isSettingsUnlocked()` |
| User tries to disable via the Samsung toggle switch | `goHome()` (no keyword required) |
| User opens app details → Uninstall button | `goHome()` ("Uninstall" keyword found) |
