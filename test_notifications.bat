@echo off
chcp 65001 >nul
echo.
echo ==============================================================================
echo  Test du blocage et snooze des notifications Custos (Samsung)
echo ==============================================================================
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0tests\test_notifications_adb.ps1"

echo.
pause
