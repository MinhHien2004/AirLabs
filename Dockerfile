# Stage 1: Build stage
FROM maven:3.9-eclipse-temurin-17 AS build


WORKDIR /app

# Copy pom.xml và download dependencies trước (layer caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code và build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Create image
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy jar file từ build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Environment variables (sẽ override trong Render)
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://dpg-d5j0ijnpm1nc73fj5cf0-a.singapore-postgres.render.com/airlabs_db
ENV SPRING_DATASOURCE_USERNAME=postgres1
ENV SPRING_DATASOURCE_PASSWORD=BSU5bqRa996QgA5Hye5iSKDB2Cd1VMG9

# Redis configuration - sử dụng Redis Cloud miễn phí
ENV SPRING_REDIS_HOST=redis-13482.c275.us-east-1-4.ec2.cloud.redislabs.com
ENV SPRING_REDIS_PORT=13482
ENV SPRING_REDIS_USERNAME=default
ENV SPRING_REDIS_PASSWORD=fvKhVJ2XXByDWhOnUvVWvIeYnnMOPKTy

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
