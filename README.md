# Microservice Project

A Java Spring Boot microservices application built with Maven.

## Overview

The project currently consists of two microservices:

- **Authentication Service** (`authentification_service`) — user authentication, credential storage, BCrypt password hashing, and JWT token generation and validation.
- **User Service** (`user_service`) — user profiles, payment cards, and JWT-based authorization.

The services use separate PostgreSQL databases. Redis is used by User Service.

Authentication uses RSA-signed JWTs:
- The Authentication Service uses a private RSA key to sign tokens.
- The User Service uses the corresponding public RSA key to validate tokens.

Orders and Payments services are planned for future development and are not included in the current implementation.

## Technologies

- Java 21
- Spring Boot
- Maven
- Spring Security
- JWT
- RSA public/private key cryptography
- BCrypt
- PostgreSQL
- Redis
- Docker and Docker Compose
- JUnit and Testcontainers

## Prerequisites

Before running the project, install:

- JDK 21
- Docker Desktop or Docker Engine
- Git

Docker must be running to execute integration tests that use Testcontainers.

## Project Structure

```text
Microservice_project/
├── authentification_service/
│   ├── src/
│   ├── Dockerfile
│   └── ...
├── user_service/
│   ├── src/
│   ├── Dockerfile
│   ├── compose.yaml
│   └── .env.example
├── mvnw
├── mvnw.cmd
├── pom.xml
└── README.md
```

## Configuration

### Environment variables

The example environment configuration is located at:

`user_service/.env.example`
`authentication_service/.env.example`

For local Docker development, create your own `.env` file from the example.

On Windows PowerShell, run this command from the project root:

```powershell
Copy-Item user_service/.env.example user_service/.env
```

Open `user_service/.env` and replace the example passwords and secrets with your own values.

Important variables include:

- `DB_PASSWORD` — User Service database password.
- `AUTH_DB_PASSWORD` — Authentication Service database password.
- `INTERNAL_API_KEY` — shared secret used for internal communication between the services.
- `JWT_ISSUER` — JWT issuer; it must be consistent across both services.
- `BOOTSTRAP_ADMIN_PASSWORD` — local development administrator password.

Do not commit `.env` to Git. Commit `.env.example` with placeholder values instead.

## RSA Key Generation

The application requires an RSA key pair for JWT signing and validation.

The project provides a Java utility class:

`authentification_service/src/main/java/org/course_project/authenticationService/RsaKeyGenerator.java`

The utility generates two files:

- `private.pem` — the private key used by Authentication Service to sign JWTs.
- `public.pem` — the public key used to validate JWT signatures.

After generation, verify that both files exist:

```text
authentification_service/private.pem
authentification_service/public.pem
src/resources/keys/public.pem
```

### Important security notes

- **Never commit `private.pem` to Git.**
- Do not include the private key in a Docker image.
- Keep the private key accessible only to the Authentication Service.
- The public key can be shared with services that need to validate JWTs.
- Do not regenerate the key pair every time the application starts. Generate it once and reuse the matching pair.
- If the private key is lost or compromised, generate a new pair and update the corresponding configuration. Tokens signed with the old key will no longer validate with the new public key.

The application is configured to read the private key location from `JWT_PRIVATE_KEY_PATH`. Set this environment variable to the actual path of `private.pem` in your environment.

For local development, configure the public key location using `JWT_PUBLIC_KEY_PATH` if necessary. The exact paths depend on where the key files are stored and how the services are launched.

**Note:** The current key-loading implementation expects filesystem paths. A value such as `classpath:keys/private.pem` will not work with `Path.of(...)` unless the key-loading implementation is changed to use Spring's resource-loading API.


### In summary:

1. Create and configure `user_service/.env`.
2. Generate the RSA key pair if the key files do not already exist.
3. Make sure the private and public key paths are configured correctly.
4. From the `user_service` directory, validate the Compose configuration and start the services:

```bash
docker compose config
docker compose up --build -d
docker compose ps
```

Check the service health endpoints:

- Authentication Service: `http://localhost:8081/actuator/health`
- User Service: `http://localhost:8080/actuator/health`


To stop the services:

```bash
docker compose down
```

To stop the services and delete their local database volumes:

```bash
docker compose down -v
```

**Warning:** `docker compose down -v` deletes the local database data.

## Run Tests

mvn test
mvn verify

### The current implementation focuses on authentication, JWT authorization, user profiles, and payment cards.

The following features are planned for future development:

- Items management
- Orders management
- Payments management

These features are not part of the current implementation.