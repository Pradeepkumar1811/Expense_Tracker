# Design Document

## Overview

This document defines the technical architecture for the Personal Expense & Subscription Tracker MVP. The system is a full-stack web application with a Spring Boot 3.x REST API backend, MySQL 8 database, Redis caching layer, and a React single-page application frontend, all containerized with Docker Compose for local development.

## Architecture

The application follows a layered architecture pattern:

```
┌──────────────────────────────────────────────────────────┐
│                   React SPA (Port 3000)                   │
│         Login | Dashboard | Transactions | Reports        │
└──────────────────────┬───────────────────────────────────┘
                       │ HTTP/REST + JWT Bearer Token
┌──────────────────────▼───────────────────────────────────┐
│              Spring Boot API (Port 8080)                  │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────────┐  │
│  │ Controllers │→ │   Services   │→ │  Repositories  │  │
│  └─────────────┘  └──────────────┘  └────────────────┘  │
│  ┌─────────────┐  ┌──────────────┐                       │
│  │  Security   │  │ Cache Layer  │                       │
│  │  (JWT)      │  │  (Redis)     │                       │
│  └─────────────┘  └──────────────┘                       │
└───────────┬──────────────────┬───────────────────────────┘
            │                  │
   ┌────────▼────────┐  ┌─────▼──────┐
   │   MySQL 8       │  │   Redis    │
   │   (Port 3306)   │  │ (Port 6379)│
   └─────────────────┘  └────────────┘
```

## Components and Interfaces

### Backend (Spring Boot 3.x)

#### Security Layer

- **JwtTokenProvider**: Generates and validates JWT tokens. Encodes user ID and expiration into the payload. Uses HS256 signing with a configurable secret key.
- **JwtAuthenticationFilter**: Servlet filter that intercepts requests, extracts the Bearer token from the Authorization header, validates it, and sets the SecurityContext.
- **SecurityConfig**: Configures Spring Security to permit `/api/auth/**` endpoints and require authentication for all others.

#### Controller Layer

| Controller | Base Path | Responsibility |
|---|---|---|
| AuthController | `/api/auth` | Registration and login |
| TransactionController | `/api/transactions` | CRUD for income/expense records |
| CategoryController | `/api/categories` | Default + custom category management |
| BudgetController | `/api/budgets` | Monthly budget CRUD and status |
| SubscriptionController | `/api/subscriptions` | Subscription CRUD |
| ReminderController | `/api/reminders` | Reminder listing and status updates |
| ReportController | `/api/reports` | Monthly spending report generation |
| ExportController | `/api/export` | CSV export |
| DashboardController | `/api/dashboard` | Dashboard summary aggregation |

#### Service Layer

| Service | Responsibility |
|---|---|
| AuthService | User registration (bcrypt hashing), credential validation, JWT issuance |
| TransactionService | Transaction CRUD with ownership validation |
| CategoryService | Category retrieval (defaults + custom), custom category creation with duplicate detection |
| BudgetService | Budget CRUD, spending calculation, overspent detection |
| SubscriptionService | Subscription CRUD, billing cycle validation |
| ReminderService | Reminder generation (scheduled), retrieval, mark-as-read |
| ReportService | Monthly report computation (income, expenses, net, category breakdown) |
| ExportService | CSV generation with date range filtering |
| DashboardService | Aggregates dashboard data from transactions, subscriptions, budgets |
| CacheService | Redis cache read/write/invalidation with fallback to direct DB access |

#### Repository Layer

Spring Data JPA repositories for each entity, extending `JpaRepository` with custom query methods for pagination, date-range filtering, and user-scoped queries.

#### Scheduled Tasks

- **ReminderGenerationJob**: Runs daily (configurable via `@Scheduled` cron expression). Queries subscriptions with `nextRenewalDate` within 3 days and generates reminders if not already created for that renewal cycle.

### Frontend (React SPA)

#### Component Structure

