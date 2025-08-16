# Forex Rate Aggregator Service

A robust Spring Boot microservice that aggregates forex rates from multiple public APIs, applies markup, and provides authenticated access to current and historical exchange rates for Wiremit.

## 🚀 Features

- **Multi-source Rate Aggregation**: Fetches rates from ExchangeRate-API, Fixer, and CurrencyLayer
- **JWT Authentication**: Secure user registration and login
- **Redis Caching**: High-performance caching for frequently accessed rates
- **Scheduled Updates**: Automatic rate refresh every hour
- **Historical Data**: Track and retrieve historical rate information
- **PostgreSQL Storage**: Persistent storage with optimized indexes
- **Docker Support**: Complete containerization with Docker Compose
- **Comprehensive Testing**: Unit and integration tests with high coverage
- **Production Ready**: Health checks, monitoring, and security headers

## 🏗️ Architecture

### Component Structure
```
┌─────────────────────────────────────────────────────────────┐
│                    Forex Rate Aggregator                    │
├─────────────────────────────────────────────────────────────┤
│  Controllers (REST API)                                    │
│  ├─ AuthController (/api/v1/auth)                         │
│  └─ ForexRateController (/api/v1/rates)                   │
├─────────────────────────────────────────────────────────────┤
│  Services (Business Logic)                                 │
│  ├─ AuthService (JWT + User Management)                   │
│  ├─ ForexAggregationService (Rate Processing)             │
│  ├─ ExternalForexApiService (External APIs)               │
│  └─ CacheService (Redis Management)                       │
├─────────────────────────────────────────────────────────────┤
│  Data Layer                                                │
│  ├─ JPA Repositories (PostgreSQL)                         │
│  ├─ Redis Cache                                           │
│  └─ External APIs (HTTP Clients)                          │
└─────────────────────────────────────────────────────────────┘
```

### Data Flow
1. **Authentication**: Users register/login → JWT token issued
2. **Rate Request**: Authenticated request → Check cache → Return or fetch from APIs
3. **Aggregation**: Multiple API responses → Calculate average → Apply markup
4. **Storage**: Save to PostgreSQL → Cache in Redis
5. **Scheduling**: Hourly background job updates all rates

## 📋 Requirements Met

### 1. Authentication and Authorization ✅
- **JWT-based authentication** with secure token generation and validation
- **User registration** with validation and password encryption (BCrypt)
- **Protected endpoints** requiring valid JWT tokens
- **Role-based access** (USER/ADMIN roles)

**Justification**: JWT tokens provide stateless authentication, perfect for microservices. They're scalable, secure, and include user claims for authorization.

### 2. Forex Rate Aggregation ✅
- **Three external APIs**: ExchangeRate-API, Fixer, CurrencyLayer
- **Supported pairs**: USD-GBP, USD-ZAR, ZAR-GBP (configurable)
- **Average calculation** from multiple sources with configurable markup (0.10)
- **Error handling** for API failures with fallback mechanisms

### 3. Persistent Storage ✅
- **PostgreSQL database** with optimized schemas and indexes
- **Timestamped records** for all rate entries
- **Hourly refresh** via Spring's `@Scheduled` annotation
- **Historical data retention** with pagination support

### 4. Required Endpoints ✅
- `POST /api/v1/auth/signup` - User registration
- `POST /api/v1/auth/login` - User authentication
- `GET /api/v1/rates` - Get all current rates
- `GET /api/v1/rates/{currency}` - Get specific currency rate
- `GET /api/v1/historical/rates` - Get historical rates with pagination

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.6+
- Docker & Docker Compose
- PostgreSQL 15+ (if running locally)
- Redis 7+ (if running locally)

### Running with Docker (Recommended)

1. **Clone the repository**
```bash
git clone <repository-url>
cd forexrateaggregator
```


3. **Start all services**
```bash
docker-compose up -d
```

4. **Verify deployment**
```bash
curl http://localhost:8989/api/v1/auth/health
```

### Local Development Setup

1. **Start dependencies**
```bash
# PostgreSQL
docker run -d \
  --name postgres \
  -e POSTGRES_DB=forexratedb \
  -e POSTGRES_USER=root \
  -e POSTGRES_PASSWORD=Mupezeni0102? \
  -p 5432:5432 \
  postgres:15-alpine

# Redis
docker run -d \
  --name redis \
  -p 6379:6379 \
  redis:7-alpine
```

2. **Configure application**
```bash
# Copy and edit application properties
cp src/main/resources/application.yml.example src/main/resources/application.yml
# Add your API keys to application.yml
```

3. **Build and run**
```bash
./mvnw clean package
./mvnw spring-boot:run
```

## 🔑 API Documentation

### Authentication Flow

#### 1. User Registration
```bash
curl -X POST http://localhost:8989/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "email": "john@example.com",
    "password": "securepassword123",
    "first_name": "John",
    "last_name": "Doe"
  }'
```

**Response:**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "expires_in": 86400000,
  "user_info": {
    "id": 1,
    "username": "johndoe",
    "email": "john@example.com",
    "first_name": "John",
    "last_name": "Doe",
    "role": "USER",
    "created_at": "2024-01-15T10:30:00"
  }
}
```

#### 2. User Login
```bash
curl -X POST http://localhost:8989/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "password": "securepassword123"
  }'
```

### Rate Aggregation Logic

The service implements a sophisticated rate aggregation algorithm:

1. **Parallel API Calls**: Simultaneously fetches rates from all configured APIs
2. **Data Validation**: Validates and filters successful responses
3. **Average Calculation**: Computes arithmetic mean of all valid rates
4. **Markup Application**: Adds configured markup (10% default) to average rate
5. **Persistence**: Stores both raw average and customer-facing rate

#### Rate Calculation Formula
```
Customer Rate = Average Rate × (1 + Markup)
Where Markup = 0.10 (10%)

