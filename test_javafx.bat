@echo off
echo Testing JavaFX setup...
echo.

echo Checking Java version:
java -version
echo.

echo Checking if JAR exists:
if exist build\libs\javafx-test-1.0-SNAPSHOT.jar (
    echo JAR found: build\libs\javafx-test-1.0-SNAPSHOT.jar
) else (
    echo ERROR: JAR not found!
    pause
    exit /b 1
)
echo.

echo Attempting to run with Gradle...
echo This should open the GUI window...
echo.
call gradlew.bat run --no-daemon --console=plain
echo.

echo If the window didn't open, try running the JAR directly:
echo java -jar build\libs\javafx-test-1.0-SNAPSHOT.jar
echo.
pause













