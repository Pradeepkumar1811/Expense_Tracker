# Implementation Plan: Expense & Subscription Tracker

## Overview

This plan implements a full-stack Personal Expense & Subscription Tracker with a Spring Boot 3.x REST API backend, MySQL 8 database, Redis caching, and React SPA frontend, all containerized with Docker Compose. Tasks are ordered to build foundational layers first (entities, security) then feature modules, and finally integration and wiring.

## Tasks

- [x] 1. Project scaffolding and Docker setup
  - [x] 1.1 Create Spring Boot project structure with Maven pom.xml
    - Initialize Spring Boot 3.x project with dependencies: spring-boot-starter-web, spring-boot-starter-data-jpa, spring-boot-starter-security, spring-boot-starter-data-redis, mysql-connector-j, jjwt, lombok, spring-boot-starter-validation
    - Create package structure: config, controller, dto, entity, enums, exception, repository, security, service, scheduler
    - Create `application.properties` with datasource, redis, and JWT configuration placeholders
    - _Requirements: 13.1, 13.3_

  - [x] 1.2 Create Docker Compose configuration and Dockerfiles
    - Create `docker-compose.yml` with api, frontend, db (mysql:8), and redis (redis:7-alpine) services
    - Create `.env.example` with configurable ports, DB credentials, JWT secret
    - Create `backend/Dockerfile` for Spring Boot (multi-stage Maven build)
    - Configure MySQL health check, volume persistence, and service dependencies
    - Expose API on port 8080 and frontend on port 3000 (configurable)
    - _Requirements: 13.1, 13.2, 13.3, 13.4, 13.5_

  - [x] 1.3 Create React frontend project structure
    - Initialize React app with Create React App or Vite
    - Install dependencies: axios, react-router-dom
    - Create directory structure: components/, services/, context/, hooks/, utils/
    - Create `frontend/Dockerfile` for production build
    - _Requirements: 11.1, 13.5_

- [x] 2. Database entities and enums
  - [x] 2.1 Create enum types and JPA entities
    - Create `TransactionType` enum (INCOME, EXPENSE)
    - Create `BillingCycle` enum (MONTHLY, ANNUAL)
    - Create `User` entity with id, email (unique), password, createdAt
    - Create `Category` entity with id, name, user (nullable for defaults), isDefault, unique constraint on (name, user_id)
    - Create `Transaction` entity with id, user, amount, type, date, description, category, createdAt
    - Create `Subscription` entity with id, user, name, amount, billingCycle, nextRenewalDate, createdAt
    - Create `Budget` entity with id, user, month, year, limitAmount, category, createdAt, unique constraint on (user_id, month, year, category_id)
    - Create `Reminder` entity with id, user, subscription, renewalDate, isRead, createdAt
    - _Requirements: 1.1, 3.1, 4.1, 5.1, 6.1, 7.1_

  - [x] 2.2 Create Spring Data JPA repositories
    - Create `UserRepository` with `findByEmail` method
    - Create `TransactionRepository` with paginated user-scoped queries and date-range filtering
    - Create `CategoryRepository` with query for default + user-specific categories
    - Create `BudgetRepository` with query by user, month, year
    - Create `SubscriptionRepository` with user-scoped queries and renewal date filtering
    - Create `ReminderRepository` with query for unread reminders by user sorted by renewalDate ASC
    - _Requirements: 3.5, 4.4, 5.3, 6.4, 7.2_

  - [x] 2.3 Create database initialization script for default categories
    - Create `data.sql` or `@PostConstruct` initializer to seed default categories: Food, Rent, Shopping, Fuel
    - _Requirements: 4.1_