```
src/
├── components/
│   ├── auth/          LoginForm, RegisterForm
│   ├── dashboard/     DashboardView, StatCard, UpcomingRenewals
│   ├── transactions/  TransactionList, TransactionForm, ExportButton
│   ├── categories/    CategoryList, CategoryForm
│   ├── budgets/       BudgetList, BudgetForm, BudgetStatus
│   ├── subscriptions/ SubscriptionList, SubscriptionForm
│   ├── reminders/     ReminderList, ReminderItem
│   └── reports/       ReportView, CategoryBreakdown
├── services/          API client with axios interceptors
├── context/           AuthContext (JWT storage + refresh logic)
├── hooks/             Custom hooks for data fetching
└── utils/             Date formatting, currency formatting
```

#### Authentication Flow

1. User submits login credentials via LoginForm
2. AuthService calls `/api/auth/login`
3. On success, JWT stored in `localStorage`
4. Axios interceptor attaches `Authorization: Bearer <token>` to all requests
5. On 401 response, interceptor clears stored token and redirects to login

### Interfaces

#### Authentication Endpoints

```java
// POST /api/auth/register
record RegisterRequest(
    @Email String email,
    @Size(min = 8) String password
) {}

record RegisterResponse(Long id, String email) {}

// POST /api/auth/login
record LoginRequest(String email, String password) {}

record LoginResponse(String accessToken, Long expiresIn) {}
```

### Transaction Endpoints

```java
// POST /api/transactions
// PUT /api/transactions/{id}
record TransactionRequest(
    @NotNull BigDecimal amount,
    @NotNull TransactionType type,  // INCOME or EXPENSE
    @NotNull LocalDate date,
    String description,
    Long categoryId
) {}

record TransactionResponse(
    Long id, BigDecimal amount, TransactionType type,
    LocalDate date, String description, String categoryName,
    LocalDateTime createdAt
) {}

// GET /api/transactions?page=0&size=20
record PagedResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}
```

### Category Endpoints

```java
// POST /api/categories
record CategoryRequest(@NotBlank String name) {}

record CategoryResponse(Long id, String name, boolean isDefault) {}

// GET /api/categories → List<CategoryResponse>
```

### Budget Endpoints

```java
// POST /api/budgets
// PUT /api/budgets/{id}
record BudgetRequest(
    @NotNull Integer month,
    @NotNull Integer year,
    @NotNull BigDecimal limitAmount,
    Long categoryId  // null for overall monthly budget
) {}

record BudgetStatusResponse(
    Long id, Integer month, Integer year,
    BigDecimal limitAmount, BigDecimal totalSpending,
    BigDecimal remainingAmount, boolean overspent,
    BigDecimal overspentAmount, String categoryName
) {}

// GET /api/budgets?month=3&year=2024 → List<BudgetStatusResponse>
```

### Subscription Endpoints

```java
// POST /api/subscriptions
// PUT /api/subscriptions/{id}
record SubscriptionRequest(
    @NotBlank String name,
    @NotNull BigDecimal amount,
    @NotNull BillingCycle billingCycle,  // MONTHLY or ANNUAL
    @NotNull LocalDate nextRenewalDate
) {}

record SubscriptionResponse(
    Long id, String name, BigDecimal amount,
    BillingCycle billingCycle, LocalDate nextRenewalDate
) {}

// GET /api/subscriptions → List<SubscriptionResponse>
```

### Reminder Endpoints

```java
record ReminderResponse(
    Long id, String subscriptionName, LocalDate renewalDate,
    BigDecimal amount, boolean read, LocalDateTime createdAt
) {}

// GET /api/reminders → List<ReminderResponse> (unread, sorted by renewalDate ASC)
// PATCH /api/reminders/{id}/read → ReminderResponse
```

### Report Endpoints

