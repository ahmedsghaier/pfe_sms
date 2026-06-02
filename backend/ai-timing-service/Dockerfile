# ========================================
# BUILD STAGE
# ========================================
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B


# ========================================
# RUNTIME STAGE
# ========================================
FROM eclipse-temurin:17-jre-jammy

# Install Python (clean)
RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    python3-dev \
    wget \
    && ln -s /usr/bin/python3 /usr/bin/python \
    && rm -rf /var/lib/apt/lists/*

# Fix matplotlib crash (IMPORTANT)
ENV MPLCONFIGDIR=/tmp/matplotlib

# Create user
RUN groupadd -r spring && useradd -r -g spring spring

WORKDIR /app

# Python dependencies (ML ONLY)
RUN pip3 install --no-cache-dir \
    numpy==1.26.4 \
    pandas==2.2.2 \
    scikit-learn==1.4.2 \
    xgboost==2.0.3 \
    lightgbm==4.3.0 \
    catboost==1.2.5 \
    joblib==1.4.2 \
    scipy==1.13.0 \
    seaborn==0.13.2

# Copy JAR
COPY --from=builder /build/target/*.jar app.jar

# Create folders
RUN mkdir -p /app/models /app/python /app/python/mongodb_synthetic /app/logs && \
    chown -R spring:spring /app

# Copy python + models + dataset
COPY --chown=spring:spring src/main/resources/python /app/python
COPY --chown=spring:spring src/main/resources/models /app/models
COPY --chown=spring:spring src/main/resources/mongodb_synthetic /app/python/mongodb_synthetic

# Permissions
RUN chmod +x /app/python/*.py || true

USER spring:spring

ENV PYTHON_PATH=/usr/bin/python3
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# Healthcheck
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-docker} -jar app.jar"]