# ECold - Simple Docker Setup

## 🚀 Quick Start

### Prerequisites
- Install [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- Get Google OAuth credentials from [Google Cloud Console](https://console.cloud.google.com/)

### 1. Setup OAuth
1. Go to Google Cloud Console → Create project → Enable Gmail API
2. Create OAuth 2.0 credentials
3. Add redirect URIs:
   - `http://localhost:8080/api/auth/google/callback`
   - `http://localhost:4200/auth/google/callback`

### 2. Start ECold

**Windows:**
```batch
start.bat
```

**Linux/Mac:**
```bash
./start.sh
```

**Manual:**
```bash
# Copy environment template
cp .env.example .env
# Edit .env with your OAuth credentials

# Start services
docker-compose up -d
```

### 3. Access Application
- **Frontend**: http://localhost:4200
- **Backend**: http://localhost:8080/api
- **Database UI**: http://localhost:5050 (admin@ecold.com / admin123)

## 📁 File Structure

```
ECold/
├── backend/                 # Spring Boot API
│   ├── src/                # Java source code
│   └── Dockerfile          # Backend container
├── frontend/               # Angular SPA
│   ├── src/                # TypeScript/Angular code
│   ├── nginx.conf          # Nginx proxy config
│   └── Dockerfile          # Frontend container
├── database/               # Database setup
│   └── init.sql           # Schema + sample data
├── docker-compose.yml     # Main Docker configuration
├── .env.example          # Environment template
├── start.sh / start.bat  # Start scripts
└── README.md            # This file
```

## ⚡ Essential Commands

### Start/Stop
```bash
# Start all services
docker-compose up -d

# Stop all services  
docker-compose down

# Stop and remove data
docker-compose down -v
```

### View Logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs backend
docker-compose logs frontend
```

### Database
```bash
# Connect to database
docker-compose exec postgres psql -U ecold_user -d ecold

# Backup database
docker-compose exec postgres pg_dump -U ecold_user ecold > backup.sql

# Start database UI
docker-compose --profile tools up -d pgadmin
```

### Development
```bash
# Rebuild services
docker-compose build

# Access backend shell
docker-compose exec backend sh

# Run tests
docker-compose exec backend ./mvnw test
```

## 🗄️ What's Included

### Pre-configured Database
- PostgreSQL 15 with ECold schema
- Sample user: `demo@ecold.com`
- 3 sample recruiters
- Email templates and campaigns
- Incoming email examples

### Services
- **Backend**: Spring Boot with OAuth, email integration, batch processing
- **Frontend**: Angular with Material Design, real-time dashboard
- **Database**: PostgreSQL with sample data
- **Cache**: Redis for sessions and rate limiting
- **UI**: PgAdmin for database management (optional)

## 🔧 Configuration

### Environment Variables (.env)
```bash
# Required
GOOGLE_CLIENT_ID=your_client_id
GOOGLE_CLIENT_SECRET=your_client_secret

# Optional
JWT_SECRET=your_secret_key
```

### Ports Used
- `4200` - Frontend (Angular)
- `8080` - Backend (Spring Boot)
- `5432` - Database (PostgreSQL) 
- `6379` - Cache (Redis)
- `5050` - PgAdmin (optional)

## 🐛 Troubleshooting

### Port Conflicts
```bash
# Check what's using port
netstat -tulpn | grep :4200

# Change ports in docker-compose.yml
```

### OAuth Issues
1. Verify `.env` file has correct credentials
2. Check redirect URIs in Google Console
3. View backend logs: `docker-compose logs backend`

### Service Issues
```bash
# Check service status
docker-compose ps

# Health check
curl http://localhost:8080/api/actuator/health

# Restart service
docker-compose restart backend
```

### Clean Restart
```bash
# Stop everything and remove data
docker-compose down -v

# Clean Docker system
docker system prune -f

# Start fresh
docker-compose up -d
```

## 🚀 Next Steps

1. Login with Google OAuth at http://localhost:4200
2. Import recruiters from CSV/Excel
3. Create email templates
4. Launch your first campaign
5. Monitor incoming emails with smart categorization

That's it! Simple, clean, and ready for development! 🎉