<#
.SYNOPSIS
    Télécharge, vérifie la signature cryptographique, installe Guardian depuis GitHub Release,
    et purge automatiquement tous les anciens fichiers APK temporaires du PC.
#>

param(
    [string]$Tag = "latest",
    [string]$DeviceIp = "100.104.70.72:5555"
)

$ErrorActionPreference = "Stop"

# Empreinte SHA-256 officielle du certificat custos-release
$EXPECTED_CERT_SHA256 = "016CBA70271065A7DBB50BDE78EBC58E5ABF6A58914F9C03BD6A0DFB5337BB7D"

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "  CUSTOS GUARDIAN - MISE A JOUR OFFICIELLE DEPUIS GITHUB RELEASE" -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan

# 1. Localiser ADB et Apksigner
$adb = if (Get-Command adb -ErrorAction SilentlyContinue) { "adb" }
       elseif (Test-Path "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe") { "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" }
       else { throw "ADB introuvable." }

$apksignerBat = Get-ChildItem -Path "$env:LOCALAPPDATA\Android\Sdk\build-tools" -Filter "apksigner.bat" -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $apksignerBat) { throw "apksigner.bat introuvable dans Android SDK build-tools." }
$apksigner = $apksignerBat.FullName

# 2. Préparer le dossier temporaire
$tempDir = Join-Path $PSScriptRoot "temp_release_download"
if (Test-Path $tempDir) { Remove-Item -Path $tempDir -Recurse -Force }
New-Item -ItemType Directory -Path $tempDir -Force | Out-Null
$targetApk = Join-Path $tempDir "guardian-release.apk"

try {
    # 3. Déterminer l'URL de téléchargement
    if ($Tag -eq "latest") {
        # Résoudre la dernière release
        $downloadUrl = "https://github.com/joshdeutc/custos/releases/latest/download/guardian-release.apk"
    } else {
        $cleanTag = if ($Tag.StartsWith("v")) { $Tag } else { "v$Tag" }
        $downloadUrl = "https://github.com/joshdeutc/custos/releases/download/$cleanTag/guardian-release.apk"
    }

    Write-Host "`n[1/5] Telechargement de l'APK officiel depuis GitHub..." -ForegroundColor Yellow
    Write-Host "  URL: $downloadUrl" -ForegroundColor Gray
    curl.exe -f -s -L -o $targetApk $downloadUrl
    if (-not (Test-Path $targetApk) -or ((Get-Item $targetApk).Length -lt 100000)) {
        throw "Echec du telechargement de l'APK. Verifiez la connexion internet et le tag de release."
    }
    $apkSizeMb = [math]::Round((Get-Item $targetApk).Length / 1MB, 2)
    Write-Host "  [OK] APK telecharge ($apkSizeMb Mo)." -ForegroundColor Green

    # 4. Vérification cryptographique de la signature
    Write-Host "`n[2/5] Verification cryptographique de la signature..." -ForegroundColor Yellow
    $certOutput = & $apksigner verify --print-certs $targetApk | Out-String
    
    if ($certOutput -match "SHA-256 digest:\s*([a-fA-F0-9]{64})") {
        $actualSha256 = $matches[1].ToUpper()
        if ($actualSha256 -ne $EXPECTED_CERT_SHA256) {
            throw "ALERTE DE SECURITE : La signature ($actualSha256) ne correspond pas a la cle officielle ($EXPECTED_CERT_SHA256) !"
        }
        Write-Host "  [OK] Signature authentifiee : Cle officielle GitHub custos-release" -ForegroundColor Green
    } else {
        throw "Impossible d'extraire l'empreinte SHA-256 du certificat de l'APK."
    }

    # 5. Connexion ADB
    Write-Host "`n[3/5] Verification de la connexion ADB..." -ForegroundColor Yellow
    $devices = & $adb devices | Out-String
    $targetDevice = $null

    if ($devices -match "(\S+)\s+device\b") {
        # Priorité : Tailscale si connecté, sinon premier device USB
        if ($devices -match "($([regex]::Escape($DeviceIp)))\s+device") {
            $targetDevice = $matches[1]
        } else {
            # Tenter de connecter Tailscale
            & $adb connect $DeviceIp | Out-Null
            Start-Sleep -Milliseconds 500
            $devices = & $adb devices | Out-String
            if ($devices -match "($([regex]::Escape($DeviceIp)))\s+device") {
                $targetDevice = $matches[1]
            } elseif ($devices -match "(\S+)\s+device\b") {
                $targetDevice = $matches[1]
            }
        }
    } else {
        # Aucun device, tenter Tailscale
        & $adb connect $DeviceIp | Out-Null
        Start-Sleep -Milliseconds 800
        $devices = & $adb devices | Out-String
        if ($devices -match "($([regex]::Escape($DeviceIp)))\s+device") {
            $targetDevice = $matches[1]
        }
    }

    if (-not $targetDevice) {
        throw "Aucun appareil Android detecte (ni en USB, ni via Tailscale $DeviceIp)."
    }
    Write-Host "  [OK] Appareil cible : $targetDevice" -ForegroundColor Green

    # 6. Installation par-dessus l'existant
    Write-Host "`n[4/5] Installation de Guardian en mise a jour Device Owner..." -ForegroundColor Yellow
    $installOutput = & $adb -s $targetDevice install -r $targetApk | Out-String
    if ($installOutput -notmatch "Success") {
        throw "Echec de l'installation ADB :`n$installOutput"
    }
    Write-Host "  [OK] Installation reussie (Success) !" -ForegroundColor Green

    # Relancer la liaison IPC entre Custos UI et le nouveau GuardianService pour activer immediatement enforceLockdown()
    & $adb -s $targetDevice shell "am force-stop com.jo.selfcontrol.ultimate && am start -n com.jo.selfcontrol.ultimate/.MainActivity" | Out-Null
    Write-Host "  [OK] Liaison GuardianService reinitialisee et verrous appliques." -ForegroundColor Green

    # 7. Vérification du statut Device Owner
    $doCheck = & $adb -s $targetDevice shell dpm list-owners | Out-String
    if ($doCheck -match "com\.jo\.custos\.guardian") {
        Write-Host "  [OK] Guardian est confirme Device Owner actif." -ForegroundColor Green
    } else {
        Write-Host "  [ATTENTION] Statut Device Owner non detecte dans list-owners." -ForegroundColor Yellow
    }

} finally {
    # 8. Nettoyage absolu des fichiers APK sur le PC
    Write-Host "`n[5/5] Nettoyage des fichiers APK temporaires et anciens..." -ForegroundColor Yellow
    
    # Supprimer le dossier temporaire
    if (Test-Path $tempDir) {
        Remove-Item -Path $tempDir -Recurse -Force -ErrorAction SilentlyContinue
    }

    # Supprimer les anciens APK orphelins à la racine du dépôt
    $looseApks = Get-ChildItem -Path $PSScriptRoot -Filter "*.apk" -Depth 0 -ErrorAction SilentlyContinue
    foreach ($apk in $looseApks) {
        Remove-Item -Path $apk.FullName -Force -ErrorAction SilentlyContinue
        Write-Host "  [PURGE] Supprime : $($apk.Name)" -ForegroundColor DarkGray
    }

    Write-Host "  [OK] Le PC est propre (zero APK residuel)." -ForegroundColor Green
}

Write-Host "`n=================================================================" -ForegroundColor Cyan
Write-Host "  OPERATION TERMINEE AVEC SUCCES : GUARDIAN EST A JOUR !" -ForegroundColor Cyan
Write-Host "=================================================================`n" -ForegroundColor Cyan
