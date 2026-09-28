# Образ собирается из уже собранного jar: сначала ./mvnw verify в корне репозитория
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/order-service.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
