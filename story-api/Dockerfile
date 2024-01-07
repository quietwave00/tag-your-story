FROM adoptopenjdk/openjdk17:alpine

COPY build/libs/*.jar ./app.jar

ENTRYPOINT ["java", "-Djasypt.encryptor.password=${JASYPT_PASSWORD}", "-Dspring.profiles.active=${PHASE}", "-jar", "app.jar"]