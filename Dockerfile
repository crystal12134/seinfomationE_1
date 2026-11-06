FROM eclipse-temurin:21-jdk-jammy
COPY target/seinfomationE_1-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
