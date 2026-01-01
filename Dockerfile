# syntax=docker/dockerfile:1

# -----------------------------------------------------------------------------
# Stage 1: Build the application
# -----------------------------------------------------------------------------
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app

# Copy gradle wrapper and settings first for caching
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
COPY infraestructure/build.gradle infraestructure/
COPY domain/build.gradle domain/
COPY application/build.gradle application/

# Download dependencies (fail-safe for offline work, optional but good for caching)
RUN chmod +x gradlew && sed -i 's/\r$//' gradlew
RUN ./gradlew dependencies --no-daemon || true

# Copy source code
COPY . .
RUN chmod +x gradlew && sed -i 's/\r$//' gradlew

# Build the application (skip tests for speed)
RUN ./gradlew :infraestructure:bootJar -x test --no-daemon

# Extract the layered jar
WORKDIR /app/infraestructure/build/libs
RUN java -Djarmode=layertools -jar infraestructure-0.0.1-SNAPSHOT.jar extract

# -----------------------------------------------------------------------------
# Stage 2: Create the runtime image with CDS
# -----------------------------------------------------------------------------
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app

# Copy layers from builder
COPY --from=builder /app/infraestructure/build/libs/dependencies/ ./
COPY --from=builder /app/infraestructure/build/libs/spring-boot-loader/ ./
COPY --from=builder /app/infraestructure/build/libs/snapshot-dependencies/ ./
COPY --from=builder /app/infraestructure/build/libs/application/ ./

# Perform CDS (Class Data Sharing) training
# This starts the app, dumps the cache, and exits
RUN java -XX:ArchiveClassesAtExit=application.jsa \
    -Dspring.context.exit=onRefresh \
    -jar application.jar || true

# -----------------------------------------------------------------------------
# Stage 3: Final Runtime Image
# -----------------------------------------------------------------------------
FROM eclipse-temurin:25-jre
WORKDIR /app

# Copy layers and CDS archive
COPY --from=runtime /app/dependencies/ ./
COPY --from=runtime /app/spring-boot-loader/ ./
COPY --from=runtime /app/snapshot-dependencies/ ./
COPY --from=runtime /app/application/ ./
COPY --from=runtime /app/application.jsa ./

# Create a non-root user
RUN addgroup --system --gid 1001 wedge && \
    adduser --system --uid 1001 --ingroup wedge wedge
USER wedge

# Application configuration
ENV SERVER_PORT=9001
EXPOSE 9001

# Run with CDS enabled
ENTRYPOINT ["java", "-XX:InitialRAMPercentage=80", "-XX:MaxRAMPercentage=80", "-XX:SharedArchiveFile=application.jsa", "org.springframework.boot.loader.launch.JarLauncher"]
