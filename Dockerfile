# ---- Stage 1: build (full JDK + Maven) ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
# Dependencies first: this layer is cached until pom.xml changes
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
# Tests run in CI; the Testcontainers tests need Docker, which isn't available here
RUN mvn -B -DskipTests package

# ---- Stage 2: run (just a JRE and the jar) ----
FROM eclipse-temurin:21-jre
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
USER app
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "-jar", "app.jar"]