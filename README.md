# Microservice Project

A Java Spring Boot microservices application built with Maven, Docker Compose, and Spring Cloud Gateway.

## Overview

The project consists of four independent applications:

- **Authentication Service (`authentification-service`)** — handles user authentication, credential management, BCrypt password hashing, and JWT token generation.
- **User Service (`user-service`)** — manages user profiles and payment cards, provides user-related operations, and handles JWT-based authorization.
- **Order Service (`order-service`)** — manages items and orders, including order creation, retrieval, updating, deletion, pagination, and filtering.
- **API Gateway (`api-gateway`)** — provides a single public HTTP entry point, routes requests to downstream services, and validates JWT access tokens.

Each business microservice has its own Maven configuration and PostgreSQL database.

Authentication is based on RSA-signed JWT tokens:

- Authentication Service uses a private RSA key to sign tokens.
- User Service and Order Service use the corresponding public RSA key to validate tokens.
- API Gateway also validates JWT access tokens using the public RSA key.
- The private signing key is available only to Authentication Service.

Order Service communicates with User Service to retrieve user information. Circuit breaker logic is used to improve resilience during inter-service communication.

Redis is used by User Service.

## Architecture

The API Gateway is a Spring Cloud Gateway **WebFlux** application and serves as the public HTTP entry point.

### API Gateway routes

| Public path | Downstream service |
| --- | --- |
| `/api/auth/**` | Authentication Service (`auth-app:8081`) |
| `/api/users/**`, `/api/cards/**` | User Service (`app:8080`) |
| `/api/orders/**`, `/api/items/**` | Order Service (`order-app:8082`) |

### Gateway security

- `POST /api/auth/login` and `POST /api/users/register` are public endpoints.
- All other routes, including actuator endpoints, require a valid RSA-signed JWT access token.
- The gateway validates the token signature, issuer, timestamps, and `tokenType=access`.
- Refresh tokens are rejected for protected API calls.
- The gateway receives only the public RSA key.
- The private signing key is mounted only into Authentication Service.

### Registration and compensation

User Service persists the user profile in its own PostgreSQL transaction and calls the private Authentication Service credentials endpoint using the `X-Internal-Api-Key` header.

Because the services own separate databases, a regular Spring `@Transactional` annotation cannot provide a distributed atomic transaction.

User Service therefore registers a transaction-completion compensation before calling Authentication Service. If its local transaction rolls back after the credentials creation call has been attempted, it calls Authentication Service's:

`DELETE /internal/credentials/{userId}`

Authentication Service deletes the credential and its associated refresh tokens.

The compensation mechanism retries failed requests up to three times with exponential backoff. Compensation failures are logged and may require manual reconciliation.

The current implementation uses synchronous saga-style compensation. A durable Transactional Outbox and retry workflow is planned as a future enhancement to improve the reliability of inter-service communication.
## Technologies

- Java 21
- Spring Boot
- Spring Cloud Gateway
- Spring WebFlux
- Maven
- Spring Security
- JWT
- RSA public/private key cryptography
- BCrypt
- Spring Data JPA
- Hibernate
- Liquibase
- MapStruct
- PostgreSQL
- Redis
- Docker and Docker Compose
- GitHub Actions
- JUnit
- Mockito
- Testcontainers

## Project Structure

```text
Microservice_project/
├── .github/
│   └── workflows/
│       └── build.yml
├── authentification-service/
│   ├── src/
│   ├── Dockerfile
│   ├── pom.xml
│   └── mvnw
├── user-service/
│   ├── src/
│   ├── Dockerfile
│   ├── pom.xml
│   └── mvnw
├── order-service/
│   ├── src/
│   ├── Dockerfile
│   ├── pom.xml
│   └── mvnw
├── api-gateway/
│   ├── src/
│   ├── Dockerfile
│   ├── pom.xml
│   └── mvnw
├── secrets/
├── compose.yaml
├── compose.override.yaml
├── .env.example
├── .gitignore
├── pom.xml
└── README.md
```

## Prerequisites

Before running the project, install:

- JDK 21
- Docker Desktop or Docker Engine
- Git

Docker must be running to execute integration tests that use Testcontainers.

## Configuration

### Environment Variables

The project uses environment variables for database connections, credentials, JWT configuration, Redis, and internal service communication.

The example configuration is located in the root directory:

`.env.example`

Create your local environment file from the example:

**PowerShell:**

```powershell
Copy-Item .env.example .env
```

Update `.env` with your local configuration.

Important variables include:

- Database URLs, usernames, and passwords for each service.
- Redis host and port.
- `INTERNAL_API_KEY` — shared secret for internal service communication.
- `JWT_ISSUER` — JWT issuer shared by services.
- `BOOTSTRAP_ADMIN_PASSWORD` — initial administrator password.
- JWT private and public key paths.

