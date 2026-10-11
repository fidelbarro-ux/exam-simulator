@echo off
title Exam Simulator

cd /d C:\Users\cabarro\IdeaProjects\exam-simulator

echo ==========================================
echo          EXAM SIMULATOR
echo ==========================================
echo.
echo Starting Spring Boot application...
echo.

start /B cmd /c "timeout /t 8 /nobreak >nul & start http://localhost:8080"

mvn spring-boot:run

pause