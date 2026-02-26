FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace

COPY pom.xml .
COPY config-server/pom.xml   ./config-server/
COPY eureka-server/pom.xml   ./eureka-server/
COPY auth-service/pom.xml    ./auth-service/
COPY api-gateway/pom.xml     ./api-gateway/
COPY inventory-service/pom.xml ./inventory-service/
COPY notification-service/pom.xml ./notification-service/

COPY notification-service/src ./notification-service/src

RUN mvn package -pl notification-service -DskipTests -B && \
    cp notification-service/target/*.jar app.jar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
