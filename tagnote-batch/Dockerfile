FROM adoptopenjdk/openjdk11

COPY build/libs/*.jar /var/lib/jenkins/workspace/tag-note-batch/app.jar

WORKDIR /var/lib/jenkins/workspace/tag-note-batch

ENTRYPOINT ["java", "-Dspring.profiles.active=${PHASE}", "-jar", "app.jar"]
