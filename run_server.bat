@echo off
setlocal
pushd "%~dp0"
title Ngoc Rong Online - Server Management Console
chcp 65001 >nul

set "JAVA_EXE=java"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
if exist "C:\Program Files\Java\jdk-21.0.12\bin\java.exe" set "JAVA_EXE=C:\Program Files\Java\jdk-21.0.12\bin\java.exe"

set "JVM_OPTS=-server -Xms512m -Xmx2048m -Xss512k -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:CICompilerCount=4 -XX:ReservedCodeCacheSize=256m -Dfile.encoding=UTF-8"

echo Starting Ngoc Rong Server - JVM: %JAVA_EXE%
echo Memory: 512MB min / 2048MB max (G1GC)

"%JAVA_EXE%" %JVM_OPTS% -jar dist\chienbinhlucy.jar %*

set "SERVER_EXIT_CODE=%ERRORLEVEL%"
if not "%SERVER_EXIT_CODE%"=="0" (
    echo Server process exited with code %SERVER_EXIT_CODE%.
    pause
)
popd
exit /b %SERVER_EXIT_CODE%
