# --- Stage 1: Build the application ---
FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /app

# Copy the pom.xml and source code
COPY pom.xml .
RUN mvn dependency:go-offline -B


COPY src ./src

# Build the application and skip tests for a faster build
RUN mvn clean package -DskipTests

# --- Stage 2: Create the runtime image ---
FROM maven:3.9-eclipse-temurin-25
WORKDIR /app


# Copy the built jar file from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose the default Eureka Server port
EXPOSE 8761

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]