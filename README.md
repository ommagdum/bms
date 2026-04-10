# Banking Management System (BMS) - Backend

This is the backend service for the Banking Management System, providing robust APIs for banking operations, user authentication, and profile management.

## Tech Stack
- **Java 21**, **Spring Boot**
- **Spring Security** & **OAuth2 Resource Server**
- **Spring Data JPA** & **PostgreSQL**
- **ShedLock** (scheduled task coordination)
- **Bucket4j** (rate limiting)
- **Apache POI** (document generation)
- **Lombok**, **Actuator**

## Features
- **User Authentication:** Secure access control including role-based permissions (Customer vs. Admin).
- **Google Authentication:** Integrated Google SSO via Spring Security OAuth2. Securely validates tokens provided by the Google login flow.
- **Transactions & Accounts:** Core APIs for handling account balances, deposits, withdrawals, and money transfers.
- **Loan Management:** APIs for loan processing and automated scheduled repayments.
- **Rate Limiting:** Request throttling mechanism using Bucket4j.

## Getting Started

### Prerequisites
- **Java 21**
- **PostgreSQL**
- **Maven**

### Setup
1. Clone the repository and navigate to the project directory.
2. Initialize and configure your local PostgreSQL database.
3. Verify your `application.properties`/`application.yml` for:
   - Database connection string (`spring.datasource.url`), username, and password.
   - Any API keys, secret elements, or OAuth2 specific properties (such as Google Provider JWK Set URI).
4. Run the application via Maven:
   ```bash
   ./mvnw spring-boot:run
   ```

## Google Authentication details
To enable Google Authentication, the backend acts as an OAuth2 Resource Server. Ensure that the correct Google Client IDs are authorized and the `spring.security.oauth2.resourceserver.jwt.issuer-uri` is set up properly in your environment configuration (usually `https://accounts.google.com`).
Tokens provided by the frontend are validated on the backend before completing secure actions.
