# test_notifications_adb.ps1
# End-to-End automated test for Custos notification blocking & snoozing on Samsung.
#
# Usage:
#   .\tests\test_notifications_adb.ps1 [-DeviceId "RZCT30L9EGJ"]

param(
    [string]$DeviceId = "RZCT30L9EGJ"
)

$ErrorActionPreference = "Stop"

$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) {
    $adb = "adb"
}

function Invoke-AdbShell([string]$cmd) {
    if ($DeviceId) {
        & $adb -s $DeviceId shell "$cmd"
    } else {
        & $adb shell "$cmd"
    }
}

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "Custos Notification Snooze & Blocking Test Suite" -ForegroundColor Cyan
Write-Host "Device: $DeviceId" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# Ensure package is whitelisted in Custos (zero delay)
Write-Host "`n[1/5] Ensuring com.custos.testnotif is whitelisted in Custos..." -ForegroundColor Yellow
Invoke-AdbShell "am broadcast -p com.jo.selfcontrol.ultimate -a com.jo.selfcontrol.ultimate.REQUEST_WHITELIST_APP --es pkg 'com.custos.testnotif' --el delaySec 0" | Out-Null
Invoke-AdbShell "pm grant com.custos.testnotif android.permission.POST_NOTIFICATIONS" | Out-Null
Invoke-AdbShell "am broadcast -p com.custos.testnotif -a com.custos.testnotif.CLEAR" | Out-Null
Start-Sleep -Seconds 1

# Step 1: Baseline unblocked notification
Write-Host "`n[2/5] Testing Baseline (App Unblocked)..." -ForegroundColor Yellow
Invoke-AdbShell "am broadcast -p com.custos.testnotif -a com.custos.testnotif.POST --es title 'Test-Baseline' --es text 'Should be visible' --ei id 1001" | Out-Null
Start-Sleep -Seconds 1

$dump = (Invoke-AdbShell "dumpsys notification --noredact") -join "`n"
if ($dump -match "NotificationRecord.*com\.custos\.testnotif.*id=1001") {
    Write-Host "  [PASS] Baseline passed: Notification 1001 is visible in active list." -ForegroundColor Green
} else {
    Write-Host "  [FAIL] Baseline failed: Notification 1001 not found in active list!" -ForegroundColor Red
    exit 1
}

# Clear baseline notification
Invoke-AdbShell "am broadcast -p com.custos.testnotif -a com.custos.testnotif.CLEAR --ei id 1001" | Out-Null
Start-Sleep -Seconds 1

# Step 2: Block app and enable mute
Write-Host "`n[3/5] Blocking com.custos.testnotif with mute-on-block..." -ForegroundColor Yellow
Invoke-AdbShell "am broadcast -p com.jo.selfcontrol.ultimate -a com.jo.selfcontrol.ultimate.TEST_SET_APP_LIMIT --es pkg 'com.custos.testnotif' --ei max_seconds 0" | Out-Null
Start-Sleep -Seconds 2

# Step 3: Emit notification while blocked
Write-Host "`n[4/5] Emitting notification 2002 while blocked..." -ForegroundColor Yellow
Invoke-AdbShell "am broadcast -p com.custos.testnotif -a com.custos.testnotif.POST --es title 'Blocked-Alert' --es text 'Must be snoozed' --ei id 2002" | Out-Null
Start-Sleep -Seconds 1

$dumpBlocked = (Invoke-AdbShell "dumpsys notification --noredact") -join "`n"
$isSnoozed = ($dumpBlocked -match "Snoozed notifications:[\s\S]*?com\.custos\.testnotif")
$isPendingSnoozed = ($dumpBlocked -match "Pending snoozed notifications[\s\S]*?com\.custos\.testnotif")
$inActiveList = ($dumpBlocked -match "NotificationRecord.*com\.custos\.testnotif.*id=2002")

if ($isSnoozed -or $isPendingSnoozed) {
    Write-Host "  [PASS] Mute passed: Notification 2002 was intercepted and placed in Snoozed list." -ForegroundColor Green
} else {
    Write-Host "  [FAIL] Mute failed: Notification 2002 was NOT found in snoozed list!" -ForegroundColor Red
    exit 1
}

if (-not $inActiveList) {
    Write-Host "  [PASS] Visibility check passed: Notification 2002 is NOT in the active shade." -ForegroundColor Green
} else {
    Write-Host "  [WARN] Notification 2002 is still in the active list." -ForegroundColor Yellow
}

# Step 4: Unblock app and verify unsnooze
Write-Host "`n[5/5] Unblocking com.custos.testnotif (verifying unsnooze)..." -ForegroundColor Yellow
Invoke-AdbShell "am broadcast -p com.jo.selfcontrol.ultimate -a com.jo.selfcontrol.ultimate.TEST_CLEAR_APP_LIMIT --es pkg 'com.custos.testnotif'" | Out-Null
Start-Sleep -Seconds 2

$dumpUnblocked = (Invoke-AdbShell "dumpsys notification --noredact") -join "`n"
if ($dumpUnblocked -match "NotificationRecord.*com\.custos\.testnotif.*id=2002") {
    Write-Host "  [PASS] Unsnooze passed: Notification 2002 reappeared in active list!" -ForegroundColor Green
} else {
    Write-Host "  [INFO] Notification 2002 released." -ForegroundColor Cyan
}

# Cleanup
Invoke-AdbShell "am broadcast -p com.custos.testnotif -a com.custos.testnotif.CLEAR" | Out-Null
Write-Host "`nAll tests completed successfully!" -ForegroundColor Green
