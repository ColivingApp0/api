# ---- Build stage -----------------------------------------------------------
# Compiles the Spring Boot fat jar with the Gradle wrapper pinned in the repo.
# The toolchain is JDK 21 (see build.gradle.kts); no tests run here — Render
# deploys the artifact, the test suite belongs to CI.
FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

# Wrapper first: this layer is cached until the build scripts change.
COPY gradle/ gradle/
COPY gradlew settings.gradle.kts build.gradle.kts ./
RUN chmod +x gradlew

COPY src/ src/

RUN ./gradlew bootJar --no-daemon --console=plain

# ---- Runtime stage ---------------------------------------------------------
FROM eclipse-temurin:21-jre

RUN groupadd --system app && useradd --system --gid app --home /app app

WORKDIR /app
COPY --from=build /workspace/build/libs/*-SNAPSHOT.jar app.jar

USER app
EXPOSE 8080

# Render injects PORT (default 10000); Spring Boot has no PORT convention, so
# the entrypoint maps it onto --server.port. MaxRAMPercentage keeps the JVM
# inside the container limit on the small Render plans.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar --server.port=${PORT:-8080}"]
