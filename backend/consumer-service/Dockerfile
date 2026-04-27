FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY backend ./backend
COPY shared ./shared
RUN mvn -pl backend/consumer-service -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/backend/consumer-service/target/consumer-service-0.1.0-SNAPSHOT.jar app.jar
COPY --from=build /workspace/shared /app/shared
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
