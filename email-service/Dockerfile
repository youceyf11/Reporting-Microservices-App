# Étape build
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn -q -pl :email-service -am -DskipTests package

# Étape runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/email-service/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java","-jar","app.jar"]