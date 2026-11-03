FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline
COPY src src
COPY resources resources
RUN mvn -B -ntp verify

FROM eclipse-temurin:17-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system jira && useradd --system --gid jira jira
WORKDIR /opt/jirarush
COPY --from=build /build/target/jira-1.0.jar app.jar
COPY --from=build /build/resources resources
RUN mkdir attachments logs && chown -R jira:jira /opt/jirarush
USER jira
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=6 \
    CMD curl -fsS http://localhost:8080/v3/api-docs > /dev/null || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
