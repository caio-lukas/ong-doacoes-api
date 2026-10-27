# syntax=docker/dockerfile:1

# ---------- Etapa 1: compila o projeto ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app


COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline

# Os testes rodam no CI (precisam de Docker), não dentro do build da imagem
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests


FROM eclipse-temurin:17-jre
WORKDIR /app

# A aplicação não precisa de root
RUN groupadd --system app && useradd --system --gid app --no-create-home app
COPY --from=build /app/target/*.jar app.jar
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
