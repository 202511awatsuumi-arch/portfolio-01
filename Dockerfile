FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace
COPY spring-app/ ./spring-app/

WORKDIR /workspace/spring-app
RUN chmod +x ./mvnw
RUN ./mvnw -DskipTests package

FROM eclipse-temurin:17-jre-jammy AS runtime

WORKDIR /app
COPY --from=build /workspace/spring-app/target/*.jar ./app.jar

EXPOSE 10000

ENTRYPOINT ["java", "-Dserver.port=10000", "-jar", "/app/app.jar"]
