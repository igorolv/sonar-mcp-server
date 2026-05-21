# AGENTS.md — Engineering Guide for AI Coding Agents

This file is for AI agents (and humans) **modifying the source code** of this repository.
It is **not** a setup guide for end users — for that, see:

- [README.md](README.md) — product description, MCP tool catalogue, env vars, security model, client-specific setup.

Read this file before changing code.

---

## 1. What this project is

A local **MCP (Model Context Protocol) server** that exposes a SonarQube 9 instance to AI clients
(Claude Code, Cursor, VS Code Copilot, Qwen Code, …) over **stdio**. It is **read-only**: it never
creates, updates, or deletes anything in SonarQube.

Core invariants — never break these without an explicit conversation:

1. **Read-only.** No MCP tool may issue `POST`, `PUT`, `DELETE`, or `PATCH` against SonarQube web-api.
   Every `@McpTool` is annotated with `readOnlyHint = true, destructiveHint = false, idempotentHint = true`.
2. **Stdio only.** The server has `spring.main.web-application-type: none`. It must never open
   an HTTP port, never write to `System.out` (stdout is the MCP transport channel — anything
   written there corrupts the JSON-RPC stream).
3. **Immediate execution for tool calls.** `McpServerConfig` sets `immediateExecution(true)` on
   the `McpSyncServer` builder. Do not switch to async/reactive without re-evaluating this.
4. **Wire format is the `api/*` package.** Tools return records from `ru.it_spectrum.ai.sonar.mcp.api`,
   never raw `client.model.*` types. The `api` types are the **stable MCP contract**;
   `client.model` types track SonarQube's REST shape and may change.

---

## 2. Tech stack and exact versions

- **Java 25** toolchain (`build.gradle.kts` pins `JavaLanguageVersion.of(25)`).
- **Spring Boot 4** + **Spring AI MCP server** (stdio transport) — version aliases in
  `gradle/libs.versions.toml`.
- Jackson Databind for JSON; ObjectMapper configured with `NON_NULL` inclusion (`JsonConfig`).
- **Gradle 9.x** with version catalog (`libs.versions.toml`).

If you change a dependency, update `libs.versions.toml`, not the build script.

---

## 3. Build, run, test

All commands assume `JAVA_HOME` points to JDK 25+.

```bash
./gradlew build              # compile + unit tests + bootJar
./gradlew test               # unit tests only (excludes @Tag("integration"))
./gradlew integrationTest    # smoke tests against a live SonarQube (needs SONAR_URL/TOKEN)
./gradlew bootJar            # build/libs/sonar-mcp-server.jar
```

To run the server manually:

```bash
SONAR_URL=... SONAR_TOKEN=... java -jar build/libs/sonar-mcp-server.jar
```

The server runs in stdio mode; logs go to `${SONAR_MCP_DATA_DIR:-~/.sonar-mcp-server}/logs/`.

---

## 4. Layered architecture

```
tools/      @Service classes with @McpTool methods. Thin: log, default pagination, call services, log again.
service/    Business logic. Validate inputs, orchestrate one or more client calls, map raw -> api/*.
api/        Stable wire format (records). Returned by tools and services.
client/     SonarClient (RestClient wrapper) + client/model/* (raw SonarQube DTOs).
config/     Spring config: RestClient with Basic auth, MCP customizer, properties, Jackson.
```

Direction of dependencies is one-way: `tools` -> `service` -> `client`. Never call the client
directly from a tool — go through a service. Never return a `client.model.*` type from a tool.

---

## 5. Conventions

- **Authentication.** SonarQube 9 user tokens are sent as HTTP Basic auth with token as username
  and empty password. `SonarConfig` builds the `Authorization: Basic base64(token:)` header.
- **Pagination.** External API is `offset`/`limit`. Internally we convert to Sonar's `p`/`ps` via
  `PaginationHelper.toPage`. If `offset` is not a multiple of `limit`, Sonar returns the page
  containing it (we round down).
