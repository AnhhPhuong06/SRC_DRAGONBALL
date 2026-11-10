@echo off
setlocal EnableExtensions EnableDelayedExpansion
pushd "%~dp0"
echo 0. Pre-build checks...
tasklist /FI "IMAGENAME eq java.exe" 2>nul | "%SystemRoot%\System32\find.exe" /I "java.exe" >nul
if not errorlevel 1 if not defined FORCE_BUILD (
    echo [CANH BAO] Dang co tien trinh java chay. Nen STOP server truoc khi build de tranh loi class thieu.
    choice /C YN /N /M "Van tiep tuc build? Y=Yes N=No: "
    if errorlevel 2 (
        echo Build da huy.
        goto :cancelled
    )
)
set "LOCKTEST="
if exist "dist\chienbinhlucy.jar" (
    ren "dist\chienbinhlucy.jar" "__locktest__.jar" >nul 2>&1
    if exist "dist\__locktest__.jar" (
        ren "dist\__locktest__.jar" "chienbinhlucy.jar" >nul 2>&1
        if not exist "dist\chienbinhlucy.jar" set "LOCKTEST=1"
    ) else (
        set "LOCKTEST=1"
    )
)
if defined LOCKTEST (
    echo [LOI] Khong the ghi de dist\chienbinhlucy.jar - file bi khoa. STOP server truoc khi build.
    goto :cancelled
)
set "JDK=%JAVA_HOME%"
if not exist "%JDK%\bin\javac.exe" set "JDK=C:\Program Files\Java\jdk-21.0.12"
set "JAVAC=javac"
set "JAR=jar"
if exist "%JDK%\bin\javac.exe" (
    set "JAVAC=%JDK%\bin\javac.exe"
    set "JAR=%JDK%\bin\jar.exe"
)

rem Keep old SLF4J alpha JARs untouched, but never load duplicate versions.
set "BUILD_CP="
for %%J in (lib\*.jar) do (
    if /I not "%%~nxJ"=="slf4j-api-2.0.0-alpha1.jar" if /I not "%%~nxJ"=="slf4j-simple-2.0.0-alpha1.jar" (
        if defined BUILD_CP (set "BUILD_CP=!BUILD_CP!;%%~fJ") else set "BUILD_CP=%%~fJ"
    )
)
if not exist build\classes mkdir build\classes
if not exist dist\lib mkdir dist\lib

echo 1. Gathering Java source files...
rem Quote paths in the javac argument file so project folders may contain spaces.
(for /r src %%F in (*.java) do (
    set "SOURCE_FILE=%%F"
    echo "!SOURCE_FILE:\=/!"
)) > build\web-sources.txt

echo 2. Compiling for Java 21...
set "LOMBOK_OPTS="
for %%P in (code comp file main model parser processing tree util jvm) do (
    set "LOMBOK_OPTS=!LOMBOK_OPTS! -J--add-opens=jdk.compiler/com.sun.tools.javac.%%P=ALL-UNNAMED"
)
"%JAVAC%" %LOMBOK_OPTS% --release 21 -encoding UTF-8 -sourcepath src -cp "!BUILD_CP!" -processorpath "lib/lombok.jar" -d build/classes @build/web-sources.txt
if errorlevel 1 goto :failed

echo 3. Copying web resources and runtime libraries...
python -I tools\stage_panel_assets.py . build\classes
if errorlevel 1 goto :failed
for %%J in (lib\*.jar) do (
    if /I not "%%~nxJ"=="slf4j-api-2.0.0-alpha1.jar" if /I not "%%~nxJ"=="slf4j-simple-2.0.0-alpha1.jar" copy /y "%%J" "dist\lib\" >nul
)

echo 4. Packaging dist/chienbinhlucy.jar...
"%JAR%" cfm dist/chienbinhlucy.jar manifest.mf -C build/classes .
if errorlevel 1 goto :failed

echo Build successful. Database migration and game startup were NOT run.
popd
exit /b 0

:failed
echo Build failed. Check JDK 21+, dependencies and whether a running process has locked the output JAR.
:cancelled
popd
exit /b 1
