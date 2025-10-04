#!/bin/bash

set -e

# Get script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
INFRA_DIR="$PROJECT_ROOT/infra"

echo "🚀 Starting ECold Application..."
echo ""

# Navigate to infra directory
cd "$INFRA_DIR"

# Check if .env exists
if [ ! -f ".env" ]; then
    echo "📝 Creating .env file from template..."
    if [ -f ".env.example" ]; then
        cp .env.example .env
        echo "⚠️  Please edit infra/.env file with your OAuth credentials!"
        echo "   Required: GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET"
        echo ""
        exit 1
    else
        echo "❌ Error: .env.example not found in infra directory"
        exit 1
    fi
fi

# Start services
echo "🐳 Starting Docker services..."
docker-compose up -d

echo "⏳ Waiting for services to be ready..."
sleep 15

echo ""
echo "✅ ECold is ready!"
echo ""
echo "🌐 Access the application:"
echo "   Frontend: http://localhost:4200"
echo "   Backend:  http://localhost:8080/api"
echo "   Database: http://localhost:5050 (admin@ecold.com / admin123)"
echo ""
echo "📖 Useful commands:"
echo "   View logs:     cd infra && docker-compose logs -f"
echo "   Stop services: cd infra && docker-compose down"
echo "   Database:      cd infra && docker-compose exec postgres psql -U ecold_user -d ecold"
echo ""
echo "🎉 Happy coding!"