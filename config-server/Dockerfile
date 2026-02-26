FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace

COPY pom.xml .
COPY config-server/pom.xml   ./config-server/
COPY eureka-server/pom.xml   ./eureka-server/
COPY auth-service/pom.xml    ./auth-service/
COPY api-gateway/pom.xml     ./api-gateway/
COPY inventory-service/pom.xml ./inventory-service/
COPY notification-service/pom.xml ./notification-service/

COPY config-server/src ./config-server/src

RUN mvn package -pl config-server -DskipTests -B && \
    cp config-server/target/*.jar app.jar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
COPY config-server/config ./config
EXPOSE 8888
ENTRYPOINT ["java", "-jar", "app.jar"]
