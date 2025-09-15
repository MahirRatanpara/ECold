@echo off
echo 🚀 Starting ECold Application...

REM Check if .env exists
if not exist ".env" (
    echo 📝 Creating .env file...
    copy .env.example .env >nul
    echo ⚠️  Please edit .env file with your OAuth credentials!
    echo    Required: GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET
    pause
    exit /b 1
)

REM Start services
echo 🐳 Starting Docker services...
docker-compose up -d

echo ⏳ Waiting for services to be ready...
timeout /t 15 /nobreak >nul

echo ✅ ECold is ready!
echo.
echo 🌐 Access the application:
echo    Frontend: http://localhost:4200
echo    Backend:  http://localhost:8080/api
echo    Database: http://localhost:5050 (admin@ecold.com / admin123)
echo.
echo 📖 Useful commands:
echo    View logs:     docker-compose logs -f
echo    Stop services: docker-compose down
echo    Database:      docker-compose exec postgres psql -U ecold_user -d ecold
echo.
echo 🎉 Happy coding!
pause