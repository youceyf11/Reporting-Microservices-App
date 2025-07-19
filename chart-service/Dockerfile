# Use Eclipse Temurin JRE 21 as the base image
FROM eclipse-temurin:21-jre

# Set the working directory
WORKDIR /app

# Copy the built jar file
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

COPY target/chart-service-0.0.1-SNAPSHOT.jar app.jar


# Expose the correct Spring Boot port
EXPOSE 8083

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