- [x] 3. Security and authentication layer
  - [x] 3.1 Implement JWT token provider
    - Create `JwtTokenProvider` class with methods: generateToken(userId, email), validateToken(token), getUserIdFromToken(token)
    - Use HS256 signing with configurable secret from `application.properties`
    - Include userId and expiration in JWT payload
    - Configure token expiration time via properties
    - _Requirements: 2.1, 2.5_

  - [x] 3.2 Implement JWT authentication filter and security config
    - Create `JwtAuthenticationFilter` extending OncePerRequestFilter
    - Extract Bearer token from Authorization header, validate, and set SecurityContext
    - Create `SecurityConfig` with: permit `/api/auth/**`, require authentication for all other endpoints
    - Configure CORS for frontend origin
    - _Requirements: 2.3, 2.4_

  - [x] 3.3 Implement AuthService and AuthController
    - Create `AuthService` with register (bcrypt cost factor 10) and login (credential validation + JWT issuance) methods
    - Create request/response DTOs: RegisterRequest, RegisterResponse, LoginRequest, LoginResponse
    - Create `AuthController` with POST `/api/auth/register` and POST `/api/auth/login`
    - Apply bean validation: @Email, @Size(min=8) on registration DTO
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 2.1, 2.2_

  - [ ]* 3.4 Write property tests for authentication (jqwik)
    - **Property 1: Password hashing security** - Verify stored password is bcrypt hash with cost >= 10
    - **Property 2: Registration input validation** - Verify invalid email/short password returns 400 and user count unchanged
    - **Property 3: Duplicate email rejection** - Verify duplicate email returns 409 and no new user created
    - **Validates: Requirements 1.2, 1.3, 1.4**

  - [ ]* 3.5 Write property tests for JWT (jqwik)
    - **Property 4: JWT access control** - Verify valid token grants access, expired/malformed returns 401
    - **Property 5: JWT payload completeness** - Verify decoded JWT contains userId and future expiration
    - **Validates: Requirements 2.3, 2.4, 2.5**

- [x] 4. Checkpoint - Ensure authentication works end-to-end
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Exception handling and DTOs
  - [x] 5.1 Create global exception handler and custom exceptions
    - Create custom exceptions: EmailAlreadyExistsException, CategoryDuplicateException, InvalidCredentialsException, JwtAuthenticationException, ResourceNotFoundException, AccessDeniedException
    - Create `ErrorResponse` record with status, error, message, timestamp
    - Create `@RestControllerAdvice` GlobalExceptionHandler mapping exceptions to correct HTTP status codes
    - Handle `MethodArgumentNotValidException` for field-level validation errors
    - _Requirements: 1.2, 1.4, 2.2, 3.4, 4.5_

  - [x] 5.2 Create all request/response DTO records
    - Create TransactionRequest, TransactionResponse, PagedResponse
    - Create CategoryRequest, CategoryResponse
    - Create BudgetRequest, BudgetStatusResponse
    - Create SubscriptionRequest, SubscriptionResponse
    - Create ReminderResponse
    - Create MonthlyReportResponse, CategoryBreakdown
    - Create DashboardResponse
    - _Requirements: 3.1, 4.2, 5.1, 6.1, 7.4, 8.1, 10.1_

- [x] 6. Transaction management
  - [x] 6.1 Implement TransactionService and TransactionController
    - Create `TransactionService` with CRUD operations, ownership validation, and pagination
    - Create `TransactionController` with endpoints: POST, PUT /{id}, DELETE /{id}, GET (paginated)
    - Validate ownership on update/delete (return 403 for unauthorized access)
    - Return paginated results sorted by date descending
    - Validate mandatory fields: amount, type, date
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_

  - [x] 6.2 Write property tests for transaction management (jqwik)
    - **Property 6: Transaction ownership isolation** - Verify user A cannot access user B's transactions
    - **Property 7: Transaction list ordering** - Verify transactions sorted by date descending
    - **Property 8: Transaction mandatory field validation** - Verify missing fields returns validation error
    - **Validates: Requirements 3.4, 3.5, 3.6**

- [x] 7. Category management
  - [x] 7.1 Implement CategoryService and CategoryController
    - Create `CategoryService` with: list categories (defaults + user custom), create custom category with duplicate detection
    - Create `CategoryController` with endpoints: POST `/api/categories`, GET `/api/categories`
    - Return combined default + user-specific categories
    - Reject duplicate category names with 409
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

  - [x] 7.2 Write property tests for categories (jqwik)
    - **Property 9: Category listing completeness** - Verify all defaults + user customs returned, no other user's categories
    - **Property 10: Category uniqueness enforcement** - Verify duplicate name rejected with 409
    - **Validates: Requirements 4.1, 4.4, 4.5**

- [x] 8. Budget management
  - [x] 8.1 Implement BudgetService and BudgetController
    - Create `BudgetService` with: create/update budget, calculate spending status (remaining, overspent detection)
    - Create `BudgetController` with endpoints: POST, PUT /{id}, GET with month/year params
    - Calculate totalSpending from transactions in the budget month
    - Compute remainingAmount = limitAmount - totalSpending, overspent flag, overspentAmount
    - Support overall and category-level budgets
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

  - [ ]* 8.2 Write property tests for budget calculations (jqwik)
    - **Property 11: Budget status calculation correctness** - Verify remainingAmount = limitAmount - totalSpending, overspent = (totalSpending > limitAmount), overspentAmount = max(0, totalSpending - limitAmount)
    - **Validates: Requirements 5.3, 5.4**

