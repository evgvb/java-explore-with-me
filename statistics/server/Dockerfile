FROM eclipse-temurin:21-jdk-alpine

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем JAR-файл приложения
COPY target/stats-server-*.jar app.jar

# Открываем порт для сервера статистики
EXPOSE 9090

# Запускаем приложение
ENTRYPOINT ["java", "-jar", "app.jar"]