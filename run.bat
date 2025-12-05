@echo off
echo Building project...
call gradlew.bat build --no-daemon
echo.
echo Running Tomasulo Simulator...
call gradlew.bat run --no-daemon
pause













