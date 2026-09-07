@echo off
echo ===================================================================
echo           VoiceShield Full-Stack Platform Launcher
echo   (ML Engine :8001, Backend :8080, Risk Engine, Frontend :5173)
echo ===================================================================

cd /d "%~dp0"

echo [1/3] Starting Python ML Microservice (Port 8001)...
start "VoiceShield ML Service (Port 8001)" cmd /k "python ml\ml_service.py"

echo [2/3] Starting Spring Boot Backend with Java Risk Engine (Port 8080)...
start "VoiceShield Spring Boot Backend (Port 8080)" cmd /k "cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev"

echo [3/3] Starting React Web Dashboard (Port 5173)...
start "VoiceShield Frontend (Port 5173)" cmd /k "cd frontend && npm run dev"

echo.
echo All services launched!
echo Access the dashboard at: http://localhost:5173
echo ===================================================================
