FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Копируем JAR-файл
COPY target/main-service-*.jar app.jar

EXPOSE 8080

# application.properties будет взят из JAR по умолчанию
ENTRYPOINT ["java", "-jar", "app.jar"]