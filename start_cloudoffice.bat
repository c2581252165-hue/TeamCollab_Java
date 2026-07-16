@echo off
title CloudOffice Startup Script
echo =========================================
echo       CloudOffice One-Click Startup
echo =========================================

echo [1/2] Starting Java Backend (Port 8000)...
start "Backend" cmd /c "cd /d %~dp0backend_java && start.bat"

echo [2/2] Starting Vue Frontend (Port 5173)...
start "Frontend" cmd /c "cd /d %~dp0frontend && npm run dev"

echo.
echo Startup commands have been issued!
echo Two new terminal windows should appear.
echo.
pause