- [x] 9. Subscription management
  - [x] 9.1 Implement SubscriptionService and SubscriptionController
    - Create `SubscriptionService` with CRUD operations and billing cycle validation
    - Create `SubscriptionController` with endpoints: POST, PUT /{id}, DELETE /{id}, GET
    - Validate billing cycle is MONTHLY or ANNUAL
    - Return all active subscriptions with next renewal date
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

  - [ ]* 9.2 Write property test for subscriptions (jqwik)
    - **Property 12: Subscription billing cycle validation** - Verify only MONTHLY and ANNUAL accepted
    - **Validates: Requirements 6.5**

- [x] 10. Reminder system
  - [x] 10.1 Implement ReminderService, ReminderController, and scheduled job
    - Create `ReminderService` with: list unread reminders sorted by renewalDate ASC, mark as read
    - Create `ReminderController` with endpoints: GET `/api/reminders`, PATCH `/api/reminders/{id}/read`
    - Create `ReminderGenerationJob` with @Scheduled cron (daily) to generate reminders for subscriptions with renewal within 3 days
    - Prevent duplicate reminder generation for same renewal cycle
    - _Requirements: 7.1, 7.2, 7.3, 7.4_

  - [ ]* 10.2 Write property tests for reminders (jqwik)
    - **Property 13: Reminder generation for upcoming renewals** - Verify reminders generated for subscriptions within 3 days
    - **Property 14: Reminder list ordering** - Verify unread reminders sorted by renewalDate ascending
    - **Validates: Requirements 7.1, 7.2, 7.4**

- [x] 11. Checkpoint - Ensure all CRUD modules pass tests
  - Ensure all tests pass, ask the user if questions arise.

- [x] 12. Reports and export
  - [x] 12.1 Implement ReportService and ReportController
    - Create `ReportService` with: compute monthly report (totalIncome, totalExpenses, netSavings, category breakdown)
    - Create `ReportController` with GET `/api/reports?month=&year=`
    - Filter transactions strictly within calendar month boundaries
    - Return zero-value report for months with no transactions
    - _Requirements: 8.1, 8.2, 8.3, 8.4_

  - [ ]* 12.2 Write property tests for reports (jqwik)
    - **Property 15: Monthly report calculation accuracy** - Verify totalIncome = sum(INCOME), totalExpenses = sum(EXPENSE), netSavings = totalIncome - totalExpenses
    - **Property 16: Report category breakdown consistency** - Verify sum of breakdowns equals totalExpenses
    - **Property 17: Report month boundary filtering** - Verify only transactions within month boundaries included
    - **Validates: Requirements 8.1, 8.2, 8.4**

  - [x] 12.3 Implement ExportService and ExportController
    - Create `ExportService` with CSV generation including date range filtering
    - Create `ExportController` with GET `/api/export/csv?startDate=&endDate=`
    - Set Content-Type: text/csv and Content-Disposition header
    - Include columns: date, type, amount, category, description
    - Return header-only CSV when no matching transactions
    - _Requirements: 9.1, 9.2, 9.3, 9.4_

  - [ ]* 12.4 Write property tests for CSV export (jqwik)
    - **Property 18: CSV export column completeness** - Verify all required columns present with values
    - **Property 19: CSV export date range filtering** - Verify only transactions within range included
    - **Validates: Requirements 9.2, 9.3**

- [x] 13. Dashboard and caching
  - [x] 13.1 Implement Redis caching configuration and CacheService
    - Create `RedisConfig` with RedisTemplate bean (StringRedisSerializer + GenericJackson2JsonRedisSerializer)
    - Create `CacheService` with get/set/invalidate operations and try-catch fallback for Redis unavailability
    - Log warning via SLF4J when Redis is unavailable, continue with direct DB query
    - _Requirements: 12.1, 12.4_

  - [x] 13.2 Implement DashboardService and DashboardController
    - Create `DashboardService` aggregating: current month income/expenses/balance, active subscription count/cost, budget utilization %, upcoming renewals within 7 days
    - Create `DashboardController` with GET `/api/dashboard`
    - Cache dashboard response in Redis with 5-minute TTL using key pattern `dashboard:{userId}`
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 12.1_

  - [x] 13.3 Add cache invalidation to mutation operations
    - Add cache invalidation in TransactionService on create/update/delete
    - Add cache invalidation in BudgetService on create/update
    - Add cache invalidation in SubscriptionService on create/update/delete
    - _Requirements: 12.2, 12.3_

  - [ ]* 13.4 Write property tests for dashboard and caching (jqwik)
    - **Property 20: Dashboard subscription summary accuracy** - Verify activeSubscriptionCount and totalSubscriptionCost match actual data
    - **Property 21: Dashboard upcoming renewals filter** - Verify only subscriptions within 7 days included
    - **Property 22: Cache invalidation on data mutation** - Verify cache invalidated after CRUD operations
    - **Validates: Requirements 10.2, 10.4, 12.2, 12.3**

