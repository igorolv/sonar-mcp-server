# Multi-stage build: compile the fat jar with a JDK, run it on a slim JRE.
#
#   docker build -t sonar-mcp-server .
#   docker run -i --rm -e SONAR_URL=https://sonar.example.com -e SONAR_TOKEN=... sonar-mcp-server
#
# /data holds the server's local files (logs); mount it to keep them between runs.

FROM eclipse-temurin:25-jdk AS build
WORKDIR /src

# Resolve the Gradle distribution and dependencies first so source edits reuse this layer.
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
# A Windows checkout may hand us gradlew with CRLF endings and no executable bit.
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew \
    && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

COPY src ./src
RUN ./gradlew --no-daemon bootJar

FROM eclipse-temurin:25-jre
LABEL org.opencontainers.image.source="https://github.com/igorolv/sonar-mcp-server" \
      org.opencontainers.image.description="Read-only MCP server for self-hosted SonarQube Community Build: issues, hotspots, rules, code snippets" \
      org.opencontainers.image.licenses="MIT" \
      io.modelcontextprotocol.server.name="io.github.igorolv/sonar-mcp-server"

RUN useradd --system --create-home --uid 10001 mcp \
    && mkdir -p /data && chown mcp:mcp /data
USER mcp
WORKDIR /app

COPY --from=build --chown=mcp:mcp /src/build/libs/sonar-mcp-server.jar ./sonar-mcp-server.jar

ENV SONAR_MCP_DATA_DIR=/data
VOLUME ["/data"]

# stdio transport: the MCP client talks over stdin/stdout, logs go to stderr and /data/logs.
ENTRYPOINT ["java", "-jar", "/app/sonar-mcp-server.jar"]
