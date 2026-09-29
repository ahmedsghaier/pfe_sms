FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Copie directement le contenu du service (pom.xml + src)
# Le contexte Docker est ./backend, donc le chemin est sms-recommender-service/
COPY sms-recommender-service/pom.xml ./pom.xml
COPY sms-recommender-service/src ./src

# Cache des dépendances
RUN mvn dependency:resolve -B --no-transfer-progress

# Build
RUN mvn clean package -DskipTests -B --no-transfer-progress

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=builder /app/target/sms-recommender-service-0.0.1-SNAPSHOT.jar app.jar

RUN chown spring:spring app.jar

USER spring

EXPOSE 8087

ENTRYPOINT ["java", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-jar", \
            "app.jar"]