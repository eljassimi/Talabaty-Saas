FROM node:20-alpine AS frontend-builder
WORKDIR /app/frontend
COPY talabaty-frontend/package.json talabaty-frontend/package-lock.json ./
RUN npm ci
COPY talabaty-frontend/ ./
ENV VITE_API_URL=/api
RUN npx vite build

FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

COPY src src
COPY --from=frontend-builder /app/frontend/dist/ src/main/resources/static/ 
RUN ./mvnw package -DskipTests -B

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd -g 1001 app && useradd -u 1001 -g app -m app
USER app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
