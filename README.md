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

Environment variables can be set in two ways:
1. System environment variables
2. Using a `.env` file in the project root directory (recommended for development)

#### Using .env File for Development

1. Copy the `.env.example` file to create your own `.env` file:
   ```bash
   cp .env.example .env
   ```

2. Edit the `.env` file with your specific configuration values:
   ```bash
   # Example: Configuring database connection
   DATABASE_URL=jdbc:postgresql://localhost:5432/event_service
   DATABASE_USERNAME=your_username
   DATABASE_PASSWORD=your_password
   ```

3. The application will automatically load these variables on startup

#### Available Environment Variables

Here are the key environment variables that can be configured:

| Variable | Description | Default Value |
|----------|-------------|---------------|
| SERVER_PORT | Application port | 8081 |
| JWT_ISSUER_URI | Keycloak issuer URI | https://auth.dpiyumal.me/realms/event-ticketing |
| JWT_JWK_SET_URI | Keycloak JWK set URI | https://auth.dpiyumal.me/realms/event-ticketing/protocol/openid-connect/certs |
| DATABASE_URL | Database URL | jdbc:postgresql://localhost:5432/event_service |
| DATABASE_USERNAME | Database username | ticketly |
| DATABASE_PASSWORD | Database password | ticketly |
| REDIS_HOST | Redis host | localhost |
| REDIS_PORT | Redis port | 6379 |
| KAFKA_BOOTSTRAP_SERVERS | Kafka bootstrap servers | localhost:9092 |
| AWS_REGION | AWS region | ap-south-1 |
| AWS_S3_BUCKET_NAME | AWS S3 bucket name | ticketly-storage |

See `.env.example` for a complete list of all available environment variables.

## API Documentation

The API documentation is available through Swagger UI when the application is running:

- Local development: http://localhost:8081/api/event-seating/swagger-ui.html
- API docs in JSON format: http://localhost:8081/api/event-seating/api-docs

## Production Deployment

For production, set the appropriate environment variables and use the prod profile:

```bash
java -jar ms-event-seating.jar --spring.profiles.active=prod
```
