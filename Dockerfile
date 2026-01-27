
FROM gradle:8.5-jdk17 AS build

WORKDIR /build

COPY build.gradle settings.gradle ./
COPY gradle ./gradle

RUN gradle dependencies --no-daemon || true

COPY src ./src
RUN gradle bootJar --no-daemon -x test

FROM gcr.io/distroless/java17-debian12@sha256:fd925ba431f3a6c1f1c8114ce1999ca38803220baf0fdf25a4c71b38db8af67f

ARG BUILD_DATE=unknown
ARG VCS_REF=unknown
ARG VERSION=1.0.0

LABEL org.opencontainers.image.title="IAM Service" \
      org.opencontainers.image.description="Identity and Access Management (IAM) Service for Fitnest system. Handles authentication, OTP, registration, and token lifecycle management." \
      org.opencontainers.image.version="${VERSION}" \
      org.opencontainers.image.vendor="Fitnest" \
      org.opencontainers.image.authors="Fitnest Team" \
      org.opencontainers.image.source="https://gitlab.com/fitnest-backend/iam-service" \
      org.opencontainers.image.created="${BUILD_DATE}" \
      org.opencontainers.image.revision="${VCS_REF}"

WORKDIR /app

COPY --from=build --chown=nonroot:nonroot /build/build/libs/iam-service.jar app.jar

USER nonroot:nonroot

EXPOSE 8080

ENTRYPOINT [ \
  "java", \
  "-XX:+UseContainerSupport", \
  "-XX:+AlwaysPreTouch", \
  "-XX:MinRAMPercentage=40.0", \
  "-XX:InitialRAMPercentage=50.0", \
  "-XX:MaxRAMPercentage=70.0", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-XX:+HeapDumpOnOutOfMemoryError", \
  "-Djava.security.egd=file:/dev/urandom", \
  "-jar", \
  "app.jar" \
]
