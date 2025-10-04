# ECold - Email Cold Outreach & Lead Tracking Platform

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/)
[![Angular](https://img.shields.io/badge/Angular-17-red.svg)](https://angular.io/)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)

> A comprehensive email automation and job application tracking platform that streamlines cold outreach to recruiters while intelligently tracking and categorizing incoming job-related emails.

## 🚀 Quick Start

### Prerequisites
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed
- Google OAuth credentials ([Setup Guide](docs/setup/OAUTH_SETUP_GUIDE.md))

### Start the Application

**Windows:**
```bash
scripts\start.bat
```

**Linux/Mac:**
```bash
./scripts/start.sh
```

### Access Points
- **Frontend**: http://localhost:4200
- **Backend API**: http://localhost:8080/api
- **Database Admin**: http://localhost:5050 (admin@ecold.com / admin123)

## 📋 Features

### ✨ Core Capabilities
- **🎯 Template-Based Management**: Organize recruiters with customizable email templates
- **📧 Bulk Email Automation**: Send personalized emails with dynamic variables
- **📅 Smart Scheduling**: Gmail API integration for scheduled email delivery
- **🔄 Follow-up Automation**: Automatic progression through follow-up templates
- **📊 Week-wise Organization**: Group recruiters by weeks with infinite scroll
- **📎 Resume Attachments**: Support for PDF/DOC/DOCX with validation
- **🔔 Smart Categorization**: AI-powered email classification (Interviews, Updates, Rejections)
- **📈 Analytics Dashboard**: Track response rates and campaign performance

### 🔐 Security & Integration
- OAuth 2.0 authentication (Google & Microsoft)
- JWT-based session management
- Gmail & Outlook API integration
- PostgreSQL database with full-text search

## 📁 Project Structure

```
ECold/
├── backend/              # Spring Boot application
│   ├── src/             # Java source code
│   └── Dockerfile       # Backend container
├── frontend/            # Angular application
│   ├── src/             # TypeScript/Angular code
│   └── Dockerfile       # Frontend container
├── database/            # Database initialization
│   └── init.sql         # Schema & sample data
├── infra/               # Infrastructure configuration
│   ├── docker-compose.yml           # Development setup
│   ├── docker-compose.prod.yml      # Production setup
│   ├── .env.example                 # Development env template
│   └── .env.production              # Production env template
├── scripts/             # Utility scripts
│   ├── start.sh         # Start script (Linux/Mac)
│   ├── start.bat        # Start script (Windows)
│   └── setup-email.sh   # Email configuration helper
├── docs/                # Documentation
│   ├── setup/           # Setup guides
│   ├── guides/          # Feature guides
│   ├── testing/         # Test cases
│   ├── ARCHITECTURE.md  # Technical architecture
│   └── DOCKER.md        # Docker guide
├── README.md            # This file
├── Requirements.md      # Project requirements
└── setup.md             # Detailed setup guide
```

## 🛠️ Technology Stack

### Backend
- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **Database**: PostgreSQL 15
- **Security**: Spring Security + OAuth 2.0
- **Email**: Gmail API, Microsoft Graph API
- **Scheduler**: Spring @Scheduled

### Frontend
- **Framework**: Angular 17
- **Language**: TypeScript 5.2
- **UI**: Angular Material 17
- **Charts**: Chart.js
- **State**: RxJS

### DevOps
- **Containerization**: Docker & Docker Compose
- **Build**: Maven (Backend), Angular CLI (Frontend)
- **Monitoring**: Spring Boot Actuator

## 📖 Documentation

### Setup & Configuration
- [Complete Setup Guide](setup.md) - Detailed local and production setup
- [Docker Setup](docs/DOCKER.md) - Docker-specific instructions
- [OAuth Configuration](docs/setup/OAUTH_SETUP_GUIDE.md) - Google/Microsoft OAuth setup
- [Email Setup](docs/setup/EMAIL_SETUP_GUIDE.md) - Email provider configuration

### Technical Documentation
- [Architecture Overview](docs/ARCHITECTURE.md) - System architecture and design
- [Requirements](Requirements.md) - Project requirements and specifications
- [Email Templates](docs/guides/EMAIL_TEMPLATE_INTEGRATION.md) - Template system guide

### Testing
- [Test Cases](docs/testing/TestCases.md) - Comprehensive test scenarios

## 🚀 Deployment

### Development
```bash
# Start development environment
./scripts/start.sh  # or start.bat on Windows

# View logs
cd infra && docker-compose logs -f

# Stop services
cd infra && docker-compose down
```

### Production

1. **Configure environment:**
```bash
cd infra
cp .env.production .env
# Edit .env with production credentials
```

2. **Deploy with Docker Compose:**
```bash
docker-compose -f docker-compose.prod.yml up -d
```

3. **For cloud deployment**, see [Setup Guide](setup.md#production-deployment)

## 🔧 Development

### Backend Development
```bash
cd backend
mvn clean install
mvn spring-boot:run
```

### Frontend Development
```bash
cd frontend
npm install
npm start
```

### Database Access
```bash
# Connect to PostgreSQL
cd infra && docker-compose exec postgres psql -U ecold_user -d ecold

# PgAdmin UI
http://localhost:5050
```

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Spring Boot for the robust backend framework
- Angular team for the amazing frontend framework
- Gmail & Microsoft Graph APIs for email integration
- Docker for containerization support

## 📞 Support

For issues and questions:
- 📧 Email: support@ecold.com
- 🐛 Issues: [GitHub Issues](https://github.com/yourusername/ecold/issues)
- 📖 Documentation: [docs/](docs/)

---

**Built with ❤️ by the ECold Team**
