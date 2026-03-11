FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY trend-watcher/pom.xml trend-watcher/pom.xml
COPY content-creator/pom.xml content-creator/pom.xml
COPY act-service/pom.xml act-service/pom.xml
COPY learn-service/pom.xml learn-service/pom.xml
RUN mvn -pl trend-watcher -am dependency:go-offline -q 2>/dev/null || true
COPY trend-watcher/src trend-watcher/src
RUN mvn -pl trend-watcher -am package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/trend-watcher/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
