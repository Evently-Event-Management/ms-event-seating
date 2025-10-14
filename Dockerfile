# --- Build Stage ---
# Use a Maven image to build the application JAR. This keeps our final image small.
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

# Set the working directory inside the container
WORKDIR /app

# Copy the Maven project files
COPY pom.xml .
COPY src ./src

# Build the application, skipping tests for faster builds in this context
RUN mvn clean package -DskipTests

# --- Final Stage ---
# Use a minimal, secure Java base image
FROM openjdk:21-slim

# Install curl for health checks
# The -y flag answers yes to prompts, and --no-install-recommends avoids installing extra packages
RUN apt-get update && apt-get install -y curl --no-install-recommends && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8081

# Add the health check instruction
# It will try 5 times, with a 10s timeout, every 30s.
HEALTHCHECK --interval=30s --timeout=10s --retries=5 \
  CMD curl -f http://localhost:8081/api/event-seating/health || exit 1

ENTRYPOINT ["java","-jar","/app/app.jar"]
