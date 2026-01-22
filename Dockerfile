# Stage 1: Build Frontend
FROM node:20-alpine AS frontend-build

WORKDIR /app

# Copy package files
COPY package.json package-lock.json* ./

# Install dependencies
RUN npm ci --silent

# Copy frontend source files
COPY index.html vite.config.ts tsconfig.json tailwind.config.cjs postcss.config.js ./
COPY src/App.tsx src/index.tsx src/index.css src/Scheduled.tsx src/Scheduled.css ./src/
COPY public ./public

# Build frontend
RUN npm run build

# Stage 2: Build Backend
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml và download dependencies trước (layer caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy Java source code
COPY src/main/java ./src/main/java
COPY src/main/resources/application.yaml ./src/main/resources/
COPY src/test ./src/test

# Copy built frontend from frontend-build stage
COPY --from=frontend-build /app/src/main/resources/static ./src/main/resources/static

# Build Java application
RUN mvn clean package -DskipTests

# Stage 3: Create runtime image
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
