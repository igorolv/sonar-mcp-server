# Sonar MCP Server

A local MCP server providing read-only access to a corporate SonarQube 9 via its web-api.
It lets AI agents (Claude Code, Cursor, VS Code Copilot, etc.) fetch a project's issue list, the files and locations where they occur, rule descriptions, source-code snippets around issues, and Security Hotspots.

Typical scenario: "find and fix Sonar issues in such-and-such project" — the LLM calls `listIssues`, optionally `getRule` and `getIssueSnippets`, and edits files locally.

## Why this server

There is an official SonarSource MCP server, but it targets SonarQube **10+** (and SonarCloud) and its web-api contract. Many corporate installations are still on **SonarQube 9** — and that's exactly what this server is built for. It speaks the v9 web-api directly, with no v10 assumptions baked in.

It is also intentionally narrower in scope:

- **Read-only by design.** The server never creates, updates, or deletes anything in SonarQube — no marking issues as false-positive, no editing comments, no admin endpoints. The token's write permissions in SonarQube are irrelevant because the server never calls those endpoints.
- **Curated tool set.** Instead of mirroring the SonarQube API surface, the server exposes a small, focused set of tools (12 in total) chosen for a single workflow: *let an AI agent read Sonar's findings and fix the code based on them*. Listing components, issues and hotspots, drilling into a single finding, fetching the rule explanation, and pulling the source-code snippet around the location — and that's it. Anything outside this "diagnose -> fix the code locally" loop is deliberately left out to keep the tool list small and the agent's choices unambiguous.

In short: a focused, read-only bridge from SonarQube 9 to an AI coding agent.

## Quick start

1. Install JDK 25+.
2. Build the server: `./gradlew build`.
3. Get your SonarQube URL and user token.
4. Add the resulting JAR to your client's MCP configuration (see [Connecting to an AI client](#connecting-to-an-ai-client)).

## Architecture

The server only supports the `stdio` transport.

```
┌─────────────┐     stdio      ┌──────────────────┐    web-api     ┌──────────┐
│  AI agent   │ <------------> │  sonar-mcp-      │ -------------> │ SonarQube│
│ (Claude Code│   stdin/stdout │  server (Java)   │  HTTP + Basic  │   v9     │
│  Cursor...) │                │                  │  auth (token)  │          │
└─────────────┘                └──────────────────┘                └──────────┘
```

The AI client spawns the server as a child process; communication uses the MCP protocol over stdin/stdout.
The server does not open any HTTP port and accepts no incoming connections.

## Tools

The server exports **12 read-only MCP tools**.

### Projects

| Tool | Description |
|---|---|
| `listProjects` | List of SonarQube projects. Parameters: `query` (name substring), `limit`, `offset`. Returns `key`, `name`, `qualifier`. |
| `listComponents` | Search/browse components inside a project using Sonar's component tree. Parameters: `projectKey`, `query`, `qualifiers`, `branch` / `pullRequest`, `limit`, `offset`. Returns opaque component `key` values plus `path`, `qualifier`, name, language, and project. Use returned `key` values unchanged as `listIssues.componentKeys`; do not pass Java package names directly as component keys. |
| `getProject` | Project overview: header info (name, qualifier, visibility, description, version, last analysis date), quality gate status with failed conditions, and curated metrics (ncloc, bugs, vulnerabilities, security hotspots, code smells, coverage, duplicated lines density, technical debt in minutes, alert status). Parameters: `projectKey`, `branch` (opt.), `pullRequest` (opt.). |
| `listProjectBranches` | List of branches analysed for the project. Each entry: `name`, `isMain`, `type` (LONG/SHORT/BRANCH), `excludedFromPurge`, `analysisDate`, `qualityGateStatus`, plus bugs/vulnerabilities/codeSmells counts. No pagination — Sonar returns all branches at once. |
| `listProjectPullRequests` | List of PR analyses for the project. Each entry: PR `key` (use as `pullRequest=` elsewhere), `title`, `branch` (head), `base`, `url`, `analysisDate`, `qualityGateStatus`, plus bugs/vulnerabilities/codeSmells counts. Empty list if the Sonar install has no DevOps integration. |

### Issues

