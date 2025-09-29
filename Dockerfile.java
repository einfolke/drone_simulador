﻿# syntax=docker/dockerfile:1
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/drone-simulador-*-jar-with-dependencies.jar /app/app.jar
EXPOSE 8080
CMD ["java", "-cp", "/app/app.jar", "com.drone.simulador.api.ApiLauncher"]
