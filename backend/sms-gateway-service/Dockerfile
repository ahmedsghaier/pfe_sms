FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copier tout le projet (monorepo)
COPY . .

# BUILD PROPRE (skip tests COMPLET)
RUN mvn clean package -Dmaven.test.skip=true

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=build /app/sms-gateway-service/target/*.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]