| Tool | Description |
|---|---|
| `listIssues` | Flat list of issues for a project. Parameters: `projectKey` (required unless defaulted), Sonar filters `componentKeys`, `directories`, `files` (opt.), `severities`, `types`, `statuses`, `rules`, `branch` / `pullRequest` (mutually exclusive), `resolved`, `limit`, `offset`. Use `listComponents` first when the user gives a module, directory, file, or package name instead of an exact Sonar component key. By default returns only open issues (`resolved=false`, statuses OPEN/CONFIRMED/REOPENED). Each item contains the rule, severity, type, status, file path, line, primary textRange, and secondary flows for cross-file rules. |
| `getIssue` | Details of a single issue by key plus its change history (`changelog`). Accepts optional `branch` / `pullRequest`. |
| `getIssueSnippets` | Source-code snippets around all issue locations (primary plus flows for cross-file rules). For each location: `componentPath`, language, and an array of source lines with SCM info. Useful when the repository isn't available locally or you need to see exactly the file version Sonar analyzed. Accepts optional `branch` / `pullRequest` — important when the issue lives on a non-main ref whose files differ from main. |
| `getProjectIssuesSummary` | Aggregated summary of open issues in a project: total plus breakdowns by severity, type, status, rule, tag, and SCM author. Parameters mirror `listIssues` except pagination. |
| `getProjectIssuesBreakdown` | Multi-module aggregation of issues by logical module and rule. Module is derived from the first `componentPath` segment. Parameters mirror `getProjectIssuesSummary`. |

### Rules

| Tool | Description |
|---|---|
| `getRule` | Details of a Sonar rule by key (e.g. `java:S1234`): title, severity, type, language, tags, description sections (introduction, root cause, how to fix, resources). Backed by an in-memory cache — repeated calls are free. |

### Security Hotspots

| Tool | Description |
|---|---|
| `listHotspots` | List of Security Hotspots for a project. Hotspots are a separate category from issues, marking spots that require manual security review. By default Sonar returns hotspots in status `TO_REVIEW`. Parameters: `projectKey`, `path` (friendly componentPath/package filter, opt.), raw Sonar `files` filter (opt.), `status` (opt.), `branch` / `pullRequest` (opt., mutually exclusive), `limit`, `offset`. |
| `getHotspot` | Security Hotspot details: full rule description (risk, vulnerability, fix recommendations), primary textRange, secondary flows, changelog. Hotspot keys are globally unique, so no `branch`/`pullRequest` parameter is needed. |

All tools are **read-only** — no data in SonarQube is modified.

### Working with branches and pull requests

Sonar analyses a *branch* and a *pull request* as two distinct, mutually exclusive scopes. The Sonar web-api accepts either `branch=` or `pullRequest=` on a single request, never both.

