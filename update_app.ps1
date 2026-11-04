# ==============================================================================
# update_app.ps1 - Script de mise à jour automatique de Custos & Guardian
# ==============================================================================

$ErrorActionPreference = "Stop"

# 1. Détection d'ADB
$adb = if (Get-Command adb -ErrorAction SilentlyContinue) { 
    "adb" 
} elseif (Test-Path "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe") { 
    "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" 
} else { 
    Write-Host "[ERREUR] adb introuvable. Veuillez vérifier Android SDK ou ajouter adb au PATH." -ForegroundColor Red
    exit 1
}

# 2. Recherche de l'appareil Android
Write-Host "`n[1/5] Recherche de l'appareil Android connecté..." -ForegroundColor Cyan
$maxAttempts = 3
$deviceLines = @()

for ($attempt = 1; $attempt -le $maxAttempts; $attempt++) {
    $devicesOutput = & $adb devices
    $deviceLines = @($devicesOutput | Where-Object { $_ -match "\tdevice$" })
    if ($deviceLines.Count -gt 0) { break }
    Start-Sleep -Seconds 1
}

if ($deviceLines.Count -eq 0) {
    Write-Host "[INFO] Aucun appareil USB détecté, tentative de connexion Tailscale..." -ForegroundColor Yellow
    & $adb connect 100.104.70.72:5555
    Start-Sleep -Milliseconds 800
    $devicesOutput = & $adb devices
    $deviceLines = @($devicesOutput | Where-Object { $_ -match "\tdevice$" })
}

if ($deviceLines.Count -eq 0) {
    Write-Host "[ERREUR] Aucun appareil Android détecté avec ADB." -ForegroundColor Red
    Write-Host "Vérifiez que :" -ForegroundColor Yellow
    Write-Host "  1. Le téléphone est branché en USB ou connecté via Tailscale (100.112.214.84:5555)." -ForegroundColor Yellow
    Write-Host "  2. Le Débogage USB est activé." -ForegroundColor Yellow
    exit 1
}

$firstLine = [string]$deviceLines[0]
$deviceId = ($firstLine -split "`t")[0].Trim()
Write-Host "-> Appareil ciblé : $deviceId" -ForegroundColor Green

# 3. Détection du dossier du projet Gradle
$projectDir = if (Test-Path "..\custos\gradlew.bat") {
    "..\custos"
} elseif (Test-Path ".\gradlew.bat") {
    "."
} else {
    "C:\Users\jo\Documents\All_of_custos_project\custos"
}

Write-Host "`n[2/4] Compilation de Custos UI (:app:assembleMeDebug)..." -ForegroundColor Cyan
Push-Location $projectDir
try {
    & .\gradlew.bat :app:assembleMeDebug
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERREUR] Échec de la compilation Gradle." -ForegroundColor Red
        exit 1
    }
} finally {
    Pop-Location
}

$appApk = Join-Path $projectDir "app\build\outputs\apk\me\debug\app-me-debug.apk"

# 4. Installation de la mise à jour de Custos UI
Write-Host "`n[3/4] Installation de Custos UI sur l'appareil..." -ForegroundColor Cyan
& $adb -s $deviceId install -r -d -t $appApk
$installOk = ($LASTEXITCODE -eq 0)

# Nettoyage de l'APK compilé sur le PC
if (Test-Path $appApk) {
    Remove-Item -Path $appApk -Force -ErrorAction SilentlyContinue
}

# 5. Maintien automatique des services d'accessibilité et relance
Write-Host "`n[4/4] Vérification des services d'accessibilité (Guardian + Custos UI)..." -ForegroundColor Cyan
& $adb -s $deviceId shell "settings put secure enabled_accessibility_services com.jo.custos.guardian/com.jo.custos.guardian.GuardianWatcherService:com.jo.selfcontrol.ultimate/com.jo.selfcontrol.ultimate.AppWatcherService && settings put secure accessibility_enabled 1"

if ($installOk) {
    Write-Host "`n[SUCCÈS] Mise à jour de Custos UI installée avec succès (APK local purgé) !" -ForegroundColor Green
    Write-Host "Relance de l'application..." -ForegroundColor Cyan
    & $adb -s $deviceId shell am start -n com.jo.selfcontrol.ultimate/.MainActivity
} else {
    Write-Host "`n[ERREUR] L'installation a échoué." -ForegroundColor Red
    exit 1
}
