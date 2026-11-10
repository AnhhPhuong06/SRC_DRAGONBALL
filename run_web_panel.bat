@echo off
setlocal
title Ngoc Rong - Java Web Panel
pushd "%~dp0"
echo Java Web Panel: http://127.0.0.1:8888/ by default
echo Cong, host va tai khoan: data\config\web_admin.properties
echo Khoi tao cau hinh: init_web_admin.bat --help
rem -web runs the game engine and the embedded panel without Swing.
call "%~dp0run_server.bat" -web %*
set "PANEL_EXIT_CODE=%ERRORLEVEL%"
popd
exit /b %PANEL_EXIT_CODE%
