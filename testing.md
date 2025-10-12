# Testing Documentation for ms-event-seating

This document outlines the testing strategy, methodologies, and frameworks used in the ms-event-seating microservice.

## Table of Contents
- [Testing Strategy](#testing-strategy)
- [Test Categories](#test-categories)
- [Testing Frameworks and Libraries](#testing-frameworks-and-libraries)
- [Test Environment Configuration](#test-environment-configuration)
- [Integration Testing with TestContainers](#integration-testing-with-testcontainers)
- [Mock Authentication and Authorization](#mock-authentication-and-authorization)
- [Test Coverage](#test-coverage)
- [Running Tests](#running-tests)
- [CI/CD Integration](#cicd-integration)
- [Best Practices](#best-practices)

## Testing Strategy

The ms-event-seating microservice follows a comprehensive testing approach with multiple layers:

1. **Unit Testing**: Testing individual components in isolation by mocking their dependencies.
2. **Integration Testing**: Testing the interaction between components and external services using containerized dependencies.
3. **API Testing**: Testing REST endpoints to ensure correct behavior, validation, and error handling.

The testing pyramid is followed, with more unit tests than integration tests, ensuring fast feedback cycles while still providing good coverage.

## Test Categories

### Unit Tests
- **Controller Tests**: Verify REST controller behavior with mocked service dependencies
- **Service Tests**: Verify business logic in service layer
- **Factory Tests**: Ensure object creation logic works correctly
- **Exception Tests**: Validate exception handling

### Integration Tests
- **API Integration Tests**: End-to-end tests of REST endpoints with containerized dependencies
- **Database Integration**: Test JPA repositories against a real PostgreSQL database
- **External Services**: Test interactions with Redis, Kafka, and S3

## Testing Frameworks and Libraries

The following testing libraries and frameworks are used:

| Library/Framework | Version | Purpose |
|-------------------|---------|---------|
| JUnit 5 | Bundled with Spring Boot | Test framework |
| Mockito | 5.2.0 | Mocking dependencies |
| Spring Test | Bundled with Spring Boot | Testing Spring components |
| Spring Security Test | Bundled with Spring Boot | Authentication/authorization testing |
| TestContainers | Latest | Running containerized dependencies for tests |
| WireMock | 4.0.0-beta.15 | Mocking external HTTP APIs |
| H2 Database | Runtime | In-memory database for unit tests |

## Test Environment Configuration

Tests are configured with a separate application configuration in `src/test/resources/application.yml`, which includes:

- H2 in-memory database for unit tests
- Disabled Redis caching
- Mock OAuth2 configurations
- Test-specific tier limits and application settings

## Integration Testing with TestContainers

The `AbstractIntegrationTest` class provides the foundation for integration tests with the following containerized dependencies:

- **PostgreSQL**: For database interaction testing
- **Redis**: For caching tests
- **Kafka**: For event processing tests
- **Debezium**: For CDC (Change Data Capture) testing
- **LocalStack**: For AWS S3 storage service testing

These containers are started before tests run and are configured dynamically in the Spring context through `@DynamicPropertySource`.

## Mock Authentication and Authorization

Authentication and authorization testing is handled through:

1. **Spring Security Test**: For controller tests
2. **Custom JWT Mock**: Using `WithMockJwtUser` annotation for JWT authentication
3. **WireMock**: For mocking the OIDC server in integration tests

The `JwtTestUtils` class provides utility methods to generate test JWT tokens with various claims and roles.

## Test Coverage

Test coverage is monitored for the following areas:

1. **Controllers**: Ensuring all endpoints handle various scenarios correctly
2. **Services**: Verifying business logic for all operations
3. **Exception Handling**: Testing appropriate exception responses
4. **Security**: Validating authorization and authentication requirements

## Running Tests

### Running Unit Tests

```bash
./mvnw test
```

### Running Integration Tests Only

```bash
./mvnw test -Dtest=*IT
```

### Running All Tests with Coverage Report

```bash
./mvnw verify
```

## CI/CD Integration

Tests are run automatically as part of the CI pipeline. The following test stages are included:

1. **Build and Unit Test**: Run on every commit
2. **Integration Tests**: Run on pull requests and main branch commits
3. **Test Reports**: Generated and stored as artifacts

## Best Practices

The following best practices are followed in the test code:

1. **Test Independence**: Each test should be independent and not rely on other tests
2. **Clean Setup and Teardown**: Tests should clean up after themselves
3. **Meaningful Test Names**: Test methods are named to describe the behavior being tested
4. **Assertions**: Use clear, specific assertions that fail with meaningful messages
5. **Test Data**: Maintain test data separately from production code
6. **Mocking**: Only mock what is necessary; prefer using test containers for external dependencies
7. **Test Configuration**: Keep test configuration separate from production configuration

## Areas for Improvement

Potential areas for testing improvement:

1. **Property-Based Testing**: Introduce property-based testing for edge cases
2. **Performance Testing**: Add performance tests for critical paths
3. **Security Testing**: Enhance security testing with more sophisticated scenarios
4. **Chaos Testing**: Introduce chaos testing for resilience verification

---

This document is maintained by the Ticketly Engineering team. Last updated: October 12, 2025.