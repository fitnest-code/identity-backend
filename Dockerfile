# -----------------------------
# Stage 1: Build JAR
# -----------------------------
FROM gradle:9.3-jdk25 AS builder
WORKDIR /app

# Cache dependencies
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src/main/proto src/main/proto
RUN ./gradlew dependencies --no-daemon

# Copy source and build
COPY . .
RUN ./gradlew clean bootJar --no-build-cache --no-daemon

# -----------------------------
# Stage 2: Runtime image
# -----------------------------
FROM eclipse-temurin:25-jre

# Create non-root user
RUN groupadd -g 1001 fitnest && \
    useradd -u 1001 -g fitnest -m -s /bin/bash fitnest

WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

# Ensure /tmp is writable for heap dumps
RUN chown -R fitnest:fitnest /app /tmp
USER fitnest

EXPOSE 8080

# Using simple exec form. JVM will pick up JAVA_TOOL_OPTIONS from environment.
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:-'-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseZGC -XX:+ZGenerational'} -jar app.jar"]
