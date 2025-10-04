# Infrastructure Configuration

This directory contains all infrastructure and deployment configurations for ECold.

## 📁 Contents

- **docker-compose.yml** - Development environment configuration
- **docker-compose.prod.yml** - Production environment configuration
- **.env.example** - Development environment template
- **.env.production** - Production environment template

## 🚀 Quick Start

### Development

1. **Copy environment template:**
```bash
cp .env.example .env
```

2. **Edit .env with your credentials:**
```bash
# Required: Google OAuth
GOOGLE_CLIENT_ID=your_client_id
GOOGLE_CLIENT_SECRET=your_client_secret
```

3. **Start services:**
```bash
docker-compose up -d
```

### Production

1. **Copy production template:**
```bash
cp .env.production .env
```

2. **Configure all required variables:**
- Database credentials
- OAuth credentials
- JWT secret (strong random key)
- Domain configuration
- Security settings

3. **Deploy:**
```bash
docker-compose -f docker-compose.prod.yml up -d
```

## 🔧 Configuration Files

### docker-compose.yml (Development)
- PostgreSQL with sample data
- Backend (Spring Boot)
- Frontend (Angular)
- PgAdmin (optional with `--profile tools`)

### docker-compose.prod.yml (Production)
- Production-optimized containers
- Resource limits and reservations
- Health checks and auto-restart
- Security hardening
- Separate networks
- Volume management

### Environment Variables

#### Required
- `GOOGLE_CLIENT_ID` - Google OAuth client ID
- `GOOGLE_CLIENT_SECRET` - Google OAuth client secret
- `JWT_SECRET` - Secret key for JWT tokens (min 32 chars)
- `POSTGRES_PASSWORD` - Database password

#### Optional
- `MICROSOFT_CLIENT_ID` - Microsoft OAuth (optional)
- `MICROSOFT_CLIENT_SECRET` - Microsoft OAuth (optional)
- `ALLOWED_ORIGINS` - CORS origins (production)
- `DOMAIN_NAME` - Your domain name (production)

## 📊 Services

### PostgreSQL
- **Port**: 5432
- **Database**: ecold
- **User**: ecold_user
- **Init**: Automatic schema creation from `../database/init.sql`

### Backend
- **Port**: 8080
- **Health**: `/api/actuator/health`
- **API Docs**: `/api/swagger-ui.html`

### Frontend
- **Port**: 4200 (dev) / 80 (prod)
- **Nginx**: Reverse proxy to backend

### PgAdmin (Development)
- **Port**: 5050
- **Email**: admin@ecold.com
- **Password**: admin123
- **Start**: `docker-compose --profile tools up -d pgadmin`

## 🛠️ Common Commands

### Development
```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down

# Restart service
docker-compose restart backend

# Access database
docker-compose exec postgres psql -U ecold_user -d ecold

# Access backend shell
docker-compose exec backend sh

# Run backend tests
docker-compose exec backend ./mvnw test
```

### Production
```bash
# Start production
docker-compose -f docker-compose.prod.yml up -d

# View logs
docker-compose -f docker-compose.prod.yml logs -f

# Health check
curl http://localhost:8080/api/actuator/health

# Backup database
docker-compose -f docker-compose.prod.yml exec postgres \
  pg_dump -U ecold_user ecold > backup_$(date +%Y%m%d).sql

# Restore database
docker-compose -f docker-compose.prod.yml exec -T postgres \
  psql -U ecold_user ecold < backup.sql
```

## 🔐 Security Notes

### Production Checklist
- [ ] Change all default passwords
- [ ] Use strong JWT secret (min 32 chars, random)
- [ ] Configure HTTPS/TLS certificates
- [ ] Set up firewall rules
- [ ] Configure CORS allowed origins
- [ ] Review and limit exposed ports
- [ ] Set up monitoring and alerts
- [ ] Configure backup strategy
- [ ] Review container resource limits

### OAuth Configuration
1. Create Google/Microsoft OAuth credentials
2. Add authorized redirect URIs:
   - Development: `http://localhost:8080/api/auth/google/callback`
   - Production: `https://yourdomain.com/api/auth/google/callback`
3. Enable required APIs (Gmail API, Graph API)
4. Store credentials in `.env` file

## 📈 Scaling

### Horizontal Scaling
- Backend: Scale with `docker-compose up -d --scale backend=3`
- Add load balancer (nginx/HAProxy)
- Database replication for read scaling

### Resource Limits (Production)
- PostgreSQL: 1 CPU, 1GB RAM
- Backend: 2 CPU, 2GB RAM
- Frontend: 1 CPU, 512MB RAM

## 🐛 Troubleshooting

### Port Conflicts
```bash
# Check what's using a port
netstat -tulpn | grep :8080

# Change port in docker-compose.yml or .env
```

### Container Issues
```bash
# Check container status
docker-compose ps

# View container logs
docker-compose logs backend

# Restart containers
docker-compose restart
```

### Database Issues
```bash
# Reset database (WARNING: deletes all data)
docker-compose down -v
docker-compose up -d

# Check database logs
docker-compose logs postgres
```

### Performance Issues
```bash
# Check resource usage
docker stats

# Increase resource limits in docker-compose.prod.yml
```

## 📚 Additional Resources

- [Main Setup Guide](../setup.md)
- [Docker Guide](../docs/DOCKER.md)
- [Architecture Overview](../docs/ARCHITECTURE.md)
- [OAuth Setup](../docs/setup/OAUTH_SETUP_GUIDE.md)
