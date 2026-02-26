FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace

COPY pom.xml .
COPY config-server/pom.xml   ./config-server/
COPY eureka-server/pom.xml   ./eureka-server/
COPY auth-service/pom.xml    ./auth-service/
COPY api-gateway/pom.xml     ./api-gateway/
COPY inventory-service/pom.xml ./inventory-service/
COPY notification-service/pom.xml ./notification-service/

COPY inventory-service/src ./inventory-service/src

RUN mvn package -pl inventory-service -DskipTests -B && \
    cp inventory-service/target/*.jar app.jar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
