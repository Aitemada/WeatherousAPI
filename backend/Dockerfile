FROM gradle:8.14-jdk17-alpine AS builder
WORKDIR /app
COPY . .
RUN gradle clean bootJar -x test

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]