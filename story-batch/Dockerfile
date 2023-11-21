FROM adoptopenjdk/openjdk11

COPY build/libs/*.jar /var/lib/jenkins/workspace/tag-your-story-batch/app.jar

WORKDIR /var/lib/jenkins/workspace/tag-your-story-batch

ENTRYPOINT ["java", "-Djasypt.encryptor.password=${JASYPT_PASSWORD}", "-Dspring.profiles.active=${PHASE}", "-jar", "app.jar"]