- **Component scoping.** Issue tools do not expose a friendly `path` filter. Use `listComponents` to resolve modules,
  directories, files, or Java/Kotlin package names to Sonar component `key` values, then pass those opaque keys unchanged
  as `componentKeys`. Raw Sonar filters are also exposed as `directories` and `files`; do not pass package names directly
  as `componentKeys`.
- **Default scope for listIssues.** When both `statuses` and `resolved` are omitted, the service
  defaults to `resolved=false` + `statuses=OPEN,CONFIRMED,REOPENED` (open issues only).
- **Component path resolution.** `SonarMappers.toIssue` resolves `componentPath` from the
  components map embedded in the Sonar response; falls back to splitting the key on the first `:`.
- **Rule caching.** `RuleService` keeps an in-memory `ConcurrentHashMap` keyed by rule key,
  no TTL. Rules are stable for years.
- **Errors.** Throw `IssueNotFoundException`, `HotspotNotFoundException`, `RuleNotFoundException`
  (all extend `ResourceNotFoundException`) for "not found" cases. Spring MCP will surface them to
  the client. Use `IllegalArgumentException` for caller mistakes (missing required parameter, etc.).
- **Logging.** Tools log entry args with `log.info("Tool call: <name> (...)")`. Always finish with
  `ToolLogger.completed(...)` or `ToolLogger.failed(...)`. Never write to stdout.
- **Branch scoping is load-bearing.** Sonar analyses each branch / pull-request independently —
  data on `main` and on a feature branch can diverge sharply. The calling LLM has no project
  context, so it tends to omit `branch=` and silently read `main`. Every branch-aware tool's
  description and every `branch` parameter description must (1) state that the default is `main` /
  `SONAR_DEFAULT_BRANCH`, (2) tell the agent to call `listProjectBranches` /
  `listProjectPullRequests` when the user is on a non-main ref, and (3) push for an explicit
  argument rather than the silent default. Reuse the shared constants in
  `ToolDescriptions` (`BRANCH_NOTE`, `BRANCH_PARAM`, `PR_PARAM`,
  `BRANCH_PARAM_FOR_KEY_LOOKUP`) so the message stays consistent across tools. The same applies
  to `@McpPrompt` instructions — when the branch/PR arg is empty, the prompt must tell the agent
  to verify the scope, not "do not pass branch".

---

## 6. Where to add new things

| Adding... | Where |
|---|---|
| A new SonarQube endpoint call | `SonarClient` + a raw DTO in `client/model/` |
| A public response type | `api/` as a `record` |
| Business logic | `service/`, mapping raw -> api in `SonarMappers` |
| A new MCP tool | `tools/`, `@Service` class, `@McpTool` method, return a record from `api/` |
| Env var or runtime tunable | `SonarMcpProperties` (nested record) + `application.yml` entry |

---

## 7. Tests

- **Unit tests** (`./gradlew test`) — Mockito-based, no Spring context. `SonarClientTest` uses
  `MockRestServiceServer.bindTo(builder)` to verify URL building and JSON parsing without
  hitting the network. Service tests mock `SonarClient`.
- **Integration tests** (`@Tag("integration")`, `./gradlew integrationTest`) — boot the whole
  Spring context and call a live SonarQube using `SONAR_URL` and `SONAR_TOKEN` from env.
  Excluded from the default `test` task.

Add a unit test for any new mapping rule, parameter default, or error path.

---

## 8. Don'ts

- Don't add a tool that writes to SonarQube.
- Don't print to `System.out` (use SLF4J -> the file/stderr appenders).
- Don't return raw `client.model.*` from tools.
- Don't bypass `SonarMappers` — keeping mapping in one place makes wire-format changes easy.
- Don't widen `componentKeys` behaviour without checking Sonar 9 docs first.
- Don't add a dependency on PDFBox / POI / Tika — there are no binary attachments here.
