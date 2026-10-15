FROM mcr.microsoft.com/playwright/java:v1.45.0-jammy
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests
ENTRYPOINT ["java", "-jar", "target/playwright-json-cli-1.0.0.jar"]