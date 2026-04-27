FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY backend ./backend
COPY shared ./shared
RUN mvn -pl backend/ingestion-service -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/backend/ingestion-service/target/ingestion-service-0.1.0-SNAPSHOT.jar app.jar
COPY --from=build /workspace/shared /app/shared
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
