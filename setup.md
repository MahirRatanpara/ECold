# ECold - Local Setup Guide

## Prerequisites

Before you begin, ensure you have the following installed on your system:

- **Docker Desktop** (recommended approach)
- **Docker Compose** v2.0 or higher
- **Git** for version control

### Alternative Manual Setup Requirements
If you prefer manual setup without Docker:
- **Java 17** or higher
- **Node.js 18** or higher
- **PostgreSQL 13** or higher
- **Redis 6** or higher
- **Maven 3.8** or higher
- **Angular CLI 17** or higher

## 🚀 Quick Start with Docker (Recommended)

### 1. Clone the Repository

```bash
git clone <your-repository-url>
cd ECold
```

### 2. Environment Configuration

```bash
# Copy environment template
cp .env.example .env

# Edit the .env file with your OAuth credentials
# At minimum, you need to set:
# - GOOGLE_CLIENT_ID
# - GOOGLE_CLIENT_SECRET
```

### 3. Start the Application

```bash
# Start all services
docker-compose up -d

# View startup logs (optional)
docker-compose logs -f
```

### 4. Access the Application

- **Frontend**: http://localhost:4200
- **Backend API**: http://localhost:8080/api
- **API Documentation**: http://localhost:8080/api/swagger-ui.html
- **PgAdmin**: http://localhost:5050 (admin@ecold.com / admin123)
- **Redis Insight**: http://localhost:8001

### 5. Optional Development Tools

```bash
# Start additional development tools
docker-compose --profile tools up -d pgadmin redis-insight mailhog

# Access tools:
# - Mailhog (email testing): http://localhost:8025
# - PgAdmin (database management): http://localhost:5050
# - Redis Insight (cache management): http://localhost:8001
```

## 📋 Essential Docker Commands

### Basic Operations

```bash
# Start all services
docker-compose up -d

# Stop all services
docker-compose down

# View logs from all services
docker-compose logs -f

# Restart all services
docker-compose restart

# Check service status
docker-compose ps
```

### Database Operations

```bash
# Connect to PostgreSQL
docker-compose exec postgres psql -U ecold_user -d ecold

# Create database backup
docker-compose exec postgres pg_dump -U ecold_user ecold > backup.sql

# Restore from backup
docker-compose exec -T postgres psql -U ecold_user ecold < backup.sql
```

### Development Tools

```bash
# Start development tools
docker-compose --profile tools up -d pgadmin redis-insight mailhog

# Access backend container
docker-compose exec backend sh

# Run tests
docker-compose exec backend ./mvnw test
```

## 🔧 Docker Services Overview

The Docker setup includes:

### Core Services
- **PostgreSQL 15**: Pre-configured with ECold schema and sample data
- **Redis 7**: Optimized for caching with custom configuration
- **Spring Boot Backend**: API server with all dependencies
- **Angular Frontend**: Nginx-served SPA with proxy configuration

### Optional Tools (with `--profile tools`)
- **PgAdmin 4**: Database administration interface
- **Redis Insight**: Redis monitoring and management
- **Mailhog**: Email testing service

## 🗄️ Database Setup

The PostgreSQL container comes pre-configured with:

- **Database**: `ecold` (main) and `ecold_test` (testing)
- **User**: `ecold_user` with password `ecold_pass123`
- **Schema**: All tables created automatically with indexes
- **Sample Data**: Demo user with sample recruiters and campaigns

### Custom Database Features
- Optimized PostgreSQL configuration for development
- Automatic schema initialization
- Sample data for immediate testing
- Health checks and monitoring
- Backup and restore capabilities

## ⚡ Redis Configuration

The Redis container includes:

- **Persistence**: Both RDB and AOF enabled
- **Memory Management**: 256MB limit with LRU eviction
- **Monitoring**: Slow query logging and latency monitoring
- **Sample Cache**: Pre-loaded development data
- **Configuration**: Production-ready settings

## 🔐 OAuth Setup

### Google OAuth (Required for Gmail integration)
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing
3. Enable Gmail API
4. Create OAuth 2.0 credentials
5. Add authorized redirect URIs:
   - `http://localhost:8080/api/auth/google/callback`
   - `http://localhost:4200/auth/google/callback`
6. Update `.env` file with your credentials