- **`branch`** — long-lived branches (main, develop, feature/...). Resolved as: explicit `branch` argument → `SONAR_DEFAULT_BRANCH` → none (Sonar uses the project's main branch).
- **`pullRequest`** — the Sonar PR key, usually the PR/MR number. Independent from branch analyses; PR analyses often contain the most relevant findings for in-flight work. Pull request keys never fall back to a server-level default — pass them explicitly.

Passing both `branch` and `pullRequest` to the same tool call is an error. Use `listProjectBranches` / `listProjectPullRequests` to discover available refs.

## Stack

- Java 25, Spring Boot 4.0.0, Spring AI MCP 2.0.0-M6 (stdio transport)
- Jackson Databind for JSON
- Gradle 9.3.1 with version catalog (`gradle/libs.versions.toml`)

## Build

```bash
# Point to a JDK 25+ if it's not the default:
export JAVA_HOME="$HOME/.jdks/jdk-25.0.2"

./gradlew build
```

On Windows: `.\gradlew.bat build`.

Output: `build/libs/sonar-mcp-server.jar`

## Configuration

The server needs a SonarQube URL and token; the rest is optional.

| Variable | Description |
|---|---|
| `SONAR_URL` | SonarQube base URL (e.g. `http://sonar.example.com`) |
| `SONAR_TOKEN` | SonarQube user token |
| `SONAR_DEFAULT_PROJECT_KEY` | Default SonarQube project key. When set, `listIssues`, `getProjectIssuesSummary`, `listHotspots` can be called without `projectKey`. |
| `SONAR_DEFAULT_BRANCH` | Default Sonar branch. When set, all branch-aware tools (`listIssues`, `getIssue`, `getIssueSnippets`, `getProjectIssuesSummary`, `listHotspots`) fall back to this branch when `branch` is omitted. Without it Sonar uses the project's main branch. |
| `SONAR_MCP_DATA_DIR` | Local data directory for the server; defaults to `~/.sonar-mcp-server` |
| `SONAR_MCP_PAGINATION_DEFAULT_LIMIT` | Default page limit for list tools; defaults to `50` |
| `SONAR_MCP_PAGINATION_DEFAULT_OFFSET` | Default offset for list tools; defaults to `0` |
| `SONAR_MCP_PAGINATION_MAX_LIMIT` | Max page limit (Sonar API itself caps at 500); defaults to `500` |
| `SONAR_MCP_SNIPPET_MAX_LINES` | Reserved for future per-snippet line cap; currently unused (Sonar picks the window itself). Defaults to `50` |

### Getting the SonarQube URL

Open SonarQube in a browser and copy the address from the location bar **without** the path — only scheme and host.

| In the browser address bar | URL value |
|---|---|
| `https://sonar.example.com/dashboard?id=...` | `https://sonar.example.com` |
| `http://192.168.1.50:9000/projects` | `http://192.168.1.50:9000` |
| `http://10.0.0.5/sonar/projects` | `http://10.0.0.5/sonar` |

> If SonarQube is reachable only by IP, use the IP as is. If it's deployed under a subpath (e.g. `/sonar`), include that in the URL as well.

### Getting a SonarQube token

1. Sign in to SonarQube with your account.
2. Open **My Account** -> **Security**.
3. In **Generate Tokens**, enter a token name and pick type **User Token**.
4. Click **Generate** — the token is shown only once. Copy it immediately.
5. Use the token value as `SONAR_TOKEN`.

> If a token is lost, you have to regenerate it — SonarQube doesn't display existing tokens again.

The server uses HTTP Basic auth, passing the token as the username with an empty password — this is the standard SonarQube 9 scheme for user tokens.

## Running

```bash
SONAR_URL=http://sonar.example.com SONAR_TOKEN=your_token \
  java -jar build/libs/sonar-mcp-server.jar
```

The server runs over `stdio`. After a successful start it opens no HTTP port and waits for MCP requests over stdin/stdout.

Logs are written to `${SONAR_MCP_DATA_DIR:-~/.sonar-mcp-server}/logs/sonar-mcp-server.log`.
The file rotates by date and size: `10MB`, retention `30` days, total cap `512MB`.

## Connecting to an AI client

```json
{
  "command": "java",
  "args": ["-jar", "<absolute-path>/sonar-mcp-server.jar"],
  "env": {
    "SONAR_URL": "http://sonar.example.com",
    "SONAR_TOKEN": "your_token"
  }
}
```

Where exactly:

| Client | How to connect |
|---|---|
| Claude Code | `claude mcp add --scope user -e SONAR_URL=... -e SONAR_TOKEN=... -- sonar java -jar /path/to/sonar-mcp-server.jar` |
| Qwen Code | `~/.qwen/settings.json` -> `"mcpServers"` -> `"sonar"`, or `qwen mcp add --scope user -e SONAR_URL=... -e SONAR_TOKEN=... sonar java -jar /path/to/sonar-mcp-server.jar` |
| VS Code | `.vscode/mcp.json` -> `"servers"` -> `"sonar"` |
| Cursor | `.cursor/mcp.json` -> `"mcpServers"` -> `"sonar"` |
| Claude Desktop | `claude_desktop_config.json` -> `"mcpServers"` -> `"sonar"` |

For CLI clients there are also commands to view and remove the registration: `claude mcp list` / `claude mcp remove --scope user sonar` (similarly for `qwen`).

After adding, restart the client.

### Example AI-agent prompts

```text
Find and fix Sonar issues in project my-project
Show the top 10 rules by number of open issues in project my-project
Show Sonar issues under src/main/java/com/example/foo
```

## Operations and security

This MCP server is meant to run locally next to the AI client. It opens no HTTP port and accepts no incoming network connections: the client starts the JAR as a child process and talks to it over `stdin/stdout`.

### Access model

- The server acts with the rights of the SonarQube user whose token is in `SONAR_TOKEN`.
- All MCP tools are read-only: the server does not create, modify, or delete issues, hotspots, rules, or projects.
- Available projects and issues are determined by the user's permissions in SonarQube. If the user can't see a project in SonarQube, the server shouldn't be able to access it either.
- Treat the token as a secret. Don't commit it to the repository, shell scripts, `.vscode/mcp.json`, `.cursor/mcp.json`, or any other shared project files.

### What data is sent to the AI client

The AI client receives exactly the data it requests through the MCP tools:

- the list of issues with rule, severity, type, file path, line number, message, tags, SCM author;
- issue change history;
- Sonar rule descriptions (including HTML/markdown);
- source-code snippets around issue locations (via `getIssueSnippets`);
- Security Hotspots and their details.

Before connecting to an external or cloud-based AI client, check your company's internal policies: source code may contain trade secrets.

## Diagnostics

Environment check:

```bash
java -version
echo "$SONAR_URL"
test -n "$SONAR_TOKEN" && echo "SONAR_TOKEN is set"
```

SonarQube web-api access check (HTTP Basic with the token as username and empty password):

```bash
curl -u "$SONAR_TOKEN:" "$SONAR_URL/api/components/search?qualifiers=TRK&p=1&ps=1"
```

The expected response is a JSON list of projects. `401 Unauthorized` means the token is invalid or expired; `403` means the user lacks permission for the API.

Build check:

```bash
./gradlew test
./gradlew build
```

Integration tests against a live SonarQube:

```bash
SONAR_URL=<url> SONAR_TOKEN=<token> ./gradlew integrationTest
```

Integration tests require a reachable SonarQube and real data. Unit tests exclude the `integration` tag by default.

### Known operational limitations

- HTTP timeouts and retry policy aren't separately configurable yet.
- The Sonar API uses page-based pagination (`p`/`ps`); the tools accept `offset`/`limit`, and an offset that is not a multiple of `limit` is rounded down to the nearest page boundary. Sonar also caps `ps` at 500.
- `path` is an MCP-side friendly filter matched against returned `componentPath` values. It accepts module roots (`bc-smev`), directories, exact files, and Java package notation (`com.example.foo`). Because it is filtered locally, the server may page through all matching Sonar issues before applying `offset`/`limit`.
- `componentKeys`, `directories`, and `files` are raw SonarQube filters. Use them only when you intentionally need Sonar's native component/directory/file semantics.
- The `author` field on `Issue` is the SCM author of the line where the issue occurred (populated by Sonar when an SCM provider is configured). Sonar doesn't return separate `scmAuthor`/`scmDate` fields in `issues/search`; for line-level SCM use `getIssueSnippets`.

## Project layout

```
├── src/main/java/ru/it_spectrum/ai/sonar/mcp/
│   ├── SonarMcpServerApplication.java   — Spring Boot entry point
│   ├── api/                              — stable MCP wire format: records returned by tools/services
│   │   ├── Issue.java, IssuePage.java, IssueDetails.java, IssueLocation.java, IssueFlow.java
│   │   ├── Project.java, ProjectPage.java
│   │   ├── RuleDetails.java, RuleSection.java
│   │   ├── Hotspot.java, HotspotDetails.java, HotspotPage.java, HotspotRule.java
│   │   ├── SourceSnippet.java, SnippetLine.java, IssueSnippets.java
│   │   ├── ChangelogEntry.java, ChangelogDiff.java
│   │   ├── TextRange.java, FacetCount.java
│   │   ├── ProjectIssuesSummary.java, ProjectIssuesBreakdown.java
│   │   └── ModuleIssuesSummary.java
│   ├── client/
│   │   ├── SonarClient.java              — SonarQube web-api wrapper
│   │   └── model/                        — raw DTOs of the SonarQube web-api, not exposed directly via MCP
│   │       └── Sonar*.java
│   ├── config/
│   │   ├── SonarClientProperties.java   — url + token from env
│   │   ├── SonarMcpProperties.java      — all sonar-mcp.* runtime settings
│   │   ├── SonarConfig.java             — RestClient with Basic auth
│   │   ├── McpServerConfig.java         — stdio MCP customizer with immediateExecution(true)
│   │   └── JsonConfig.java              — ObjectMapper for MCP JSON
│   ├── service/
│   │   ├── ProjectService.java
│   │   ├── IssueService.java
│   │   ├── RuleService.java             — with in-memory rule cache
│   │   ├── SnippetService.java
│   │   ├── HotspotService.java
│   │   ├── PaginationHelper.java        — offset/limit -> p/ps
│   │   └── SonarMappers.java            — client.model -> api mapping
│   └── tools/
│       ├── ProjectTools.java            — 5 MCP tools
│       ├── IssueTools.java              — 5 MCP tools
│       ├── RuleTools.java               — 1 MCP tool
│       ├── HotspotTools.java            — 2 MCP tools
│       ├── RefResolver.java             — branch/pullRequest resolution with default-branch fallback
│       └── ToolLogger.java
└── src/main/resources/
    ├── application.yml                  — MCP server configuration (stdio)
    └── logback-spring.xml               — logging configuration
```

## Troubleshooting

- **"Gradle requires JVM 17 or later"** — set `JAVA_HOME` to a JDK 25+.
- **Connection refused / 401** — check URL and token. Test: `curl -u "$SONAR_TOKEN:" "$SONAR_URL/api/components/search?qualifiers=TRK&p=1&ps=1"`.
- **403 Forbidden** — the token user has no rights on the project or on the web-api. Check the role in SonarQube.
- **Package/module scope returns 0 issues** — do not pass Java package names directly as `componentKeys`. Call `listComponents`, match the returned `path` values to the module/directory/package you need, then pass the returned opaque `key` unchanged as `listIssues.componentKeys`.
