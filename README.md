# Microservice Project

A Java Spring Boot microservices application built with Maven.

## Overview

The project consists of three independent microservices:

- **Authentication Service** (`authentification_service`) — handles user authentication, credential management, BCrypt password hashing, and JWT token generation.
- **User Service** (`user_service`) — manages user profiles and payment cards, provides user-related operations, and handles JWT-based authorization.
- **Order Service** (`order_service`) — manages items and orders, including order creation, retrieval, updating, deletion, pagination, and filtering.

Each service has its own Maven configuration and PostgreSQL database.

Authentication is based on RSA-signed JWT tokens:

- Authentication Service uses a private RSA key to sign tokens.
- User Service and Order Service use the corresponding public RSA key to validate tokens.

Order Service communicates with User Service to retrieve user information. Circuit breaker logic is used to improve resilience during inter-service communication.

Redis is used by User Service.

## Technologies

- Java 21
- Spring Boot
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
- JUnit, Mockito and Testcontainers

## Project Structure

```text
Microservice_project/
├── .github/
│   └── workflows/
│       └── ci.yml
├── authentification_service/
│   ├── src/
│   ├── Dockerfile
│   ├── pom.xml
│   └── mvnw
├── user_service/
│   ├── src/
│   ├── Dockerfile
│   ├── pom.xml
│   └── mvnw
├── order_service/
│   ├── src/
│   ├── Dockerfile
│   ├── pom.xml
│   └── mvnw
├── secrets/
├── compose.yaml
├── .env.example
├── .gitignore
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

The project uses environment variables for database connections, credentials, JWT configuration, and internal service communication.

The example configuration is located in the root directory:

`.env.example`

Create your local environment file from the example:

```powershell
Copy-Item .env.example .env
```

Update `.env` with your local configuration.

Important variables include:

- Database URLs, usernames and passwords for each service.
- Redis host and port.
- `INTERNAL_API_KEY` — shared secret for internal service communication.
- `JWT_ISSUER` — JWT issuer shared by services.
- `BOOTSTRAP_ADMIN_PASSWORD` — initial administrator password.
- JWT private and public key paths.

Never commit `.env` or real credentials to Git. Use `.env.example` with placeholder values.

## RSA Key Generation

The application uses an RSA key pair for JWT signing and validation.

- `private.pem` — used by Authentication Service to sign JWTs.
- `public.pem` — used by other services to validate JWT signatures.

Store production keys in the root `secrets/` directory.

Security requirements:

- Never commit `private.pem` to Git.
- Do not include the private key in Docker images.
- Only Authentication Service should have access to the private key.
- Other services should receive only the public key.
- Keep the same matching key pair across services.

Configure key locations using environment variables.

## Running with Docker Compose

From the project root, validate the Compose configuration:

```bash
docker compose config
```

Build and start the services:

```bash
docker compose up --build -d
```

Check running containers:

```bash
docker compose ps
```

To stop the services:

```bash
docker compose down
```

To stop the services and delete their database volumes:

```bash
docker compose down -v
```

**Warning:** `docker compose down -v` deletes local database data.

## Running Tests

Each microservice is an independent Maven project.

Run tests from the project root:

### Authentication Service

```powershell
.\authentification_service\mvnw.cmd clean verify
```

### User Service

```powershell
.\user_service\mvnw.cmd clean verify
```

### Order Service

```powershell
.\order_service\mvnw.cmd clean verify
```

Docker must be running for tests that use Testcontainers.

## CI Pipeline

The project uses GitHub Actions for continuous integration.

The pipeline includes:

1. Building all microservices.
2. Running automated tests.
3. Optional code analysis.
4. Building Docker images.
5. Saving Docker images as CI artifacts.

The workflow is located at:

`.github/workflows/build.yml`

## Microservices

### Authentication Service

Responsible for authentication, password hashing, and JWT generation.

### User Service

Responsible for user profiles, payment card management, and authorization.

### Order Service

Responsible for item and order management. Supports CRUD operations, pagination, filtering, soft deletion, and communication with User Service.

Important environment variables:

**User Service database:**
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`

**Authentication Service database:**
- `AUTH_DB_USERNAME`
- `AUTH_DB_PASSWORD`

**Order Service database:**
- `ORDER_DB_NAME`
- `ORDER_DB_USERNAME`
- `ORDER_DB_PASSWORD`

**JWT configuration:**
- `JWT_ISSUER`

**Internal communication:**
- `INTERNAL_API_KEY`

**Redis configuration:**
- `REDIS_HOST`
- `REDIS_PORT`

**Bootstrap administrator:**
- `BOOTSTRAP_ADMIN_ENABLED`
- `BOOTSTRAP_ADMIN_LOGIN`
- `BOOTSTRAP_ADMIN_PASSWORD`
- `BOOTSTRAP_ADMIN_USER_ID`

Replace all example credentials and passwords with secure local values.
The bootstrap administrator ID must be a valid identifier accepted by the application.