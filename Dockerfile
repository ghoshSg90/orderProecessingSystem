# syntax=docker/dockerfile:1

# ---- Build stage: compile + package the fat jar ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Cache dependencies first (only re-runs when pom.xml changes)
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

# Build (tests skipped here; the context-load test needs a live DB)
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# ---- Runtime stage: slim JRE, non-root ----
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# curl is used by the container HEALTHCHECK / compose healthcheck
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system spring \
    && useradd --system --gid spring spring

COPY --from=build /workspace/target/*.jar app.jar
RUN chown spring:spring app.jar
USER spring

EXPOSE 8080

# App serves under the /orderProcessingSystem context path
HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=5 \
    CMD curl -fsS http://localhost:8080/orderProcessingSystem/v1/healthCheck/readiness || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