Never commit `.env` or real credentials to Git. Use `.env.example` with placeholder values.

### RSA Key Generation

The application uses an RSA key pair for JWT signing and validation.

- `private.pem` — used by Authentication Service to sign JWTs.
- `public.pem` — used by API Gateway, User Service, and Order Service to validate JWT signatures.

Store production keys in the root `secrets/` directory.

Security requirements:

- Never commit `private.pem` to Git.
- Do not include the private key in Docker images.
- Only Authentication Service should have access to the private key.
- Other services should receive only the public key.
- Keep the same matching key pair across services.
- Configure key locations using environment variables.

The included keys are for local development only. Replace them for any shared or production environment.

## Running with Docker Compose

1. Copy `.env.example` to `.env`.
2. Set strong, unique values for `DB_PASSWORD`, `AUTH_DB_PASSWORD`, `ORDER_DB_PASSWORD`, and `INTERNAL_API_KEY`.
3. Ensure `secrets/private.pem` and `secrets/public.pem` are a matching RSA key pair.
4. Validate the Compose configuration:

```bash
docker compose config
```

5. Build and start the services:

```bash
docker compose up --build -d
```

6. Check running containers:

```bash
docker compose ps
```

The API Gateway is exposed at:

`http://localhost:8080`

Useful commands:

```bash
docker compose logs -f api-gateway
docker compose logs -f auth-app
docker compose down
```

Persistent volumes are configured for all three PostgreSQL databases and Redis.

To stop the services and delete their database volumes:

```bash
docker compose down -v
```

**Warning:** `docker compose down -v` deletes local database data.

## Running Tests

Each microservice is an independent Maven project.

Run tests from the project root.

### Authentication Service

```powershell
.\authentification-service\mvnw.cmd clean verify
```

### User Service

```powershell
.\user-service\mvnw.cmd clean verify
```

### Order Service

```powershell
.\order-service\mvnw.cmd clean verify
```

### API Gateway

```powershell
.\api-gateway\mvnw.cmd clean verify
```

Docker must be running for tests that use Testcontainers.

The API Gateway security integration tests cover:

- Public login and registration POST routes.
- Rejection of missing and malformed tokens.
- Rejection of expired and wrong-issuer tokens.
- Rejection of refresh tokens on protected API routes.
- Rejection of tampered tokens.

User Service unit tests cover compensation on transaction rollback and no compensation on commit.

If Maven Central is unavailable, the Maven Wrapper cannot download Maven. Run the command again when repository access is available.

## CI Pipeline

The project uses GitHub Actions for continuous integration.

The pipeline includes:

- Building all microservices.
- Running automated tests.
- Optional code analysis.
- Building Docker images.
- Saving Docker images as CI artifacts.

The workflow is located at:

`.github/workflows/build.yml`

## Microservices

### Authentication Service

Responsible for:

- User authentication.
- Credential management.
- BCrypt password hashing.
- JWT access and refresh token generation.
- RSA-based JWT signing.
- Internal credential management endpoints.

### User Service

Responsible for:

- User profiles.
- Payment card management.
- User-related operations.
- JWT-based authorization.
- Redis integration.
- Communication with Authentication Service during registration.

### Order Service

Responsible for:

- Item management.
- Order creation and management.
- CRUD operations.
- Pagination and filtering.
- Soft deletion.
- Resource authorization.
- Communication with User Service.

### API Gateway

Responsible for:

- Providing a single public entry point.
- Routing requests to downstream microservices.
- Validating RSA-signed JWT access tokens.
- Enforcing public and protected endpoint rules.
- Rejecting refresh tokens for protected API calls.

## Important Environment Variables

### User Service database

- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`

### Authentication Service database

- `AUTH_DB_USERNAME`
- `AUTH_DB_PASSWORD`

### Order Service database

- `ORDER_DB_NAME`
- `ORDER_DB_USERNAME`
- `ORDER_DB_PASSWORD`

### JWT configuration

- `JWT_ISSUER`

### Internal communication

- `INTERNAL_API_KEY`

### Redis configuration

- `REDIS_HOST`
- `REDIS_PORT`

### Bootstrap administrator

- `BOOTSTRAP_ADMIN_ENABLED`
- `BOOTSTRAP_ADMIN_LOGIN`
- `BOOTSTRAP_ADMIN_PASSWORD`
- `BOOTSTRAP_ADMIN_USER_ID`

Replace all example credentials and passwords with secure local values. The bootstrap administrator ID must be a valid identifier accepted by the application.


## Planned Features

The following features are planned for future development:

- **Transactional Outbox Pattern** — replace the current synchronous saga compensation mechanism used during user registration with a durable event-based workflow. This will improve reliability of inter-service communication and ensure that failed compensation operations can be retried after service restarts.
- **Payments Management** — implement payment processing and transaction management.

These features are not part of the current implementation.
