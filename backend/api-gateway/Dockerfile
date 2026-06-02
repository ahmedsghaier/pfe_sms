FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# On copie tout le projet (nécessaire pour résoudre common-lib et le parent)
COPY . .

# On compile tout le projet pour s'assurer que common-lib est installé
RUN mvn clean package -Dmaven.test.skip=true

# Stage final
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Correction ici : pointer vers le target du module spécifique
COPY --from=build /app/api-gateway/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]