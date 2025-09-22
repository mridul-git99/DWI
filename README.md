# DWI - Digital Work Instructions Platform

[![Version](https://img.shields.io/badge/version-4.9.11-blue.svg)](https://github.com/mridul-git99/DWI)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.x-blue.svg)](https://reactjs.org/)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.java.net/projects/jdk/17/)
[![TypeScript](https://img.shields.io/badge/TypeScript-4.x-blue.svg)](https://www.typescriptlang.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-11-blue.svg)](https://www.postgresql.org/)

## Overview

**DWI (Digital Work Instructions)** is an enterprise-grade platform designed for manufacturing and industrial process management. It transforms traditional paper-based standard operating procedures (SOPs) into intelligent, interactive digital workflows that enable real-time execution tracking, quality control, and regulatory compliance.

### Key Features

- 🏭 **Digital Work Instructions**: Convert paper-based SOPs into interactive digital workflows
- 📊 **Real-time Tracking**: Monitor process execution with live dashboards and analytics
- ✅ **Quality Control**: Built-in verification, peer review, and approval workflows
- 📋 **Compliance Ready**: Supports FDA 21 CFR Part 11, SOX, and GMP compliance requirements
- 🔄 **Workflow Engine**: Flexible process definition with conditional logic and branching
- 📱 **Multi-platform**: Web-based interface with responsive design
- 🔐 **Enterprise Security**: Role-based access control and audit trails

### Business Impact

- **95% reduction** in report creation time
- **99% reduction** in manual errors
- **70% faster** audit preparation
- **ROI of 300%+** within 2 years

## Architecture

The DWI platform follows a modern microservices architecture:

```
┌─────────────────────┐    ┌─────────────────────┐
│    Frontend         │    │    Backend          │
│   React/TypeScript  │◄──►│   Spring Boot/Java  │
│   Redux State Mgmt  │    │   REST APIs         │
└─────────────────────┘    └─────────────────────┘
                                      │
                           ┌─────────────────────┐
                           │    Data Layer       │
                           │  PostgreSQL + Mongo │
                           │  Quartz Scheduler   │
                           └─────────────────────┘
```

### Technology Stack

#### Backend
- **Framework**: Spring Boot 2.7.2
- **Language**: Java 17
- **Database**: PostgreSQL 11 (primary), MongoDB 5.0 (ontology)
- **Scheduler**: Quartz
- **Authentication**: JAaS (Java as a Service)
- **Documentation**: Swagger/OpenAPI

#### Frontend
- **Framework**: React 18.x
- **Language**: TypeScript 4.x
- **State Management**: Redux with Redux-Saga
- **UI Components**: Material-UI (MUI) v4
- **Forms**: React Hook Form
- **PDF Generation**: React-PDF
- **Build Tool**: Webpack

#### Infrastructure
- **Containerization**: Docker
- **Reverse Proxy**: Nginx
- **Monitoring**: New Relic, Prometheus
- **Message Queue**: AWS SQS
- **File Storage**: Local/CDN
- **Email**: SMTP integration

## Quick Start

### Prerequisites

- **Java 17** or higher
- **Node.js 16** or higher
- **PostgreSQL 11** or higher
- **MongoDB 5.0** or higher
- **Docker** (optional, for containerized deployment)

### 1. Clone the Repository

```bash
git clone https://github.com/mridul-git99/DWI.git
cd DWI
```

### 2. Backend Setup

```bash
cd streem-backend-platform-develop

# Configure database connection
cp config/application.properties.example config/application.properties
# Edit application.properties with your database credentials

# Build and run
./gradlew build
./gradlew bootRun
```

The backend will start on `http://localhost:8080`

### 3. Frontend Setup

```bash
cd streem-frontend-platform-develop

# Install dependencies
npm install

# Start development server
npm start
```

The frontend will start on `http://localhost:3000`

### 4. Docker Setup (Alternative)

```bash
# Build backend Docker image
cd streem-backend-platform-develop
docker build -t dwi-backend .

# Build frontend Docker image
cd ../streem-frontend-platform-develop
./docker/script.sh

# Run with docker-compose
docker-compose up -d
```

## Configuration

### Environment Variables

```bash
# Database
DATABASE_HOST=localhost
DATABASE_PORT=5432
DATABASE_NAME=dwi
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=postgres

# MongoDB
MONGODB_HOST=localhost
MONGODB_PORT=27017
MONGODB_DATABASE=ontology

# JAaS Service
JAAS_ROOT=http://localhost:9091
JAAS_SERVICE_ID=your-service-id

# Email Configuration
EMAIL_HOST=smtp.gmail.com
EMAIL_PORT=587
EMAIL_USERNAME=your-email
EMAIL_PASSWORD=your-password

# File Storage
MEDIAS_LOCATION=/tmp
MEDIAS_CDN=http://your-cdn-url

# Frontend Backend URL
BACKEND_URL=http://localhost:8080/v1
```

### Database Setup

1. **PostgreSQL**: Create database and run migrations
2. **MongoDB**: Set up for ontology data storage
3. **Quartz**: Job scheduling tables (auto-created)

Detailed schema documentation is available in [SCHEMA_DOCUMENTATION.md](./SCHEMA_DOCUMENTATION.md).

## Development

### Building the Application

#### Backend
```bash
cd streem-backend-platform-develop
./gradlew clean build
```

#### Frontend
```bash
cd streem-frontend-platform-develop
npm run build
```

### Running Tests

#### Backend
```bash
./gradlew test
```

#### Frontend
```bash
npm test
```

### Code Quality

#### Backend
- **Checkstyle**: Java code style validation
- **SpotBugs**: Static analysis
- **JaCoCo**: Test coverage

#### Frontend
- **ESLint**: TypeScript/JavaScript linting
- **Prettier**: Code formatting
- **TypeScript**: Type checking

```bash
# Frontend linting and formatting
npm run lint:check
npm run prettier:check
npm run ts:check
```

## API Documentation

### Backend APIs
- **Swagger UI**: `http://localhost:8080/api-docs.html`
- **OpenAPI Spec**: `http://localhost:8080/v3/api-docs`

### Key API Endpoints
- `/v1/auth/*` - Authentication and authorization
- `/v1/checklists/*` - Work instruction templates
- `/v1/jobs/*` - Work instruction executions
- `/v1/users/*` - User management
- `/v1/facilities/*` - Facility management

## Deployment

### Production Deployment

#### Docker Deployment
```bash
# Build production images
docker build -t dwi-backend:latest ./streem-backend-platform-develop
docker build -t dwi-frontend:latest ./streem-frontend-platform-develop

# Deploy with docker-compose
docker-compose -f docker-compose.prod.yml up -d
```

#### Traditional Deployment
1. **Backend**: Deploy JAR file to application server
2. **Frontend**: Build static assets and serve with Nginx
3. **Database**: Set up PostgreSQL and MongoDB clusters
4. **Monitoring**: Configure New Relic and Prometheus

### Environment-specific Configuration
- **Development**: Local database, hot reloading
- **Staging**: Staging database, production-like setup
- **Production**: Clustered database, CDN, monitoring

## Documentation

### Architecture Documentation
- [Backend README](./streem-backend-platform-develop/README.md) - Comprehensive backend documentation
- [Frontend README](./streem-frontend-platform-develop/README.md) - Frontend-specific documentation
- [Schema Documentation](./SCHEMA_DOCUMENTATION.md) - Complete database schema and data models

### Development Guides
- [Backend Development Guide](./streem-backend-platform-develop/docs/) - Layer-by-layer documentation
- [Frontend User Stories](./streem-frontend-platform-develop/development/) - Feature implementations
- [API Documentation](http://localhost:8080/api-docs.html) - Interactive API documentation

## Contributing

### Development Workflow
1. **Fork** the repository and create a feature branch
2. **Set up** local development environment following the Quick Start guide
3. **Follow** coding standards and architectural patterns
4. **Write** comprehensive tests for new features
5. **Update** documentation for API changes

### Code Style Guidelines
- **Java**: Follow Google Java Style Guide
- **TypeScript**: Use ESLint and Prettier configurations
- **Database**: Use snake_case for tables and columns
- **API Design**: Follow RESTful conventions

### Pull Request Process
1. Ensure all tests pass and code coverage is maintained
2. Update API documentation for endpoint changes
3. Add database migrations for schema changes
4. Update relevant documentation

## Support

### Getting Help
- **📖 Documentation**: Start with this README and linked documentation
- **🐛 Issues**: Report bugs via GitHub issues
- **💡 Questions**: Check existing issues or create new ones
- **🔧 Technical Support**: Contact the development team

### Troubleshooting
- **Build Issues**: Check Java/Node.js versions and dependencies
- **Database Issues**: Verify connection strings and database setup
- **Performance**: Review query optimization in repository documentation

## License

This project is proprietary software. See [LICENSE](./LICENSE) for details.

## Team

**Development Team:**
- Snehal Seth - snehal.seth@leucinetech.com
- Ashish Choudhary - ashish.c@leucinetech.com  
- Sathyam Lokare - sathyam.lokare@leucinetech.com

---

**Version**: 4.9.11  
**Last Updated**: January 2025  
**Maintained by**: Leucine Technologies