- [x] 14. Frontend implementation
  - [x] 14.1 Implement authentication context and API service layer
    - Create `AuthContext` with JWT storage in localStorage, login/logout methods
    - Create axios instance with interceptor to attach Bearer token
    - Create interceptor to handle 401 responses (clear token, redirect to login)
    - Create API service modules for each resource (transactions, categories, budgets, subscriptions, reminders, reports, dashboard)
    - _Requirements: 11.2, 11.5_

  - [x] 14.2 Implement login and registration views
    - Create `LoginForm` component with email/password fields and validation
    - Create `RegisterForm` component with email/password fields and validation
    - Set up React Router with public (auth) and protected routes
    - _Requirements: 11.1_

  - [x] 14.3 Implement dashboard view
    - Create `DashboardView` component displaying: income/expenses/balance, subscription count/cost, budget utilization, upcoming renewals
    - Create `StatCard` reusable component for metric display
    - Create `UpcomingRenewals` component listing subscriptions due in 7 days
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 11.5_

  - [x] 14.4 Implement transaction management views
    - Create `TransactionList` with pagination and date-sorted display
    - Create `TransactionForm` for adding/editing transactions with category selection
    - Create `ExportButton` component for CSV download
    - _Requirements: 11.3, 11.6_

  - [x] 14.5 Implement remaining resource views
    - Create `CategoryList` and `CategoryForm` for category management
    - Create `BudgetList`, `BudgetForm`, and `BudgetStatus` for budget management
    - Create `SubscriptionList` and `SubscriptionForm` for subscription management
    - Create `ReminderList` and `ReminderItem` for reminder display and mark-as-read
    - Create `ReportView` and `CategoryBreakdown` for monthly reports
    - _Requirements: 11.3, 11.4_

- [x] 15. Integration and wiring
  - [x] 15.1 Wire frontend routing and navigation
    - Create main App component with navigation layout
    - Connect all views with React Router navigation
    - Implement protected route wrapper using AuthContext
    - Add responsive navigation menu
    - _Requirements: 11.1, 11.3, 11.5_

  - [ ]* 15.2 Write integration tests with Testcontainers
    - Set up Testcontainers with MySQL and Redis for integration tests
    - Write integration tests for authentication flow (register → login → access protected endpoint)
    - Write integration tests for transaction CRUD with ownership isolation
    - Write integration tests for cache invalidation with real Redis
    - _Requirements: 1.1, 2.1, 3.4, 12.2_

- [x] 16. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests use jqwik framework and validate universal correctness properties from the design
- Unit tests validate specific examples and edge cases
- Backend uses Java 17+ with Spring Boot 3.x
- Frontend uses React with axios for API communication
- All tests should be runnable with `mvn test` (backend) and `npm test` (frontend)

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.3"] },
    { "id": 1, "tasks": ["1.2", "2.1"] },
    { "id": 2, "tasks": ["2.2", "2.3"] },
    { "id": 3, "tasks": ["3.1", "5.1"] },
    { "id": 4, "tasks": ["3.2", "5.2"] },
    { "id": 5, "tasks": ["3.3"] },
    { "id": 6, "tasks": ["3.4", "3.5"] },
    { "id": 7, "tasks": ["6.1", "7.1", "9.1"] },
    { "id": 8, "tasks": ["6.2", "7.2", "8.1", "9.2"] },
    { "id": 9, "tasks": ["8.2", "10.1"] },
    { "id": 10, "tasks": ["10.2", "12.1", "12.3"] },
    { "id": 11, "tasks": ["12.2", "12.4", "13.1"] },
    { "id": 12, "tasks": ["13.2"] },
    { "id": 13, "tasks": ["13.3"] },
    { "id": 14, "tasks": ["13.4", "14.1"] },
    { "id": 15, "tasks": ["14.2", "14.3"] },
    { "id": 16, "tasks": ["14.4", "14.5"] },
    { "id": 17, "tasks": ["15.1"] },
    { "id": 18, "tasks": ["15.2"] }
  ]
}
```
