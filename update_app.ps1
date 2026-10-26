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
    & $adb connect 100.112.214.84:5555
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

Write-Host "`n[2/5] Compilation de Custos et Guardian (:app:assembleMeDebug :guardian:assembleDebug)..." -ForegroundColor Cyan
Push-Location $projectDir
try {
    & .\gradlew.bat :app:assembleMeDebug :guardian:assembleDebug
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERREUR] Échec de la compilation Gradle." -ForegroundColor Red
        exit 1
    }
} finally {
    Pop-Location
}

$appApk = Join-Path $projectDir "app\build\outputs\apk\me\debug\app-me-debug.apk"
$guardianApk = Join-Path $projectDir "guardian\build\outputs\apk\debug\guardian-debug.apk"

# 4. Déverrouillage temporaire de l'installation (Device Owner)
Write-Host "`n[3/5] Déverrouillage temporaire des restrictions d'installation..." -ForegroundColor Cyan
& $adb -s $deviceId shell am broadcast -p com.jo.selfcontrol.ultimate -a com.jo.selfcontrol.ultimate.ALLOW_INSTALL --ei minutes 10
Start-Sleep -Seconds 1

# 5. Installation des mises à jour
Write-Host "`n[4/5] Installation des APKs sur l'appareil..." -ForegroundColor Cyan
if (Test-Path $guardianApk) {
    Write-Host "-> Installation de Guardian..." -ForegroundColor Cyan
    & $adb -s $deviceId install -r $guardianApk
}
Write-Host "-> Installation de Custos..." -ForegroundColor Cyan
& $adb -s $deviceId install -r -d -t $appApk
$installOk = ($LASTEXITCODE -eq 0)

# 6. Re-verrouillage immédiat des restrictions d'installation
Write-Host "`n[5/5] Re-verrouillage des restrictions d'installation..." -ForegroundColor Cyan
& $adb -s $deviceId shell am broadcast -p com.jo.selfcontrol.ultimate -a com.jo.selfcontrol.ultimate.BLOCK_INSTALL

if ($installOk) {
    Write-Host "`n[SUCCÈS] Mise à jour installée avec succès !" -ForegroundColor Green
    Write-Host "Relance de l'application..." -ForegroundColor Cyan
    & $adb -s $deviceId shell am start -n com.jo.selfcontrol.ultimate/.MainActivity
} else {
    Write-Host "`n[ERREUR] L'installation a échoué." -ForegroundColor Red
    exit 1
}