### Microsoft OAuth (Optional for Outlook integration)
1. Go to [Azure Portal](https://portal.azure.com/)
2. Register a new application
3. Add Microsoft Graph API permissions
4. Configure redirect URIs similar to Google
5. Update `.env` file with your credentials

## 🐳 Docker Container Details

### Backend Container
- **Base**: OpenJDK 17 Alpine
- **User**: Non-root user for security
- **Health Check**: Automated health monitoring
- **Volumes**: Persistent file uploads and logs
- **Environment**: Production-ready JVM settings

### Frontend Container
- **Multi-stage Build**: Optimized production build
- **Nginx**: High-performance web server
- **Security**: Security headers and CORS configuration
- **Proxy**: API requests proxied to backend
- **Health Check**: Automated availability monitoring

### Database Container
- **Custom Image**: Extended PostgreSQL with ECold schema
- **Initialization**: Automatic database and user creation
- **Sample Data**: Pre-loaded development data
- **Configuration**: Optimized for development workload
- **Monitoring**: Query logging and performance tracking

#### OAuth Setup

**Google OAuth:**
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing
3. Enable Gmail API
4. Create OAuth 2.0 credentials
5. Add authorized redirect URIs:
   - `http://localhost:8080/api/auth/google/callback`
   - `http://localhost:4200/auth/google/callback`

**Microsoft OAuth (Optional):**
1. Go to [Azure Portal](https://portal.azure.com/)
2. Register a new application
3. Add Microsoft Graph API permissions
4. Configure redirect URIs similar to Google

#### Start Backend

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

The backend will start on `http://localhost:8080`

### 3. Frontend Setup

```bash
cd frontend
npm install
npm start
```

The frontend will start on `http://localhost:4200`

## Development Setup

### Backend Development

```bash
cd backend
# Run with dev profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Run tests
mvn test

# Package for production
mvn clean package -Pprod
```

### Frontend Development

```bash
cd frontend
# Install dependencies
npm install

# Start development server
ng serve

# Run tests
ng test

# Build for production
ng build --prod
```

## Database Setup

### H2 Database (Default)
- Console: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: `password`

### PostgreSQL Setup
```sql
CREATE DATABASE ecold;
CREATE USER ecold_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE ecold TO ecold_user;
```

## Production Deployment

### Backend Deployment

1. **Build the application:**
```bash
cd backend
mvn clean package -Pprod
```

2. **Set environment variables:**
```bash
export DATABASE_URL=jdbc:postgresql://your_host:5432/ecold
export GOOGLE_CLIENT_ID=your_production_client_id
export JWT_SECRET=your_production_secret
# ... other variables
```

3. **Run the application:**
```bash
java -jar target/ecold-backend-0.0.1-SNAPSHOT.jar
```

### Frontend Deployment

1. **Build for production:**
```bash
cd frontend
ng build --prod
```

2. **Deploy dist folder to your web server (nginx, apache, etc.)**

### Docker Deployment (Recommended)

Create `docker-compose.yml`:
```yaml
version: '3.8'
services:
  postgres:
    image: postgres:13
    environment:
      POSTGRES_DB: ecold
      POSTGRES_USER: ecold_user
      POSTGRES_PASSWORD: your_password
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"

  redis:
    image: redis:6-alpine
    ports:
      - "6379:6379"

  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/ecold
      DATABASE_USERNAME: ecold_user
      DATABASE_PASSWORD: your_password
      REDIS_HOST: redis
      GOOGLE_CLIENT_ID: your_client_id
      GOOGLE_CLIENT_SECRET: your_client_secret
    ports:
      - "8080:8080"
    depends_on:
      - postgres
      - redis

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    ports:
      - "80:80"
    depends_on:
      - backend

volumes:
  postgres_data:
```

Run with:
```bash
docker-compose up -d
```

## Troubleshooting

### Common Issues

1. **Port already in use:**
   - Change ports in `application.yml` (backend) or `angular.json` (frontend)

2. **OAuth redirect URI mismatch:**
   - Ensure redirect URIs in OAuth provider match your application URLs

3. **Database connection issues:**
   - Check database credentials and connectivity
   - Ensure PostgreSQL is running if not using H2

4. **CORS issues:**
   - Update `CORS_ALLOWED_ORIGINS` in environment variables

5. **File upload issues:**
   - Ensure upload directory exists and has write permissions
   - Check file size limits in `application.yml`

### Performance Optimization

1. **Enable Redis caching:**
   ```bash
   # Install Redis
   docker run -d -p 6379:6379 redis:6-alpine
   
   # Set environment variable
   export REDIS_HOST=localhost
   ```

2. **Database optimization:**
   - Create indexes for frequently queried fields
   - Configure connection pooling
   - Enable query optimization

3. **Frontend optimization:**
   - Enable AOT compilation in production
   - Use lazy loading for routes
   - Implement OnPush change detection strategy

## Security Considerations

1. **Change default passwords and secrets**
2. **Use HTTPS in production**
3. **Implement rate limiting for API endpoints**
4. **Regular security updates for dependencies**
5. **Monitor application logs for suspicious activity**

## Monitoring and Logging

Access application metrics at:
- Health check: `http://localhost:8080/api/actuator/health`
- Metrics: `http://localhost:8080/api/actuator/metrics`
- Application logs: Check `logs/` directory

For production, consider integrating with:
- **ELK Stack** for log aggregation
- **Prometheus + Grafana** for metrics monitoring
- **Sentry** for error tracking