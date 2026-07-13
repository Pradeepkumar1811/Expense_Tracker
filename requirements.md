# Requirements Document

## Introduction

This document defines the requirements for the Personal Expense & Subscription Tracker MVP — a full-stack web application that allows users to track income, expenses, subscriptions, and monthly budgets. The system consists of a Spring Boot REST API backend with JWT authentication, a MySQL database with Redis caching, and a React single-page application frontend. The application is containerized using Docker for local development.

## Glossary

- **System**: The complete Expense & Subscription Tracker application including backend API, frontend SPA, and supporting infrastructure
- **API**: The Spring Boot REST backend that handles business logic and data persistence
- **Frontend**: The React single-page application that provides the user interface
- **User**: A registered individual who interacts with the application to manage finances
- **Transaction**: A financial record representing either an income or an expense entry
- **Category**: A classification label applied to expenses (e.g., Food, Rent, Shopping, Fuel, or user-defined)
- **Subscription**: A recurring expense with a known renewal date and billing cycle
- **Budget**: A spending limit set by the User for a specific month and optionally a specific Category
- **Dashboard**: The main frontend view displaying summary financial statistics
- **Reminder**: An in-app notification alerting the User about an upcoming subscription renewal
- **JWT**: JSON Web Token used for stateless authentication between the Frontend and the API

## Requirements

### Requirement 1: User Registration

**User Story:** As a new user, I want to create an account, so that I can securely access my personal financial data.

#### Acceptance Criteria

1. WHEN a registration request is received with a valid email and password, THE API SHALL create a new User account and return a success response with status 201.
2. WHEN a registration request is received with an email that already exists, THE API SHALL reject the request and return a conflict error with status 409.
3. THE API SHALL store User passwords using bcrypt hashing with a minimum cost factor of 10.
4. WHEN a registration request is received with an invalid email format or a password shorter than 8 characters, THE API SHALL reject the request and return a validation error with status 400.

### Requirement 2: User Login and Authentication

**User Story:** As a registered user, I want to log in with my credentials, so that I can access my financial data securely.

#### Acceptance Criteria

1. WHEN a login request is received with valid credentials, THE API SHALL return a JWT access token with a configurable expiration time.
2. WHEN a login request is received with invalid credentials, THE API SHALL return an authentication error with status 401.
3. WHILE a request carries a valid non-expired JWT token, THE API SHALL authorize access to protected endpoints.
4. WHILE a request carries an expired or invalid JWT token, THE API SHALL reject the request with status 401.
5. THE API SHALL include the User identifier and token expiration timestamp in the JWT payload.

### Requirement 3: Transaction Management

**User Story:** As a user, I want to add, edit, and delete income and expense records, so that I can maintain an accurate history of my finances.

#### Acceptance Criteria

1. WHEN a User submits a new transaction with amount, type (income or expense), date, and description, THE API SHALL persist the Transaction and return it with a unique identifier.
2. WHEN a User updates an existing Transaction owned by that User, THE API SHALL persist the changes and return the updated Transaction.
3. WHEN a User deletes an existing Transaction owned by that User, THE API SHALL remove the Transaction and return a success response with status 204.
4. WHEN a User attempts to access a Transaction owned by another User, THE API SHALL reject the request with status 403.
5. WHEN a User requests their transaction list, THE API SHALL return paginated results sorted by date in descending order.
6. THE API SHALL require amount, type, and date as mandatory fields for every Transaction.

### Requirement 4: Expense Categorization

**User Story:** As a user, I want to categorize my expenses, so that I can understand my spending patterns by category.

#### Acceptance Criteria

1. THE API SHALL provide default categories: Food, Rent, Shopping, and Fuel for all Users.
2. WHEN a User creates a custom category with a unique name, THE API SHALL persist the Category and associate it with that User.
3. WHEN a User assigns a Category to an expense Transaction, THE API SHALL store the association between the Transaction and the Category.
4. WHEN a User requests their categories, THE API SHALL return both default categories and custom categories created by that User.
5. WHEN a User attempts to create a custom category with a name that duplicates an existing category for that User, THE API SHALL reject the request with status 409.

### Requirement 5: Monthly Budget Management

**User Story:** As a user, I want to set monthly budgets, so that I can control my spending and stay within my financial goals.

#### Acceptance Criteria

1. WHEN a User sets a budget with a month, year, and spending limit amount, THE API SHALL persist the Budget for that User.
2. WHEN a User sets a budget for a specific Category and month, THE API SHALL persist the category-level Budget for that User.
3. WHEN a User requests budget status for a given month, THE API SHALL return the budget limit, total spending, and remaining amount.
4. WHILE a User has exceeded a monthly budget, THE API SHALL indicate the overspent status and overspent amount in budget responses.
5. WHEN a User updates an existing Budget for a given month, THE API SHALL persist the updated spending limit.

### Requirement 6: Subscription Tracking

