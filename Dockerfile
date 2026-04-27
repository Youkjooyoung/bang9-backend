FROM gradle:8.10-jdk21 AS build
WORKDIR /workspace
COPY settings.gradle build.gradle gradle.properties ./
COPY src ./src
RUN gradle bootWar --no-daemon -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/build/libs/bang9-backend.war app.war
EXPOSE 8080
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.war"]
