# 1단계: Render 서버 안에서 메이븐으로 파일 조립(빌드)하기
FROM maven:3.9.6-eclipse-temurin-21 AS build
COPY . .
RUN mvn clean package -DskipTests

# 2단계: 조립된 파일을 실행하기
FROM eclipse-temurin:21-jdk-jammy
COPY --from=build /target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
