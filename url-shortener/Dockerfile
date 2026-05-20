# ==========================================
# STAGE 1: Build & Compile Environment
# ==========================================
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml and download project dependencies to cache them inside the Docker layer
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build the final executable production jar file without running unit tests
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# STAGE 2: Lightweight Production Runtime
# ==========================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy only the compiled fat JAR from the build stage container
COPY --from=build /app/target/*.jar app.jar

# Expose the standard web server port
EXPOSE 8080

# Configure production-optimized JVM flags for smooth garbage collection and memory usage
ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]