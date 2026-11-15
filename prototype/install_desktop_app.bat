@echo off
setlocal
echo ========================================================
echo   OFF-GRID SOS MESH - DESKTOP APP INSTALLER
echo ========================================================
echo.
echo Installing standalone application shortcut to your Desktop...

set "APP_DIR=%~dp0"
set "HTML_FILE=%APP_DIR%index.html"
set "SHORTCUT=%USERPROFILE%\Desktop\Off-Grid SOS Mesh.lnk"

if exist "%USERPROFILE%\OneDrive\Desktop" (
    set "SHORTCUT=%USERPROFILE%\OneDrive\Desktop\Off-Grid SOS Mesh.lnk"
)

powershell -NoProfile -Command "$WshShell = New-Object -ComObject WScript.Shell; $Shortcut = $WshShell.CreateShortcut('%SHORTCUT%'); $Shortcut.TargetPath = 'msedge.exe'; $Shortcut.Arguments = '--app=\"file:///%HTML_FILE:\=/%\"'; $Shortcut.Description = 'Off-Grid Multi-Hop Emergency SOS Mesh Application'; $Shortcut.Save();"

echo.
echo [SUCCESS] Off-Grid SOS Mesh application shortcut installed on Desktop!
echo You can now double-click "Off-Grid SOS Mesh" on your Desktop to open it as a standalone app.
echo.
pause
