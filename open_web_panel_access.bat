@echo off
setlocal
pushd "%~dp0"
echo Run this file as Administrator ON THE WINDOWS VPS.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\Enable-WebPanelRemote.ps1"
set "PANEL_ACCESS_EXIT=%ERRORLEVEL%"
popd
pause
exit /b %PANEL_ACCESS_EXIT%