Example:
- API 1: 0.75 USD/GBP
- API 2: 0.74 USD/GBP  
- API 3: 0.76 USD/GBP
- Average: 0.75 USD/GBP
- Customer Rate: 0.75 × 1.10 = 0.825 USD/GBP
```

### Forex Rate Endpoints

#### Get All Current Rates
```bash
curl -X GET http://localhost:8989/api/v1/rates \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Response:**
```json
{
  "rates": [
    {
      "currency_pair": "USD-GBP",
      "base_currency": "USD",
      "target_currency": "GBP",
      "customer_rate": 0.825000,
      "average_rate": 0.750000,
      "markup": 0.10,
      "markup_amount": 0.075000,
      "api_sources": ["ExchangeRate-API", "Fixer"],
      "source_count": 2,
      "timestamp": "2024-01-15T10:30:00",
      "display_name": "USD to GBP"
    }
  ],
  "last_updated": "2024-01-15T10:30:00",
  "total_pairs": 3,
  "base_currency": "USD"
}
```

#### Get Specific Currency Rate
```bash
curl -X GET http://localhost:8989/api/v1/rates/GBP \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### Get Historical Rates
```bash
curl -X GET "http://localhost:8989/api/v1/historical/rates?currency=GBP&days=7&page=0&size=50" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## 🛡️ Security Considerations

### Authentication Security
- **BCrypt Password Hashing**: Industry-standard password encryption
- **JWT Secret Management**: Configurable secret keys with environment variables
- **Token Expiration**: 24-hour token lifetime with configurable expiration
- **Role-based Authorization**: Granular permission control

### API Security
- **Rate Limiting**: Nginx-based request throttling (10 req/sec)
- **CORS Configuration**: Configurable cross-origin policies
- **Security Headers**: XSS protection, frame options, content type validation
- **Input Validation**: Comprehensive DTO validation with custom constraints

### Infrastructure Security
- **Non-root Containers**: All Docker containers run as non-privileged users
- **Network Isolation**: Docker network segmentation
- **Health Checks**: Automated container health monitoring
- **SSL/TLS Ready**: Nginx configuration for HTTPS termination

## 📊 Scaling Considerations

### Horizontal Scaling
The application is designed for horizontal scaling:

1. **Stateless Design**: JWT tokens eliminate session dependencies
2. **Database Connection Pooling**: HikariCP with configurable pool sizes
3. **Redis Clustering**: Support for Redis cluster mode
4. **Load Balancing**: Nginx upstream configuration for multiple instances

### Performance Optimizations
- **Redis Caching**: Multi-tier caching strategy with TTL management
- **Database Indexing**: Optimized indexes on frequently queried columns
- **Async Processing**: Non-blocking external API calls
- **Connection Reuse**: HTTP client connection pooling

### Monitoring & Observability
- **Spring Actuator**: Health checks and metrics endpoints
- **Application Metrics**: JVM, database, and custom business metrics
- **Structured Logging**: JSON-formatted logs with correlation IDs
- **Health Checks**: Comprehensive dependency health monitoring

## 🧪 Testing Strategy

### Test Coverage
- **Unit Tests**: Service layer business logic (80%+ coverage)
- **Integration Tests**: Controller and repository testing
- **Security Tests**: Authentication and authorization flows
- **Performance Tests**: Load testing for critical endpoints

### Running Tests
```bash
# Unit tests only
./mvnw test

# Integration tests
./mvnw test -Dspring.profiles.active=test

# With coverage report
./mvnw test jacoco:report
```

## 🔧 Configuration

### Application Properties
Key configuration options in `application.yml`:

```yaml
# JWT Configuration
jwt:
  secret: ${JWT_SECRET:default-secret}
  expiration: 86400000

# Forex API Configuration
forex:
  markup: 0.10
  refresh-interval: 3600000
  currencies:
    base: USD
    targets: 
      - GBP
      - ZAR

# External API Keys
forex:
  apis:
    fixer-api:
      access-key: ${FIXER_API_KEY}
    currencylayer-api:
      access-key: ${CURRENCYLAYER_API_KEY}
```

### Environment Variables
- `DB_USERNAME`: Database username
- `DB_PASSWORD`: Database password
- `REDIS_HOST`: Redis server hostname
- `JWT_SECRET`: JWT signing secret
- `FIXER_API_KEY`: Fixer.io API key
- `CURRENCYLAYER_API_KEY`: CurrencyLayer API key

## 📈 Monitoring & Health Checks

### Health Check Endpoints
- `GET /actuator/health` - Overall application health
- `GET /api/v1/auth/health` - Authentication service health
- `GET /api/v1/rates/health` - Rates service health

### Metrics
- Database connection pool status
- Redis connection health
- External API response times
- Cache hit/miss ratios
- JWT token validation metrics

## 🚀 Deployment

### Production Considerations
1. **Environment Separation**: Use different profiles for dev/staging/prod
2. **Secret Management**: Use proper secret management solutions
3. **Database Migration**: Use Flyway or Liquibase for schema management
4. **SSL Termination**: Configure proper SSL certificates
5. **Backup Strategy**: Regular database and Redis backups

### CI/CD Pipeline
The project includes GitHub Actions workflow for:
- Automated testing
- Docker image building
- Security scanning
- Deployment automation

## 🤝 Contributing

### Development Guidelines
1. Follow Spring Boot best practices
2. Maintain test coverage above 80%
3. Use conventional commit messages
4. Update documentation for new features

### Code Quality
- CheckStyle configuration for consistent formatting
- SonarQube integration for code quality analysis
- Automated dependency vulnerability scanning
**