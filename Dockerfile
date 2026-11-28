# ---- Stage 1: build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
COPY src ./src

RUN mvn -B -DskipTests package

# ---- Stage 2: run ----
FROM eclipse-temurin:21-jre

RUN groupadd --system app && useradd --system --gid app --no-create-home app

WORKDIR /app

COPY --from=build /build/target/*.jar app.jar

USER app

EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

ENTRYPOINT ["java", "-jar", "app.jar"]