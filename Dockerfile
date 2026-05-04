# Build stage
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy pom.xml and download dependencies (cache layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Default environment variables (can be overridden)
ENV DB_HOST=localhost \
    DB_PORT=3306 \
    DB_NAME=tracking_order \
    DB_USER=root \
    DB_PASSWORD="" \
    JWT_SECRET="" \
    JWT_EXPIRATION=86400000

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
