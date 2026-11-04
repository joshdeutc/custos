# ==============================================================================
# connect_tailscale.ps1 - Connexion sans fil ADB au Google Pixel 8a via Tailscale
# ==============================================================================

$adb = if (Get-Command adb -ErrorAction SilentlyContinue) { 
    "adb" 
} elseif (Test-Path "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe") { 
    "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" 
} else { 
    Write-Host "[ERREUR] adb introuvable." -ForegroundColor Red
    exit 1
}

$pixelIp = "100.104.70.72:5555"
Write-Host "`n[ADB] Connexion sans fil au Pixel 8a via Tailscale ($pixelIp)..." -ForegroundColor Cyan

& $adb connect $pixelIp
Start-Sleep -Milliseconds 800

$devices = & $adb devices
Write-Host "`nAppareils connectes :" -ForegroundColor Green
Write-Host $devices
