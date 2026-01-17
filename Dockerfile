# syntax=docker/dockerfile:1

# -----------------------------------------------------------------------------
# Stage 1: Build the application
# -----------------------------------------------------------------------------
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app

# Copy gradle wrapper and settings
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
COPY infraestructure/build.gradle infraestructure/
COPY domain/build.gradle domain/
COPY application/build.gradle application/

# Download dependencies
RUN chmod +x gradlew && sed -i 's/\r$//' gradlew
RUN ./gradlew dependencies --no-daemon || true

# Copy source code
COPY . .
RUN chmod +x gradlew && sed -i 's/\r$//' gradlew

# Build the application (skip tests)
RUN ./gradlew :infraestructure:bootJar -x test --no-daemon

# Extract the layered jar
WORKDIR /app/infraestructure/build/libs
RUN java -Djarmode=layertools -jar infraestructure-0.0.1-SNAPSHOT.jar extract

# -----------------------------------------------------------------------------
# Stage 2: Create the CDS Archive (Intermediate Stage)
# -----------------------------------------------------------------------------
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app

# Copy layers from BUILDER to prepare for CDS generation
# Note: copying to "./" flattens the folders, recreating the exploded jar structure at /app/
COPY --from=builder /app/infraestructure/build/libs/dependencies/ ./
COPY --from=builder /app/infraestructure/build/libs/spring-boot-loader/ ./
COPY --from=builder /app/infraestructure/build/libs/snapshot-dependencies/ ./
COPY --from=builder /app/infraestructure/build/libs/application/ ./

# Generate CDS archive (application.jsa)
RUN java -XX:ArchiveClassesAtExit=application.jsa \
    -Dspring.context.exit=onRefresh \
    org.springframework.boot.loader.launch.JarLauncher || true

# -----------------------------------------------------------------------------
# Stage 3: Final Runtime Image
# -----------------------------------------------------------------------------
FROM eclipse-temurin:25-jre
WORKDIR /app

# 1. Copy layers from BUILDER (Source of Truth)
# We copy from builder again to ensure we get the clean layers
COPY --from=builder /app/infraestructure/build/libs/dependencies/ ./
COPY --from=builder /app/infraestructure/build/libs/spring-boot-loader/ ./
COPY --from=builder /app/infraestructure/build/libs/snapshot-dependencies/ ./
COPY --from=builder /app/infraestructure/build/libs/application/ ./

# 2. Copy ONLY the JSA file from RUNTIME
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