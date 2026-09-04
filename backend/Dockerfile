# Multi-stage Java 21 Build for VoiceShield Backend
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy risk-engine and install to local maven cache
COPY risk_engine ./risk_engine
RUN cd risk_engine && mvn clean install -DskipTests

# Build Spring Boot Backend
COPY backend ./backend
RUN cd backend && mvn clean package -DskipTests

# Lightweight Production Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /app/backend/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
