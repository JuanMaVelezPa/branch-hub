FROM gradle:9.1.0-jdk25 AS build
WORKDIR /app
COPY build.gradle settings.gradle gradlew gradlew.bat ./
COPY gradle gradle
COPY src src
RUN sed -i 's/\r$//' ./gradlew \
    && chmod +x ./gradlew \
    && ./gradlew --no-daemon clean bootJar -x test

FROM eclipse-temurin:25-jre

# Used by docker-compose healthcheck (the JRE image does not ship with wget/curl).
RUN apt-get update \
    && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /app/build/libs/branch-hub-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
