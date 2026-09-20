FROM eclipse-temurin:25-jre

RUN apt-get update \
 && apt-get install -y --no-install-recommends git ca-certificates \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY target/git-stat-0.0.1-SNAPSHOT.jar app.jar

ENV GIT_STORAGE_PATH=/var/lib/git-stat/repos
RUN mkdir -p /var/lib/git-stat/repos

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]