# Custos Notification Blocking & Snooze Testing Suite

This directory contains the testing infrastructure to verify that Custos correctly snoozes, intercepts, and restores notifications when an application is blocked.

## Architecture

1. **`notif-simulator/`**: A standalone Android application module (`com.custos.testnotif`) included in the project.
   - Designed specifically to emit notifications on demand via **ADB broadcasts** or **UI actions** (Maestro).
   - Supports immediate posts, custom titles/texts/IDs, high-priority heads-up alerts, delayed background execution (to test background notifications while the app is blocked/suspended), and clearing notifications.
   - Exported receiver: `com.custos.testnotif.NotificationTriggerReceiver`.

2. **`tests/test_notifications_adb.ps1`**: Automated end-to-end verification script runnable by developers or autonomous agents.
   - Step 1: Whitelist ensuring `com.custos.testnotif` is exempt from initial quarantine delay.
   - Step 2: Baseline test: Post notification unblocked -> Assert active in `dumpsys notification`.
   - Step 3: Block test: Apply 0s quota limit + mute rule via `TEST_SET_APP_LIMIT`.
   - Step 4: Snooze verification: Post notification while app is blocked -> Assert intercepted by `SelfControlNotificationListener`, moved to `Snoozed notifications`, and hidden from the notification shade.
   - Step 5: Unblock & recovery: Clear quota limit via `TEST_CLEAR_APP_LIMIT` -> Assert notification is unsnoozed and restored to the active shade.

3. **`tests/test_notifications_maestro.yaml`**: Maestro UI test flow to verify UI controls, permissions, and background delayed notifications.

---

## Quick Start

### 1. Build and Install Simulator
```powershell
.\gradlew :notif-simulator:assembleDebug
adb install -r notif-simulator\build\outputs\apk\debug\notif-simulator-debug.apk
adb shell pm grant com.custos.testnotif android.permission.POST_NOTIFICATIONS
```

### 2. Run Automated ADB E2E Test
```powershell
powershell.exe -ExecutionPolicy Bypass -File .\tests\test_notifications_adb.ps1 -DeviceId "RZCT30L9EGJ"
```

### 3. Run Maestro Flow
```bash
maestro test tests/test_notifications_maestro.yaml
```

---

## ADB Broadcast Reference

### Send Notification Immediately
```bash
adb shell am broadcast -p com.custos.testnotif -a com.custos.testnotif.POST \
  --es title "My Alert" \
  --es text "Notification message body" \
  --ei id 1001 \
  --ez high_priority true
```

### Send Notification with Background Delay (e.g. 5 seconds)
```bash
adb shell am broadcast -p com.custos.testnotif -a com.custos.testnotif.POST \
  --es title "Delayed Alert" \
  --es text "Arrived in background" \
  --ei delay_seconds 5
```

### Clear Notifications
```bash
# Clear all
adb shell am broadcast -p com.custos.testnotif -a com.custos.testnotif.CLEAR

# Clear specific ID
adb shell am broadcast -p com.custos.testnotif -a com.custos.testnotif.CLEAR --ei id 1001
```

### Custos Automation Hooks (Debug Builds)
```bash
# Block app immediately and enable notification muting
adb shell am broadcast -p com.jo.selfcontrol.ultimate -a com.jo.selfcontrol.ultimate.TEST_SET_APP_LIMIT \
  --es pkg "com.custos.testnotif" --ei max_seconds 0

# Unblock app and release snoozed notifications
adb shell am broadcast -p com.jo.selfcontrol.ultimate -a com.jo.selfcontrol.ultimate.TEST_CLEAR_APP_LIMIT \
  --es pkg "com.custos.testnotif"
```
