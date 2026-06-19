FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY apps/client/frontend/ /app/apps/client/frontend/
COPY target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]