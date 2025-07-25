# Event Seating Microservice

This microservice handles event seating management for the Ticketly platform.

## Development Setup

### Prerequisites
- Java 21
- Maven
- Docker and Docker Compose

### Running Development Environment

1. Start the required services (PostgreSQL and LocalStack) using Docker Compose:
   ```bash
   docker-compose up -d
   ```

2. After the first run, initialize the S3 bucket in LocalStack:
   ```bash
   aws --endpoint-url=http://localhost:4566 s3 mb s3://ticketly-dev-uploads
   ```

3. Run the application with the dev profile:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

### Database Migrations

The project uses Flyway for database migrations:

1. To create a new migration file in IntelliJ:
   - Navigate to `src/main/resources/db/migration`
   - Create a new file following the naming convention: `V{version}__{description}.sql`
   - Example: `V3__add_user_preferences.sql`

2. Run migrations manually with Maven:
   ```bash
   ./mvnw flyway:migrate
   ```

3. Migrations run automatically on application startup

### Authentication

The application uses Keycloak for authentication and authorization:

- Default issuer URL: http://localhost:8080/realms/event-ticketing
- JWK Set URI: http://localhost:8080/realms/event-ticketing/protocol/openid-connect/certs

### Environment Variables

The following environment variables can be set:

| Variable | Description | Default Value |
|----------|-------------|---------------|
| SERVER_PORT | Application port | 8081 |
| JWT_ISSUER_URI | Keycloak issuer URI | http://localhost:8080/realms/event-ticketing |
| JWT_JWK_SET_URI | Keycloak JWK set URI | http://localhost:8080/realms/event-ticketing/protocol/openid-connect/certs |
| DATABASE_URL | Database URL | Development: jdbc:postgresql://localhost:5433/event_seating |
| DATABASE_USERNAME | Database username | Development: postgres |
| DATABASE_PASSWORD | Database password | Development: postgres |
| AWS_ACCESS_KEY | AWS access key | Development: test |
| AWS_SECRET_KEY | AWS secret key | Development: test |

## Production Deployment

For production, set the appropriate environment variables and use the prod profile:

```bash
java -jar ms-event-seating.jar --spring.profiles.active=prod
```
