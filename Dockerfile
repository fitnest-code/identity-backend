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

ENTRYPOINT [ \
  "java", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-jar", "app.jar" \
]
