# Образ собирается из уже собранного jar: сначала ./mvnw verify в корне репозитория
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/catalog-service.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
