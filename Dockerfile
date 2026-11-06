# 1단계: 빌드 환경 (Render 서버 안에서 직접 최신 소스코드를 다운받아 압축파일을 구워냅니다)
FROM maven:3.9.6-eclipse-temurin-21-jammy AS build
COPY . .
RUN mvn clean package -DskipTests

# 2단계: 실행 환경 (방금 구워낸 청정 최종 알맹이 파일만 쏙 빼와서 서버를 구동합니다)
FROM eclipse-temurin:21-jdk-jammy
COPY --from=build /target/seinfomationE_1-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
