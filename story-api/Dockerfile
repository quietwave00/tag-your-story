FROM openjdk:17-jdk-slim

COPY build/libs/*.jar ./app.jar

ENTRYPOINT ["java", "-Djasypt.encryptor.password=${JASYPT_PASSWORD}", "-Dspring.profiles.active=${PHASE}", "-jar", "app.jar"]