@echo off
setlocal
pushd "%~dp0"
set "JAVA_EXE=java"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
if "%JAVA_EXE%"=="java" if exist "C:\Program Files\Java\jdk-21.0.12\bin\java.exe" set "JAVA_EXE=C:\Program Files\Java\jdk-21.0.12\bin\java.exe"
if not exist dist\chienbinhlucy.jar (
    echo Build the project first with build_server.bat.
    popd
    exit /b 1
)
rem With no arguments the tool prints help only. Schema writes require explicit flags.
"%JAVA_EXE%" -cp dist\chienbinhlucy.jar server.web.tools.WebAdminInit %*
set "SETUP_EXIT_CODE=%ERRORLEVEL%"
popd
exit /b %SETUP_EXIT_CODE%