**User Story:** As a user, I want to track my subscriptions with renewal dates, so that I know when recurring charges will occur.

#### Acceptance Criteria

1. WHEN a User creates a subscription with name, amount, billing cycle, and next renewal date, THE API SHALL persist the Subscription for that User.
2. WHEN a User updates an existing Subscription, THE API SHALL persist the changes including updated renewal date and amount.
3. WHEN a User deletes a Subscription, THE API SHALL remove it and return status 204.
4. WHEN a User requests their subscriptions, THE API SHALL return all active subscriptions with the next renewal date for each.
5. THE API SHALL support monthly and annual billing cycle options for Subscriptions.

### Requirement 7: Subscription Renewal Reminders

**User Story:** As a user, I want to receive reminders before my subscriptions renew, so that I can decide whether to keep or cancel them.

#### Acceptance Criteria

1. WHEN a Subscription renewal date is within 3 days, THE System SHALL generate an in-app Reminder for the owning User.
2. WHEN a User requests their reminders, THE API SHALL return all unread Reminders sorted by renewal date in ascending order.
3. WHEN a User marks a Reminder as read, THE API SHALL update the Reminder status to read.
4. THE Reminder SHALL include the Subscription name, renewal date, and amount.

### Requirement 8: Monthly Spending Reports

**User Story:** As a user, I want to view monthly spending reports, so that I can analyze my financial behavior over time.

#### Acceptance Criteria

1. WHEN a User requests a report for a specific month and year, THE API SHALL return total income, total expenses, and net savings for that period.
2. WHEN a User requests a report for a specific month, THE API SHALL return a breakdown of expenses grouped by Category with totals for each.
3. WHEN a User requests a report for a month with no transactions, THE API SHALL return a report with zero values rather than an error.
4. THE API SHALL calculate report data from the User's Transactions within the requested calendar month boundaries.

### Requirement 9: CSV Export

**User Story:** As a user, I want to export my transactions to CSV, so that I can analyze my data in external tools.

#### Acceptance Criteria

1. WHEN a User requests a CSV export, THE API SHALL return a downloadable CSV file with a Content-Disposition header.
2. THE API SHALL include date, type, amount, category, and description columns in the exported CSV.
3. WHEN a User specifies a date range for export, THE API SHALL include only Transactions within that range.
4. WHEN a User requests a CSV export with no matching transactions, THE API SHALL return a CSV file containing only the header row.

### Requirement 10: Dashboard Summary

**User Story:** As a user, I want to see a dashboard with summary statistics, so that I can quickly understand my current financial status.

#### Acceptance Criteria

1. WHEN a User requests dashboard data, THE API SHALL return current month total income, total expenses, and net balance.
2. WHEN a User requests dashboard data, THE API SHALL return the count and total cost of active subscriptions.
3. WHEN a User requests dashboard data, THE API SHALL return budget utilization percentage for the current month.
4. WHEN a User requests dashboard data, THE API SHALL return upcoming subscription renewals within the next 7 days.
5. THE Frontend SHALL display dashboard data on the main authenticated view after login.

### Requirement 11: Frontend Application

**User Story:** As a user, I want a web-based interface, so that I can interact with my financial data through a browser.

#### Acceptance Criteria

1. THE Frontend SHALL provide a login and registration form for unauthenticated Users.
2. THE Frontend SHALL store the JWT token in browser storage and include it in API requests.
3. THE Frontend SHALL provide views for managing Transactions, Categories, Subscriptions, and Budgets.
4. THE Frontend SHALL provide a view for viewing monthly spending reports.
5. WHEN the API returns status 401, THE Frontend SHALL redirect the User to the login view.
6. THE Frontend SHALL provide a CSV export action accessible from the transactions view.

### Requirement 12: Caching

**User Story:** As a user, I want fast response times, so that the application feels responsive during normal usage.

#### Acceptance Criteria

1. THE API SHALL cache dashboard summary data in Redis with a time-to-live of 5 minutes.
2. WHEN a User creates, updates, or deletes a Transaction, THE API SHALL invalidate the cached dashboard data for that User.
3. WHEN a User modifies a Budget or Subscription, THE API SHALL invalidate related cached data for that User.
4. IF Redis is unavailable, THEN THE API SHALL serve requests directly from MySQL without caching and log a warning.

### Requirement 13: Docker Development Environment

**User Story:** As a developer, I want a containerized setup, so that I can run the full application stack locally with a single command.

#### Acceptance Criteria

1. THE System SHALL provide a Docker Compose configuration that starts Spring Boot, MySQL, and Redis containers.
2. WHEN `docker-compose up` is executed, THE System SHALL start all services with correct networking between containers.
3. THE System SHALL provide environment variable configuration for database credentials and Redis connection.
4. THE System SHALL persist MySQL data using a Docker volume to survive container restarts.
5. THE System SHALL expose the API on a configurable host port (default 8080) and the Frontend on a configurable host port (default 3000).
