@echo off
title JavaWeb Dev Server
echo ==========================================
echo   Starting JavaWeb Frontend & Backend...
echo ==========================================

:: 启动后端服务
echo [INFO] Starting Backend (Spring Boot)...
start "Backend - Spring Boot" cmd /k "cd /d %~dp0backend && .\mvnw.cmd spring-boot:run"

:: 启动前端服务
echo [INFO] Starting Frontend (npm)...
start "Frontend - Vite/React" cmd /k "cd /d %~dp0frontend && npm.cmd run dev"

echo ==========================================
echo   Both servers are starting in new windows.
echo   Close the new windows to stop the servers.
echo ==========================================
pause