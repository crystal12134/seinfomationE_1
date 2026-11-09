#dockerfile
FROM maven:3.8.8-eclipse-temurin-17 AS build
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jdk-jammy
COPY --from=build /target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]