```java
record MonthlyReportResponse(
    Integer month, Integer year,
    BigDecimal totalIncome, BigDecimal totalExpenses,
    BigDecimal netSavings,
    List<CategoryBreakdown> categoryBreakdown
) {}

record CategoryBreakdown(String categoryName, BigDecimal total) {}

// GET /api/reports?month=3&year=2024 → MonthlyReportResponse
```

### Dashboard Endpoint

```java
record DashboardResponse(
    BigDecimal currentMonthIncome,
    BigDecimal currentMonthExpenses,
    BigDecimal netBalance,
    int activeSubscriptionCount,
    BigDecimal totalSubscriptionCost,
    BigDecimal budgetUtilizationPercent,
    List<SubscriptionResponse> upcomingRenewals
) {}

// GET /api/dashboard → DashboardResponse
```

### Export Endpoint

```
GET /api/export/csv?startDate=2024-01-01&endDate=2024-03-31
Response: Content-Type: text/csv, Content-Disposition: attachment; filename="transactions_export.csv"
```

## Data Models

### Entity Relationship Diagram

```
┌─────────────┐       ┌─────────────────┐       ┌─────────────┐
│    User     │1    N │   Transaction   │N    1 │  Category   │
├─────────────┤───────├─────────────────┤───────├─────────────┤
│ id (PK)     │       │ id (PK)         │       │ id (PK)     │
│ email (UQ)  │       │ user_id (FK)    │       │ name        │
│ password    │       │ amount          │       │ user_id(FK) │
│ created_at  │       │ type            │       │ is_default  │
└─────────────┘       │ date            │       └─────────────┘
      │               │ description     │
      │               │ category_id(FK) │
      │               │ created_at      │
      │               └─────────────────┘
      │
      │1    N ┌─────────────────┐
      ├───────│   Subscription  │
      │       ├─────────────────┤
      │       │ id (PK)         │
      │       │ user_id (FK)    │
      │       │ name            │
      │       │ amount          │
      │       │ billing_cycle   │
      │       │ next_renewal    │
      │       │ created_at      │
      │       └─────────────────┘
      │
      │1    N ┌─────────────────┐
      ├───────│     Budget      │
      │       ├─────────────────┤
      │       │ id (PK)         │
      │       │ user_id (FK)    │
      │       │ month           │
      │       │ year            │
      │       │ limit_amount    │
      │       │ category_id(FK) │
      │       │ created_at      │
      │       └─────────────────┘
      │
      │1    N ┌─────────────────┐
      └───────│    Reminder     │
              ├─────────────────┤
              │ id (PK)         │
              │ user_id (FK)    │
              │ subscription_id │
              │ renewal_date    │
              │ is_read         │
              │ created_at      │
              └─────────────────┘
```

### JPA Entity Definitions

```java
@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;  // bcrypt hash

    private LocalDateTime createdAt;
}

@Entity
@Table(name = "transactions")
public class Transaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;  // INCOME, EXPENSE

    @Column(nullable = false)
    private LocalDate date;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    private LocalDateTime createdAt;
}

@Entity
@Table(name = "categories",
       uniqueConstraints = @UniqueConstraint(columns = {"name", "user_id"}))
public class Category {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;  // null for default categories

    private boolean isDefault;
}
```

```java
@Entity
@Table(name = "subscriptions")
public class Subscription {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillingCycle billingCycle;  // MONTHLY, ANNUAL

    @Column(nullable = false)
    private LocalDate nextRenewalDate;

    private LocalDateTime createdAt;
}

@Entity
@Table(name = "budgets",
       uniqueConstraints = @UniqueConstraint(columns = {"user_id", "month", "year", "category_id"}))
public class Budget {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer month;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal limitAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;  // null for overall monthly budget

    private LocalDateTime createdAt;
}

@Entity
@Table(name = "reminders")
public class Reminder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(nullable = false)
    private LocalDate renewalDate;

    private boolean isRead;

    private LocalDateTime createdAt;
}
```

### Enum Types

