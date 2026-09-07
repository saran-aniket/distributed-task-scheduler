# --- Stage 1: Dependency Caching ---
FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /app

# Copy only the dependency file first to leverage Docker caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests


# --- STAGE 2: Run the Application ---
FROM maven:3.9-eclipse-temurin-25 as runner
WORKDIR /app


# Copy only the compiled JAR file from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose Spring Boot's default port
EXPOSE 8080

# Execute the application
ENTRYPOINT ["java", "-jar", "app.jar"]
