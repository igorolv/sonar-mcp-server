# Contributing to Sonar MCP Server

Thank you for considering a contribution! This document covers how to set up the project,
what conventions the codebase follows, and how to submit changes.

For the product overview, tool catalogue and configuration see [README.md](README.md).
For the full engineering guide (architecture, conventions, invariants) see [AGENTS.md](AGENTS.md).

## Prerequisites

- **JDK 25+** (`build.gradle.kts` pins the Java toolchain to 25).
- Git. Gradle itself is wrapped (`gradlew`) — no local installation needed.

## Building and testing

```bash
./gradlew build              # compile + unit tests + bootJar
./gradlew test               # unit tests only (integration tests excluded)
./gradlew integrationTest    # smoke tests against a live SonarQube
./gradlew bootJar            # build/libs/sonar-mcp-server.jar
```

Integration tests need a reachable SonarQube Community Build 26.4+ instance:

```bash
SONAR_URL=https://your-sonar.example.com SONAR_TOKEN=your-token ./gradlew integrationTest
```

Unit tests are Mockito-based and do not boot Spring or touch the network.
Before opening a PR, make sure `./gradlew build` passes on your machine.

## Project layout

```
src/main/java/ru/it_spectrum/ai/sonar/mcp/
  tools/      @McpTool entry points (thin: log, paginate, delegate, log again)
  service/    business logic, raw -> api mapping orchestration
  api/        stable wire-format records returned by tools
  client/     SonarClient (RestClient wrapper) + raw SonarQube DTOs
  config/     Spring configuration, properties, Jackson setup
```

Dependencies flow one way only: `tools -> service -> client`. Tools never call the client
directly and never return `client.model.*` types — everything crossing the wire comes from `api/`.

## Non-negotiable invariants

A PR will be rejected if it breaks any of these (details in AGENTS.md):

1. **Read-only.** No tool may issue `POST` / `PUT` / `DELETE` / `PATCH` against SonarQube.
2. **Stdio transport only.** Never open an HTTP port, never write to `System.out`.
3. **Wire format is `api/*`.** Raw client DTOs stay inside the service/client layers.
4. **No raw `componentKeys` / `directories` / `files` parameters** on issue/hotspot listing tools —
   the single LLM-friendly `componentPathPrefix` parameter is intentional.
5. **Branch scoping stays load-bearing.** Branch-aware tool descriptions must keep telling the agent
   that the default is `main` and to verify scope via `listProjectBranches` /
   `listProjectPullRequests`. Reuse the shared constants in `ToolDescriptions`.

## Coding conventions

- Map all raw responses through `SonarMappers`; do not hand-map fields inside tools.
- Throw `ResourceNotFoundException` subclasses for "not found", `IllegalArgumentException`
  for caller mistakes.
- Keep `@Schema` annotations on `api/*` records lean — no examples, no descriptions that restate
  the field name; wrap heavy nested types in `Opaque<T>` where the typed schema is not worth its bytes.
- New tool classes must carry a `sonar-mcp.tools.*` `@ConditionalOnProperty` gate and be listed in
  `application.yml` and `ToolGroupConditionTest`.
- Add a unit test for every new mapping rule, parameter default or error path.
- If you change a dependency version, update `gradle/libs.versions.toml`, not the build script.

## Commit messages

Use short conventional-style summaries, as in the existing history:

```
feat: add X
fix: correct Y
docs: sync Z with code
tools: ...       # tool-surface changes
api: ...         # wire-format changes
perf: ...
refactor: ...
```

## Pull requests

1. Fork / branch from `main`, keep the change focused — one logical topic per PR.
2. Run `./gradlew build` and confirm it passes.
3. Update documentation alongside behavior:
   user-facing changes → `README.md`; engineering conventions → `AGENTS.md`.
4. Describe *why* the change is needed in the PR description, not just what it does.
5. Do not commit secrets (`SONAR_TOKEN`, local config) or IDE files.

## Reporting bugs

Open a GitHub issue and include:

- Server JAR version or commit the JAR was built from;
- AI client used (Claude Code, Cursor, VS Code Copilot, …);
- SonarQube edition/version and mode (Standard Experience or MQR);
- The failing tool name and arguments;
- Relevant excerpt from `${SONAR_MCP_DATA_DIR:-~/.sonar-mcp-server}/logs/`
  (redact tokens and private URLs first).

## Licensing

By contributing you agree that your contributions are licensed under the
[MIT License](LICENSE).