```java
public enum TransactionType { INCOME, EXPENSE }
public enum BillingCycle { MONTHLY, ANNUAL }
```

### Database Schema (MySQL DDL)

```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    user_id BIGINT,
    is_default BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uk_category_user (name, user_id)
);

CREATE TABLE transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    type ENUM('INCOME','EXPENSE') NOT NULL,
    date DATE NOT NULL,
    description VARCHAR(500),
    category_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (category_id) REFERENCES categories(id),
    INDEX idx_user_date (user_id, date)
);

CREATE TABLE subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    billing_cycle ENUM('MONTHLY','ANNUAL') NOT NULL,
    next_renewal_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_user_renewal (user_id, next_renewal_date)
);

CREATE TABLE budgets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    month TINYINT NOT NULL,
    year SMALLINT NOT NULL,
    limit_amount DECIMAL(12,2) NOT NULL,
    category_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (category_id) REFERENCES categories(id),
    UNIQUE KEY uk_budget_user_month (user_id, month, year, category_id)
);

CREATE TABLE reminders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    subscription_id BIGINT NOT NULL,
    renewal_date DATE NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (subscription_id) REFERENCES subscriptions(id)
);
```

## Error Handling

### Global Exception Handler

A `@RestControllerAdvice` class handles all exceptions and returns consistent error responses:

```java
record ErrorResponse(int status, String error, String message, LocalDateTime timestamp) {}
```

### Error Mapping

| Exception | HTTP Status | Scenario |
|---|---|---|
| `EmailAlreadyExistsException` | 409 Conflict | Duplicate registration email |
| `CategoryDuplicateException` | 409 Conflict | Duplicate category name for user |
| `InvalidCredentialsException` | 401 Unauthorized | Wrong email or password |
| `JwtAuthenticationException` | 401 Unauthorized | Expired or invalid JWT |
| `ResourceNotFoundException` | 404 Not Found | Entity not found by ID |
| `AccessDeniedException` | 403 Forbidden | Accessing another user's resource |
| `MethodArgumentNotValidException` | 400 Bad Request | Bean validation failures |

### Validation Strategy

- Use Jakarta Bean Validation annotations (`@NotNull`, `@NotBlank`, `@Email`, `@Size`, `@Min`) on request DTOs
- Controller methods annotated with `@Valid` to trigger validation
- Global exception handler transforms `MethodArgumentNotValidException` into structured 400 responses with field-level error details

## Caching Strategy

### Redis Configuration

```java
@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}
```

### Cache Keys and TTL

| Cache Key Pattern | TTL | Invalidated By |
|---|---|---|
| `dashboard:{userId}` | 5 minutes | Transaction CRUD, Budget CRUD, Subscription CRUD |

### Fallback Behavior

The `CacheService` wraps all Redis operations in try-catch blocks. If `RedisConnectionException` is caught, the service logs a warning via SLF4J and returns `null` (cache miss), allowing the caller to proceed with a direct database query.

## Docker Configuration

### docker-compose.yml Structure

```yaml
version: '3.8'
services:
  api:
    build: ./backend
    ports:
      - "${API_PORT:-8080}:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/expense_tracker
      - SPRING_DATASOURCE_USERNAME=${DB_USER:-root}
      - SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD:-secret}
      - SPRING_REDIS_HOST=redis
      - SPRING_REDIS_PORT=6379
      - JWT_SECRET=${JWT_SECRET:-default-dev-secret}
      - JWT_EXPIRATION_MS=${JWT_EXPIRATION_MS:-86400000}
    depends_on:
      db:
        condition: service_healthy
      redis:
        condition: service_started

  frontend:
    build: ./frontend
    ports:
      - "${FRONTEND_PORT:-3000}:3000"
    environment:
      - REACT_APP_API_URL=http://localhost:${API_PORT:-8080}

  db:
    image: mysql:8
    environment:
      - MYSQL_ROOT_PASSWORD=${DB_PASSWORD:-secret}
      - MYSQL_DATABASE=expense_tracker
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      retries: 5

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

volumes:
  mysql_data:
```

