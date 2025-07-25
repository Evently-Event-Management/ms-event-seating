# Event Seating Microservice

This microservice is part of a larger Event Ticketing and Management Platform. It handles event creation, venues, seating layouts, and real-time seat statuses.

## Overview

The Events & Seating Service:
- Manages event creation, updates, and deletion
- Stores event metadata, venues, and seating layouts
- Provides customizable seating layouts (arcs, blocks, tables)
- Delivers seat charts to frontend applications
- Emits domain events (e.g., event-created, event-published)
- Tracks real-time seat statuses

## Technology Stack

- **Framework**: Spring Boot
- **Security**: OAuth2 with Keycloak
- **Database**: PostgreSQL
- **Migration Tool**: Flyway
- **Containerization**: Docker & Docker Compose
- **API Documentation**: Swagger/OpenAPI

## Getting Started

### Prerequisites

- Java 17+
- Docker and Docker Compose
- Maven

### Development Environment Setup

1. **Clone the repository**
   ```bash
   git clone [your-repository-url]
   cd ms-event-seating
   ```

2. **Run the development database using Docker Compose**
   ```bash
   docker-compose up -d
   ```
   This will start PostgreSQL on port 5433 and create the `event_seating` database.

3. **Build the application**
   ```bash
   ./mvnw clean install
   ```

4. **Run the application**
   ```bash
   ./mvnw spring-boot:run
   ```

   The service will be available at: http://localhost:8081

### Database Migrations with Flyway

This project uses Flyway for database migrations.

#### Creating New Migrations

1. **Using IntelliJ IDEA**:
   - Right-click on `src/main/resources/db/migration`
   - Select "New" → "File"
   - Name the file using Flyway's naming convention: `V{version}__{description}.sql`
   - Example: `V2__add_seat_sections.sql`

2. **Writing Migration SQL**:
   - Add your SQL statements to create or modify database objects
   - Follow best practices for idempotent migrations

#### Running Migrations

Migrations run automatically when the application starts.

To run migrations manually using IntelliJ's Flyway integration:

1. Do changes in the model
2. Move to `src/main/resources/db/migration`
3. Right click and select `Flyway Migration`
4. On left select whole project or specific module
5. On right select `Database` and choose the database connection
6. Generate migration file under `src/main/resources/db/migration` with correct Version (Eg. `V2__add_seat_sections.sql`)
7. Generate the file and run the application to apply the migration

#### Troubleshooting Migrations

If you encounter issues with migrations:

- Set `spring.flyway.validate-on-migrate: false` temporarily in application.yml
- Check that the migration files follow the correct naming pattern
- Verify SQL syntax against the PostgreSQL version being used

## API Documentation

Once the application is running, access the API documentation at:
- http://localhost:8081/swagger-ui.html

## Configuration

The main configuration is in `application.yml`. Key settings include:

- Server port: 8081
- Database connection details
- OAuth2/JWT configuration
- Flyway migration settings

## Security

This service is configured as an OAuth2 Resource Server, using Keycloak for authentication and authorization.

JWT tokens are verified independently by this service using Keycloak's JWK endpoint.

## Development Guidelines

- Follow the existing package structure
- Write unit tests for all new functionality
- Document APIs using OpenAPI annotations
- Use the provided Flyway migration patterns for database changes

## Docker Support

The service is containerized and can be deployed in various environments:

- Development: Use `docker-compose.yml` for local development dependencies
- Production: The service is designed to run in Kubernetes with proper configurations

## Contact

For questions or issues, contact the project maintainers.
