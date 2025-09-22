# Streem - Digital Work Instructions Platform

[![Version](https://img.shields.io/badge/version-4.9.11-blue.svg)](https://github.com/leucinetech/streem-backend)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.java.net/projects/jdk/17/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-11-blue.svg)](https://www.postgresql.org/)
[![MongoDB](https://img.shields.io/badge/MongoDB-5.0-green.svg)](https://www.mongodb.com/)

## Overview

**Streem** is an enterprise-grade **Digital Work Instructions (DWI)** platform designed for manufacturing and industrial process management. It transforms traditional paper-based standard operating procedures (SOPs) into intelligent, interactive digital workflows that enable real-time execution tracking, quality control, and regulatory compliance.

### Business Impact
- **95% reduction** in report creation time
- **99% reduction** in manual errors
- **70% faster** audit preparation
- **ROI of 300%+** within 2 years
- Supports FDA 21 CFR Part 11, SOX, and GMP compliance

## Architecture Overview

Streem follows a robust **N-Tier Architecture** designed for enterprise scalability, maintainability, and performance:

```
┌─────────────────────┐
│   Presentation      │  REST APIs, DTOs, Controllers
│      Layer          │  (docs/presentation-layer.md)
├─────────────────────┤
│   Business/Service  │  Workflow Engine, Business Logic
│      Layer          │  (docs/business-layer.md)
├─────────────────────┤
│   Data Access       │  Repositories, JPA Entities
│      Layer          │  (docs/data-access-layer/)
├─────────────────────┤
│   Data Model        │  131 JPA Entities, Relationships
│      Layer          │  (docs/data-model-layer.md)
├─────────────────────┤
│   Database Layer    │  PostgreSQL + MongoDB
│                     │  (docs/database-layer.md)
├─────────────────────┤
│   Integration       │  External APIs, Message Queues
│      Layer          │  (docs/integration-layer.md)
├─────────────────────┤
│   Cross-cutting     │  Security, Logging, Caching
│   Concerns          │  (docs/cross-cutting-concerns.md)
└─────────────────────┘
```

> **📚 Comprehensive Architecture Documentation**: Each layer is extensively documented with implementation details, design patterns, database schemas, and Hibernate removal strategies.

### 🏗️ **Documentation Structure**

Our documentation follows a systematic approach for understanding and migrating the entire system:

#### **📊 Data Model Layer** - *Complete (131 entities)*
- **[Data Model Documentation](docs/data-model-layer.md)** - Complete analysis of all 131 JPA entities
- **Entity Relationships** - Comprehensive mapping of all database relationships
- **Business Domain Context** - Understanding entities in workflow management context
- **Database Schema Implications** - Index strategies, constraints, and performance considerations

#### **🔌 Data Access Layer** - *In Progress (3/88 repositories)*
- **[Repository Documentation](docs/data-access-layer/repositories/)** - Ultra-detailed analysis for Hibernate removal
  - ✅ **[ChecklistRepository.md](docs/data-access-layer/repositories/ChecklistRepository.md)** - 2,500+ lines, 9 methods
  - ✅ **[JobRepository.md](docs/data-access-layer/repositories/JobRepository.md)** - 1,900+ lines, 16 methods  
  - ✅ **[ParameterRepository.md](docs/data-access-layer/repositories/ParameterRepository.md)** - 2,500+ lines, 24 methods
  - 🔄 **TaskRepository.md** - Next priority (workflow task management)
  - 📋 **85 more repositories** - Systematic documentation in progress

**Each repository includes:**
- Method-by-method SQL analysis and DAO conversion code
- Database execution plans and performance optimization
- JSONB query strategies with GIN indexing
- Complete transaction management and error handling
- Comprehensive test strategies for DAO validation

#### **🎯 Hibernate Removal Project**

Our systematic approach to replacing Hibernate with custom DAOs:

- **Current Progress**: 3 of 88 repositories documented (~7,000 lines)
- **Completion Rate**: 3.4% with ultra-detailed analysis
- **Methodology**: Method-level SQL analysis with production-ready DAO code
- **Performance Focus**: Optimized queries, proper indexing, and caching strategies
- **Testing Coverage**: Comprehensive test suites for each converted repository

## Core Business Domains

### 🔄 Workflow Management
- **Checklists**: Digital process templates with stages and tasks
- **Jobs**: Execution instances of checklists for specific operations
- **Tasks**: Individual work steps with defined parameters and validations
- **Parameters**: Data collection points (measurements, selections, media uploads)

### ✅ Quality Control & Compliance
- **Parameter Verification**: Peer and self-verification workflows
- **Exception Handling**: Structured management of out-of-specification values
- **Corrections**: Error correction mechanisms with audit trails
- **Automation**: Trigger-based actions and process automation

### 👥 Organizational Structure
- **Multi-tenant**: Organization and facility-based data segregation
- **Role-based Access**: Granular permissions for different user types
- **Use Cases**: Process categorization by production lines or equipment types

## Technology Stack

### Core Framework
- **Java 17** - Modern LTS Java with enhanced performance
- **Spring Boot 2.7.2** - Enterprise application framework
- **Spring Security** - Authentication and authorization
- **Spring Data JPA** - Database abstraction layer

### Data Persistence
- **PostgreSQL 11+** - Primary relational database for structured data
- **MongoDB 5.0+** - Document storage for flexible collections and logs
- **Liquibase** - Database schema versioning and migration
- **HikariCP** - High-performance connection pooling

### Integration & Communication
- **REST APIs** - RESTful web services with OpenAPI documentation
- **JWT Tokens** - Stateless authentication
- **Quartz Scheduler** - Job scheduling and recurring tasks
- **Apache POI** - Excel file processing

### Development & Operations
- **Gradle** - Build automation and dependency management
- **Docker** - Containerization and deployment
- **Caffeine** - High-performance caching
- **Logback** - Structured logging with configurable outputs

## Quick Start

### Prerequisites
- **Java 17+** (OpenJDK recommended)
- **PostgreSQL 11+** with database `dwi`
- **MongoDB 5.0+** with replica set configured
- **Gradle 7.0+** (or use included wrapper)

### Database Setup

#### PostgreSQL
```bash
# Create database
createdb dwi
createuser dwi_user --pwprompt

# Update application.properties
datasource.host=localhost
datasource.port=5432
datasource.database=dwi
datasource.username=dwi_user
datasource.password=your_password
```

#### MongoDB
```bash
# Start MongoDB with replica set
mongod --replSet rs0 --dbpath /data/db

# Initialize replica set
mongo --eval "rs.initiate()"

# Update application.properties
mongodb.host=localhost
mongodb.port=27017
mongodb.database=ontology
mongodb.replica-set=rs0
```

### Build and Run

```bash
# Clone repository
git clone <repository-url>
cd streem-backend/backend

# Build application
./gradlew clean build

# Run database migrations
./gradlew update

# Start application
./gradlew bootRun

# Application will be available at http://localhost:8080
# API documentation at http://localhost:8080/api-docs.html
```

### Docker Deployment

```bash
# Using provided Docker Compose
cd backend/src/main/docker
docker-compose up -d

# Application: http://localhost:8080
# Database: localhost:8081 (PostgreSQL)
```

## API Documentation

### Interactive API Explorer
- **Swagger UI**: `http://localhost:8080/api-docs.html`
- **OpenAPI Spec**: Auto-generated documentation for all endpoints

### Core API Endpoints

| Domain | Endpoint | Description |
|--------|----------|-------------|
| **Checklists** | `/api/checklists` | Process template management |
| **Jobs** | `/api/jobs` | Job execution and monitoring |
| **Tasks** | `/api/tasks` | Task execution and completion |
| **Parameters** | `/api/parameters` | Parameter definition and validation |
| **Users** | `/api/users` | User management and authentication |
| **Facilities** | `/api/facilities` | Multi-tenant facility management |

> **📋 Complete API Reference**: Detailed endpoint documentation available in `docs/api-reference.md`

## Development Workflow

### Build Commands
```bash
# Full build with tests
cd backend && ./gradlew build

# Run specific test suite
cd backend && ./gradlew test --tests "*JobServiceTest*"

# Generate test coverage report
cd backend && ./gradlew jacocoTestReport

# Code quality checks
cd backend && ./gradlew checkstyleMain
```

### 📚 **Working with Documentation**

#### **Understanding the System**
```bash
# Start with complete entity understanding
docs/data-model-layer.md                    # All 131 entities documented

# Study repository patterns for specific entities
docs/data-access-layer/repositories/ChecklistRepository.md    # Process templates
docs/data-access-layer/repositories/JobRepository.md          # Workflow execution  
docs/data-access-layer/repositories/ParameterRepository.md    # Data collection
```

#### **Database Development**
```bash
# Reference repository documentation for:
# - Exact SQL queries generated by Hibernate
# - Performance optimization strategies
# - Index requirements for optimal performance
# - JSONB operations with GIN indexing
# - Transaction management patterns

# Example: Understanding complex JSONB queries
grep -n "JSONB" docs/data-access-layer/repositories/ParameterRepository.md
```

#### **Repository Migration (Hibernate Removal)**
```bash
# Each repository documentation includes:
# 1. Method-by-method SQL analysis
# 2. Production-ready DAO conversion code
# 3. Database execution plans
# 4. Comprehensive test strategies
# 5. Performance optimization guidelines

# Progress tracking:
# ✅ ChecklistRepository.md  - 2,500+ lines (9 methods)
# ✅ JobRepository.md        - 1,900+ lines (16 methods) 
# ✅ ParameterRepository.md  - 2,500+ lines (24 methods)
# 🔄 85 more repositories in systematic documentation
```

### Database Operations
```bash
# Apply database migrations
./gradlew update

# Generate changelog from existing schema
./gradlew generateChangelog

# Rollback last migration
./gradlew rollback -PliquibaseCommandValue=1

# Database diff between environments
./gradlew diff
```

### Development Server
```bash
# Run with live reload
./gradlew bootRun --args="--spring.profiles.active=dev"

# Run with custom configuration
./gradlew bootRun --args="--spring.config.location=file:./config/application-local.properties"

# Debug mode (port 5005)
./gradlew bootRun --debug-jvm
```

## Configuration

### Environment Variables
```bash
# Database Configuration
export DATASOURCE_HOST=localhost
export DATASOURCE_PORT=5432
export DATASOURCE_DATABASE=dwi
export DATASOURCE_USERNAME=dwi_user
export DATASOURCE_PASSWORD=secure_password

# MongoDB Configuration
export MONGODB_HOST=localhost
export MONGODB_PORT=27017
export MONGODB_DATABASE=ontology

# External Service Configuration
export JAAS_ROOT=https://auth.your-domain.com
export AWS_ACCESS_KEY=your_aws_key
export AWS_SECRET_KEY=your_aws_secret
```

### Configuration Files
- **`application.properties`** - Main application configuration
- **`application-{profile}.properties`** - Environment-specific overrides
- **`liquibase.properties`** - Database migration configuration

## Testing Strategy

### Test Pyramid
```
┌─────────────────────────────────┐
│        E2E Tests (Few)          │  Integration tests
├─────────────────────────────────┤
│     Integration Tests (Some)    │  Service layer tests
├─────────────────────────────────┤
│      Unit Tests (Many)          │  Component tests
└─────────────────────────────────┘
```

### Running Tests
```bash
# All tests
./gradlew test

# Unit tests only
./gradlew test --tests "*Test*" --exclude-task integrationTest

# Integration tests
./gradlew integrationTest

# Test coverage report
./gradlew jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

## Monitoring & Observability

### Health Checks
- **Application Health**: `/actuator/health`
- **Database Health**: Automatic connection validation
- **External Dependencies**: JAAS service, MongoDB connectivity

### Logging
```bash
# Application logs
tail -f /tmp/log/leucine/dwi/spring.log

# SQL query logging (dev only)
logging.level.org.hibernate.SQL=DEBUG

# Custom log levels
logging.level.com.leucine.streem=DEBUG
```

### Metrics & Monitoring
- **Micrometer Metrics**: `/actuator/metrics`
- **JVM Metrics**: Memory, GC, thread pools
- **Business Metrics**: Job execution rates, parameter validation

## Security

### Authentication & Authorization
- **JWT Token-based Authentication** via JAAS service
- **Role-based Access Control (RBAC)** with granular permissions
- **Multi-tenant Security** with facility-level data isolation

### Security Headers
- **CORS Configuration** for cross-origin requests
- **XSS Protection** via OWASP Java HTML Sanitizer
- **Input Validation** using Bean Validation (JSR-303)

### Data Protection
- **Encryption at Rest** for sensitive data
- **Audit Trails** for all data modifications
- **Data Retention Policies** for compliance requirements

## Performance Optimization

### Caching Strategy
- **Application-level Caching** using Caffeine
- **Database Query Optimization** with proper indexing
- **Connection Pooling** with HikariCP

### Scalability Features
- **Stateless Architecture** for horizontal scaling
- **Database Connection Pooling** with configurable pool sizes
- **Asynchronous Processing** for background tasks

## Contributing

### Development Setup
1. **Fork the repository** and create a feature branch
2. **Set up local development environment** following Quick Start guide
3. **Follow coding standards** and architectural patterns
4. **Write comprehensive tests** for new features
5. **Update documentation** for API changes

### Code Style Guidelines
- **Java Code Style**: Follow Google Java Style Guide
- **Database Naming**: Use snake_case for tables and columns
- **API Design**: Follow RESTful conventions and OpenAPI standards
- **Error Handling**: Use structured exception handling with proper HTTP status codes

### Pull Request Process
1. Ensure all tests pass and code coverage is maintained
2. Update API documentation for endpoint changes
3. Add database migrations for schema changes
4. Update relevant documentation in `docs/` folder

## Production Deployment

### Environment Requirements
- **Java Runtime**: OpenJDK 17+ with sufficient heap memory
- **Database**: PostgreSQL 11+ with proper backup strategy
- **Document Store**: MongoDB 5.0+ with replica set configuration
- **Load Balancer**: For high availability and traffic distribution

### Deployment Checklist
- [ ] Environment-specific configuration files
- [ ] Database migrations applied
- [ ] External service connectivity verified
- [ ] Security certificates installed
- [ ] Monitoring and alerting configured
- [ ] Backup and disaster recovery procedures tested

### Performance Tuning
```properties
# JVM Options
-Xms2g -Xmx4g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200

# Database Connection Pool
datasource.pool-size=20
spring.jpa.properties.hibernate.jdbc.batch_size=50

# Application Performance
spring.jpa.properties.hibernate.cache.use_second_level_cache=true
server.compression.enabled=true
```

## License

Copyright © 2024 Leucine Technologies. All rights reserved.

## Support & Documentation

### 📚 **Complete Architecture Documentation**

#### **Data Layer Documentation**
- **[📊 Data Model Layer](docs/data-model-layer.md)** - Complete entity analysis (131 entities documented)
- **[🔌 Data Access Layer](docs/data-access-layer/repositories/)** - Repository documentation for Hibernate removal
  - **[ChecklistRepository](docs/data-access-layer/repositories/ChecklistRepository.md)** - Process template management
  - **[JobRepository](docs/data-access-layer/repositories/JobRepository.md)** - Workflow execution tracking  
  - **[ParameterRepository](docs/data-access-layer/repositories/ParameterRepository.md)** - Data collection system
  - **[85+ more repositories](docs/data-access-layer/repositories/)** - Systematic documentation in progress

#### **Development Resources**
- **🔧 API Reference**: Interactive Swagger UI at `/api-docs.html`
- **🗄️ Database Schema**: Comprehensive entity relationships and indexes
- **🚀 Deployment Guide**: Production deployment procedures with Docker
- **🧪 Testing Strategy**: Repository-level test suites and coverage reports
- **⚡ Performance Tuning**: Query optimization and caching strategies

#### **Hibernate Removal Project**
- **📋 Project Status**: 3 of 88 repositories documented (~7,000 lines)
- **🎯 Methodology**: Ultra-detailed SQL analysis with DAO conversion examples
- **🔍 Code Examples**: Production-ready DAO implementations with error handling
- **📈 Performance Focus**: Optimized queries, indexing strategies, and caching

### 🚀 **Developer Onboarding**

1. **Start with Architecture**: Read [Data Model Layer](docs/data-model-layer.md) for complete entity understanding
2. **Explore Repositories**: Review documented repositories for data access patterns
3. **Understand Workflows**: Study Checklist → Job → Task → Parameter relationships
4. **Database Knowledge**: Learn JSONB operations and GIN indexing strategies
5. **Testing Approach**: Follow comprehensive test strategies for each layer

### 🆘 **Getting Help**

- **📖 Documentation**: Start with the `docs/` folder for comprehensive guides
- **🐛 Issues**: Report bugs and feature requests via GitHub issues
- **💡 Architecture Questions**: Refer to layer-specific documentation
- **🔧 Database Issues**: Check repository documentation for query optimization
- **📊 Performance**: Review execution plans and indexing strategies in repository docs

For technical support and questions, please contact the development team or create an issue in the project repository.