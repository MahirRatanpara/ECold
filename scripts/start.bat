@echo off
setlocal

REM Get script directory and navigate to infra
set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%.."
set "INFRA_DIR=%PROJECT_ROOT%\infra"

echo 🚀 Starting ECold Application...
echo.

REM Navigate to infra directory
cd /d "%INFRA_DIR%"

REM Check if .env exists
if not exist ".env" (
    echo 📝 Creating .env file from template...
    if exist ".env.example" (
        copy .env.example .env >nul
        echo ⚠️  Please edit infra\.env file with your OAuth credentials!
        echo    Required: GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET
        echo.
        pause
        exit /b 1
    ) else (
        echo ❌ Error: .env.example not found in infra directory
        pause
        exit /b 1
    )
)

REM Start services
echo 🐳 Starting Docker services...
docker-compose up -d

echo ⏳ Waiting for services to be ready...
timeout /t 15 /nobreak >nul

echo.
echo ✅ ECold is ready!
echo.
echo 🌐 Access the application:
echo    Frontend: http://localhost:4200
echo    Backend:  http://localhost:8080/api
echo    Database: http://localhost:5050 (admin@ecold.com / admin123)
echo.
echo 📖 Useful commands:
echo    View logs:     cd infra ^&^& docker-compose logs -f
echo    Stop services: cd infra ^&^& docker-compose down
echo    Database:      cd infra ^&^& docker-compose exec postgres psql -U ecold_user -d ecold
echo.
echo 🎉 Happy coding!
pause
