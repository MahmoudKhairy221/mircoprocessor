@echo off
echo ========================================
echo Tomasulo Algorithm Simulator
echo ========================================
echo.
echo Building project...
call gradlew.bat build --no-daemon
if %ERRORLEVEL% NEQ 0 (
    echo Build failed!
    pause
    exit /b 1
)
echo.
echo Build successful!
echo.
echo Starting simulator...
echo.
call gradlew.bat run --no-daemon
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Error running application!
    echo Trying alternative method...
    echo.
    java -jar build\libs\javafx-test-1.0-SNAPSHOT.jar
)
echo.
pause













