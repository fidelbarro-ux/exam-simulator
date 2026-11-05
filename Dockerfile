# BUILD STAGE
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

RUN JAR=$(find target -maxdepth 1 -type f \
    -name '*.jar' ! -name '*.original' | head -n 1) \
    && cp "$JAR" /app/app.jar

# RUNTIME STAGE
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/app.jar app.jar

EXPOSE 10000

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-jar", "app.jar"]