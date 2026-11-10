@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\Enable-SocketServerRemote.ps1"
if errorlevel 1 (
    echo.
    echo Run this file as Administrator to allow TCP 4425 in Windows Firewall.
    pause
    exit /b 1
)
echo.
pause
