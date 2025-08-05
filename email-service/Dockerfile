FROM eclipse-temurin:21-jre
WORKDIR /app
COPY email-service/target/*-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]