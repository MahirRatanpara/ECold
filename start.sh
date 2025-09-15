#!/bin/bash

set -e

echo "🚀 Starting ECold Application..."

# Check if .env exists
if [ ! -f ".env" ]; then
    echo "📝 Creating .env file..."
    cp .env.example .env
    echo "⚠️  Please edit .env file with your OAuth credentials!"
    echo "   Required: GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET"
    exit 1
fi

# Start services
echo "🐳 Starting Docker services..."
docker-compose up -d

echo "⏳ Waiting for services to be ready..."
sleep 15

echo "✅ ECold is ready!"
echo ""
echo "🌐 Access the application:"
echo "   Frontend: http://localhost:4200"
echo "   Backend:  http://localhost:8080/api"
echo "   Database: http://localhost:5050 (admin@ecold.com / admin123)"
echo ""
echo "📖 Useful commands:"
echo "   View logs:     docker-compose logs -f"
echo "   Stop services: docker-compose down"
echo "   Database:      docker-compose exec postgres psql -U ecold_user -d ecold"
echo ""
echo "🎉 Happy coding!"