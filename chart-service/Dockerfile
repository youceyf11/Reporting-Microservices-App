# ── Étape build ──
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn -q -pl :chart-service -am -DskipTests package \
    && cp chart-service/target/*-SNAPSHOT.jar app.jar

# ── Étape runtime ──
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/app.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]