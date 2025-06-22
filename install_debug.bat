@echo off
echo Installing debug APK...
echo.
echo Please make sure your Android device is connected and USB debugging is enabled.
echo.
echo If you don't have ADB in your PATH, you can:
echo 1. Copy the APK file to your device and install manually
echo 2. Or install ADB and add it to your PATH
echo.
echo APK Location: app\build\outputs\apk\debug\app-debug.apk
echo.
pause

REM Try to install with adb if available
adb install -r app\build\outputs\apk\debug\app-debug.apk
if %errorlevel% neq 0 (
    echo.
    echo ADB not found or installation failed.
    echo Please install the APK manually from: app\build\outputs\apk\debug\app-debug.apk
    echo.
)
pause