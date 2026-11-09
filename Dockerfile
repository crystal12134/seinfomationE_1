# 1단계: Render 서버 안에서 메이븐으로 파일 조립(빌드)하기
FROM maven:3.9.6-eclipse-temurin-21 AS build
COPY . .
# 뒤에 -DskipTests -Dmaven.test.skip=true 옵션을 확실하게 붙여줍니다!
RUN mvn clean package -DskipTests -Dmaven.test.skip=true

# 2단계: 조립된 파일을 실행하기
FROM eclipse-temurin:21-jdk-jammy
COPY --from=build /target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
