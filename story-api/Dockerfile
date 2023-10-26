FROM adoptopenjdk/openjdk11

COPY build/libs/*.jar ./app.jar

ENTRYPOINT ["java", "-Djasypt.encryptor.password=${JASYPT_PASSWORD}", "-Dspring.profiles.active=${PHASE}", "-jar", "app.jar"]