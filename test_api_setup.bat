@echo off
echo ========================================
echo Manga Learn JP - API Setup Test
echo ========================================
echo.
echo This script will help you test your API setup
echo and diagnose common configuration issues.
echo.
echo Instructions:
echo 1. Make sure you have configured at least one API key in the app
echo 2. Run this script to check for common issues
echo 3. Follow the troubleshooting steps if any issues are found
echo.
echo ========================================
echo Checking Android Debug Bridge (ADB)...
echo ========================================

adb version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ ADB not found or not in PATH
    echo Please install Android SDK Platform Tools
    echo Download from: https://developer.android.com/studio/releases/platform-tools
    pause
    exit /b 1
)

echo ✅ ADB is available
echo.

echo ========================================
echo Checking connected devices...
echo ========================================

adb devices
echo.

echo ========================================
echo Checking app installation...
echo ========================================

adb shell pm list packages | findstr "com.example.manga_apk" >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Manga Learn JP app not found on device
    echo Please install the app first using install_debug.bat
    pause
    exit /b 1
)

echo ✅ Manga Learn JP app is installed
echo.

echo ========================================
echo Starting app and monitoring logs...
echo ========================================
echo.
echo Starting the app...
adb shell am start -n com.example.manga_apk/.MainActivity
echo.
echo Monitoring logs for API configuration issues...
echo Press Ctrl+C to stop monitoring
echo.
echo Look for these patterns in the logs:
echo - "API key updated" - indicates successful key saving
echo - "No AI providers configured" - indicates configuration issue
echo - "Validation failed" - indicates validation problems
echo - "whitespace" - indicates API key formatting issues
echo.
echo ========================================
echo Live Log Monitoring (Press Ctrl+C to stop)
echo ========================================

adb logcat -s MangaLearnJP:* | findstr /i "api key config provider validation error"