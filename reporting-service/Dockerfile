# Étape 1 : Utiliser une image Java officielle
FROM eclipse-temurin:21-jre

# Étape 2 : Définir le répertoire de travail
WORKDIR /app

# Étape 3 : Copier le JAR
COPY target/reporting-service-0.0.1-SNAPSHOT.jar app.jar


# Étape 5 : Lancer l'application
ENTRYPOINT ["java", "-jar", "app.jar"]