## Security Design

### JWT Token Structure

```json
{
  "sub": "user@example.com",
  "userId": 42,
  "iat": 1700000000,
  "exp": 1700086400
}
```

### Password Security

- Bcrypt with cost factor of 10 (configurable via `application.properties`)
- Passwords never returned in API responses
- Raw passwords never logged

### Resource Ownership

Every data-modifying or data-reading endpoint extracts the authenticated user ID from the JWT SecurityContext and applies it as a filter. Accessing or modifying another user's resource results in a 403 Forbidden response.

## Project Structure

```
expense-subscription-tracker/
├── docker-compose.yml
├── .env.example
├── backend/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/tracker/
│       ├── TrackerApplication.java
│       ├── config/         SecurityConfig, RedisConfig, WebConfig
│       ├── controller/     REST controllers
│       ├── dto/            Request/Response records
│       ├── entity/         JPA entities
│       ├── enums/          TransactionType, BillingCycle
│       ├── exception/      Custom exceptions + GlobalExceptionHandler
│       ├── repository/     Spring Data JPA repositories
│       ├── security/       JwtTokenProvider, JwtAuthFilter
│       ├── service/        Business logic services
│       └── scheduler/      ReminderGenerationJob
├── frontend/
│   ├── Dockerfile
│   ├── package.json
│   └── src/
│       ├── components/
│       ├── services/
│       ├── context/
│       ├── hooks/
│       └── utils/
└── .kiro/
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Password hashing security

For any registered user, the stored password SHALL be a bcrypt hash with a cost factor of at least 10, and the raw password SHALL never be recoverable from the stored hash.

**Validates: Requirements 1.3**

### Property 2: Registration input validation

For any registration request where the email does not match a valid email pattern OR the password is shorter than 8 characters, the API SHALL reject the request with status 400 and the user count SHALL remain unchanged.

**Validates: Requirements 1.4**

### Property 3: Duplicate email rejection

For any email that already exists in the system, a registration attempt with that email SHALL be rejected with status 409 and no new user SHALL be created.

**Validates: Requirements 1.2**

### Property 4: JWT access control

For any request to a protected endpoint, access SHALL be granted if and only if the request carries a valid, non-expired JWT token. Expired or malformed tokens SHALL result in status 401.

**Validates: Requirements 2.3, 2.4**

### Property 5: JWT payload completeness

For any JWT issued by the login endpoint, decoding the token SHALL reveal the user identifier and a future expiration timestamp.

**Validates: Requirements 2.5**

### Property 6: Transaction ownership isolation

For any two distinct users A and B, user A SHALL NOT be able to read, update, or delete transactions belonging to user B. Any such attempt SHALL return status 403.

**Validates: Requirements 3.4**

### Property 7: Transaction list ordering

For any user's transaction list response, the transactions SHALL be ordered by date in strictly descending order (most recent first).

**Validates: Requirements 3.5**

### Property 8: Transaction mandatory field validation

For any transaction creation request missing amount, type, or date, the API SHALL reject the request with a validation error and no transaction SHALL be persisted.

**Validates: Requirements 3.6**

### Property 9: Category listing completeness

For any user, querying their categories SHALL return all four default categories (Food, Rent, Shopping, Fuel) plus all custom categories created by that user, and no categories from other users.

**Validates: Requirements 4.1, 4.4**

### Property 10: Category uniqueness enforcement

For any user who already has a category with a given name (whether default or custom), attempting to create another category with the same name SHALL be rejected with status 409.

**Validates: Requirements 4.5**

### Property 11: Budget status calculation correctness

For any user with a budget for a given month, the budget status response SHALL satisfy: remainingAmount = limitAmount − totalSpending, AND overspent = (totalSpending > limitAmount), AND overspentAmount = max(0, totalSpending − limitAmount).

**Validates: Requirements 5.3, 5.4**

### Property 12: Subscription billing cycle validation

For any subscription creation or update request, the API SHALL accept only MONTHLY and ANNUAL as valid billing cycle values and reject all others.

**Validates: Requirements 6.5**

### Property 13: Reminder generation for upcoming renewals

For any subscription with a next renewal date within 3 days from today, the system SHALL generate a reminder containing the subscription name, renewal date, and amount for the owning user.

**Validates: Requirements 7.1, 7.4**

### Property 14: Reminder list ordering

For any user's unread reminder list, the reminders SHALL be sorted by renewal date in ascending order (earliest renewal first).

**Validates: Requirements 7.2**

### Property 15: Monthly report calculation accuracy

For any user and any given month/year, the report SHALL satisfy: totalIncome = sum of all INCOME transactions in that month, totalExpenses = sum of all EXPENSE transactions in that month, and netSavings = totalIncome − totalExpenses.

**Validates: Requirements 8.1**

### Property 16: Report category breakdown consistency

For any user's monthly report, the sum of all category breakdown totals SHALL equal the totalExpenses value in the same report, and each category total SHALL equal the sum of expense transactions assigned to that category within the month.

**Validates: Requirements 8.2**

### Property 17: Report month boundary filtering

For any report request for month M and year Y, the report SHALL include only transactions with dates >= first day of month M and dates <= last day of month M, excluding transactions from adjacent months.

**Validates: Requirements 8.4**

### Property 18: CSV export column completeness

For any CSV export, the output SHALL contain columns for date, type, amount, category, and description, and each transaction row SHALL have values in all columns (with empty string for null description/category).

**Validates: Requirements 9.2**

### Property 19: CSV export date range filtering

For any CSV export with a specified date range [startDate, endDate], every transaction row in the output SHALL have a date within that range, and no transactions outside the range SHALL appear.

**Validates: Requirements 9.3**

### Property 20: Dashboard subscription summary accuracy

For any user, the dashboard SHALL report activeSubscriptionCount equal to the count of that user's subscriptions, and totalSubscriptionCost equal to the sum of amounts of all active subscriptions.

**Validates: Requirements 10.2**

### Property 21: Dashboard upcoming renewals filter

For any user's dashboard response, the upcomingRenewals list SHALL contain only subscriptions with a next renewal date within the next 7 days, and SHALL NOT include subscriptions with renewal dates beyond 7 days.

**Validates: Requirements 10.4**

### Property 22: Cache invalidation on data mutation

For any mutation operation (create, update, or delete) on a user's transactions, budgets, or subscriptions, the cached dashboard data for that user SHALL be invalidated immediately after the operation completes.

**Validates: Requirements 12.2, 12.3**


## Testing Strategy

### Unit Tests (JUnit 5 + Mockito)

- **Service layer**: Test business logic in isolation with mocked repositories
- **Controller layer**: Use `@WebMvcTest` with MockMvc for endpoint validation, request/response format, and security filter behavior
- **JWT utilities**: Test token generation, parsing, validation, and expiration logic

### Property-Based Tests (jqwik)

- Minimum 100 iterations per property
- Focus on pure service-layer logic with mocked persistence
- Each property test references its corresponding design property
- Tag format: **Feature: expense-subscription-tracker, Property {number}: {title}**

### Integration Tests (Spring Boot Test + Testcontainers)

- Use Testcontainers for MySQL and Redis in integration tests
- Test full request lifecycle through controller → service → repository
- Verify cache behavior with real Redis instances
- Verify ownership isolation with multi-user scenarios

### Test Coverage Targets

| Layer | Coverage Target |
|---|---|
| Service | 90%+ (property + unit tests) |
| Controller | 80%+ (MockMvc tests) |
| Repository | Covered via integration tests |
| Security | 85%+ (JWT + filter tests) |
