@echo off
echo Building PTP4L Configuration Tool EXE with SSH Support...

REM Clean previous builds
if exist target rmdir /s /q target
if exist dist rmdir /s /q dist

REM Build with Maven
echo Compiling with Maven...
call mvn clean package

REM Check if build was successful
if %ERRORLEVEL% neq 0 (
    echo Build failed!
    pause
    exit /b 1
)

echo.
echo JAR file created successfully at: target\ptpapp-1.0.0.jar
echo.

REM Create EXE with jpackage (includes JavaFX modules)
echo Creating Windows executable with jpackage...

REM Get the user's Maven repository path
set MAVEN_REPO=%USERPROFILE%\.m2\repository

REM Check if JavaFX modules exist
if not exist "%MAVEN_REPO%\org\openjfx\javafx-controls" (
    echo JavaFX modules not found in Maven repository. Installing dependencies...
    call mvn dependency:copy-dependencies -DoutputDirectory=target\lib
    if %ERRORLEVEL% neq 0 (
        echo Failed to copy dependencies!
        pause
        exit /b 1
    )
    
    REM Use the copied dependencies
    jpackage --input target --name PTP4L-Config-Tool --main-jar ptpapp-1.0.0.jar --main-class com.ptpapp.Main --type app-image --dest dist --module-path "target\lib" --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base
) else (
    REM Use Maven repository modules
    jpackage --input target --name PTP4L-Config-Tool --main-jar ptpapp-1.0.0.jar --main-class com.ptpapp.Main --type app-image --dest dist --module-path "%MAVEN_REPO%\org\openjfx\javafx-controls\17.0.2;%MAVEN_REPO%\org\openjfx\javafx-fxml\17.0.2;%MAVEN_REPO%\org\openjfx\javafx-graphics\17.0.2;%MAVEN_REPO%\org\openjfx\javafx-base\17.0.2" --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base
)

REM Check if jpackage was successful
if %ERRORLEVEL% neq 0 (
    echo jpackage failed! Check if JavaFX modules are available.
    pause
    exit /b 1
)

echo.
echo ✅ EXE created successfully!
echo Location: dist\PTP4L-Config-Tool\PTP4L-Config-Tool.exe
echo.
echo Features included:
echo - SSH connectivity for remote PTP4L management
echo - Modern JavaFX user interface
echo - Self-contained (no Java installation required)
echo - All PTP4L configuration options
echo - Real-time command output
echo.
echo To run: cd dist\PTP4L-Config-Tool ^&^& PTP4L-Config-Tool.exe
echo.

pause 