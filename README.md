# Sonar MCP Server

Локальный MCP-сервер для read-only доступа к корпоративному SonarQube 9 через web-api.
Позволяет AI-агентам (Claude Code, Cursor, VS Code Copilot и др.) получать список замечаний по проекту, файлы и места, где они возникают, описания правил, фрагменты исходного кода вокруг замечаний и Security Hotspots.

Типовой сценарий: «найди и поправь замечания Sonar в таком-то проекте» — LLM зовёт `listIssues`, при необходимости `getRule` и `getIssueSnippets`, и правит файлы локально.

## Быстрый старт

1. Установите JDK 25+.
2. Соберите сервер: `./gradlew build`.
3. Получите URL SonarQube и user token.
4. Добавьте собранный JAR в MCP-конфигурацию вашего клиента.

| Клиент | Инструкция |
|---|---|
| Claude Code | [CLAUDE_CODE_SETUP.md](CLAUDE_CODE_SETUP.md) |
| Qwen Code | [QWEN_CODE_SETUP.md](QWEN_CODE_SETUP.md) |

## Архитектура

Сервер поддерживает только транспорт `stdio`.

```
┌─────────────┐     stdio      ┌──────────────────┐    web-api     ┌──────────┐
│  AI-агент   │ <------------> │  sonar-mcp-      │ -------------> │ SonarQube│
│ (Claude Code│   stdin/stdout │  server (Java)   │  HTTP + Basic  │   v9     │
│  Cursor...) │                │                  │  auth (token)  │          │
└─────────────┘                └──────────────────┘                └──────────┘
```

AI-клиент запускает сервер как дочерний процесс, обмен по протоколу MCP через stdin/stdout.
Сервер не открывает HTTP-порт и не принимает входящих подключений.

## Инструменты

Сервер экспортирует **8 read-only MCP tools**.

### Проекты

| Tool | Описание |
|---|---|
| `listProjects` | Список проектов SonarQube. Параметры: `query` (подстрока имени), `limit`, `offset`. Возвращает `key`, `name`, `qualifier`. |

### Замечания

| Tool | Описание |
|---|---|
| `listIssues` | Плоский список замечаний для проекта. Параметры: `projectKey` (обязательный), `pathPrefix` (относительный путь от корня проекта, опц.), `severities`, `types`, `statuses`, `rules`, `branch`, `resolved`, `limit`, `offset`. По умолчанию возвращает только открытые (`resolved=false`, статусы OPEN/CONFIRMED/REOPENED). Каждый элемент содержит правило, severity, type, status, file path, line, primary textRange, и secondary flows для cross-file правил. |
| `getIssue` | Детали одного замечания по ключу + история изменений (`changelog`). |
| `getIssueSnippets` | Фрагменты исходного кода вокруг всех локаций замечания (первичная + flows для cross-file правил). По каждой локации — `componentPath`, язык и массив строк кода со SCM-информацией. Полезно, когда репозиторий недоступен локально или нужно увидеть точно ту версию файла, которую анализировал Sonar. |
| `getProjectIssuesSummary` | Агрегированная сводка открытых замечаний по проекту: total + срезы по severity, type, status, rule, tag и SCM-author. Параметры: `projectKey`, `pathPrefix` (опц.), `branch` (опц.). |

### Правила

| Tool | Описание |
|---|---|
| `getRule` | Детали правила Sonar по ключу (например `java:S1234`): заголовок, severity, type, язык, теги, секции описания (introduction, root cause, how to fix, resources). С in-memory кэшем — повторные вызовы бесплатные. |

### Security Hotspots

| Tool | Описание |
|---|---|
| `listHotspots` | Список Security Hotspots для проекта. Hotspots — отдельная категория от issues, помечает места, требующие ручной проверки безопасности. По умолчанию Sonar возвращает hotspots в статусе `TO_REVIEW`. Параметры: `projectKey`, `pathPrefix` (опц.), `status` (опц.), `branch` (опц.), `limit`, `offset`. |
| `getHotspot` | Детали Security Hotspot: полное описание правила (риск, vulnerability, fix recommendations), primary textRange, secondary flows, changelog. |

Все инструменты **read-only** — данные в SonarQube не изменяются.

## Стек

- Java 25, Spring Boot 4.0.0, Spring AI MCP 2.0.0-M6 (stdio transport)
- Jackson Databind для JSON
- Gradle 9.3.1 с version catalog (`gradle/libs.versions.toml`)

## Сборка

```bash
# Указать JDK 25+, если не является JDK по умолчанию:
export JAVA_HOME="$HOME/.jdks/jdk-25.0.2"

./gradlew build
```

Результат: `build/libs/sonar-mcp-server.jar`

## Настройка

Серверу нужны URL и токен SonarQube; остальные переменные опциональны.

| Переменная | Описание |
|---|---|
| `SONARQUBE_URL` (или `SONAR_URL`) | Базовый URL SonarQube (например `http://sonar.example.com`) |
| `SONARQUBE_TOKEN` (или `SONAR_TOKEN`) | User token SonarQube |
| `SONAR_MCP_DATA_DIR` | Каталог локальных данных сервера; по умолчанию `~/.sonar-mcp-server` |
| `SONAR_MCP_PAGINATION_DEFAULT_LIMIT` | Лимит страницы по умолчанию для list-инструментов; по умолчанию `50` |
| `SONAR_MCP_PAGINATION_DEFAULT_OFFSET` | Offset по умолчанию для list-инструментов; по умолчанию `0` |
| `SONAR_MCP_PAGINATION_MAX_LIMIT` | Максимальный лимит страницы (Sonar API сам режет на 500); по умолчанию `500` |
| `SONAR_MCP_SNIPPET_MAX_LINES` | Резерв для будущего ограничения числа строк в одном snippet; сейчас не используется напрямую, Sonar сам выбирает окно. По умолчанию `50` |

### Как получить SonarQube URL

Откройте SonarQube в браузере и скопируйте адрес из адресной строки **без** пути — только схему и домен.

| В адресной строке браузера | Значение URL |
|---|---|
| `https://sonar.example.com/dashboard?id=...` | `https://sonar.example.com` |
| `http://192.168.1.50:9000/projects` | `http://192.168.1.50:9000` |
| `http://10.0.0.5/sonar/projects` | `http://10.0.0.5/sonar` |

> Если SonarQube доступен только по IP-адресу, используйте IP как есть. Если развёрнут по подпути (например `/sonar`), его тоже нужно включить в URL.

### Как получить токен SonarQube

1. Войдите в SonarQube под своей учётной записью.
2. Откройте **My Account** -> **Security**.
3. В разделе **Generate Tokens** введите имя токена и выберите тип **User Token**.
4. Нажмите **Generate** — токен будет показан один раз. Скопируйте его сразу.
5. Используйте значение токена как `SONARQUBE_TOKEN`.

> Если токен потерян, его нужно сгенерировать заново — SonarQube не показывает существующие токены повторно.

Сервер использует HTTP Basic auth, где токен передаётся в качестве username с пустым паролем — это стандартная схема SonarQube 9 для user-токенов.

## Запуск

```bash
SONARQUBE_URL=http://sonar.example.com SONARQUBE_TOKEN=your_token \
  java -jar build/libs/sonar-mcp-server.jar
```

Сервер работает через `stdio`. После успешного старта он не открывает HTTP-порт, ждёт MCP-запросы через stdin/stdout.

Логи пишутся в `${SONAR_MCP_DATA_DIR:-~/.sonar-mcp-server}/logs/sonar-mcp-server.log`.
Файл ротируется по дате и размеру: `10MB`, хранение `30` дней, общий лимит `512MB`.

## Подключение к AI-клиенту

```json
{
  "command": "java",
  "args": ["-jar", "<абсолютный-путь>/sonar-mcp-server.jar"],
  "env": {
    "SONARQUBE_URL": "http://sonar.example.com",
    "SONARQUBE_TOKEN": "your_token"
  }
}
```

Куда именно:

| Клиент | Способ подключения |
|---|---|
| Claude Code | `claude mcp add --scope user -e SONARQUBE_URL=... -e SONARQUBE_TOKEN=... -- sonar java -jar /path/to/sonar-mcp-server.jar` |
| Qwen Code | `~/.qwen/settings.json` -> `"mcpServers"` -> `"sonar"` |
| VS Code | `.vscode/mcp.json` -> `"servers"` -> `"sonar"` |
| Cursor | `.cursor/mcp.json` -> `"mcpServers"` -> `"sonar"` |
| Claude Desktop | `claude_desktop_config.json` -> `"mcpServers"` -> `"sonar"` |

После добавления перезапустить клиент.

## Эксплуатация и безопасность

Этот MCP-сервер предназначен для локального запуска рядом с AI-клиентом. Он не открывает HTTP-порт и не принимает входящие сетевые подключения: клиент запускает JAR как дочерний процесс и общается с ним через `stdin/stdout`.

### Модель доступа

- Сервер использует права того пользователя SonarQube, чей токен указан в `SONARQUBE_TOKEN`.
- Все MCP-инструменты read-only: сервер не создаёт, не изменяет и не удаляет замечания, hotspots, правила или проекты.
- Доступные проекты и замечания определяются правами пользователя в SonarQube. Если пользователь не видит проект в SonarQube, сервер тоже не должен получить к нему доступ.
- Токен нужно хранить как секрет. Не коммитьте его в репозиторий, shell-скрипты, `.vscode/mcp.json`, `.cursor/mcp.json` или другие общие файлы проекта.

### Какие данные передаются AI-клиенту

AI-клиент получает ровно те данные, которые запрашивает через MCP-инструменты:

- список замечаний с правилом, severity, type, путём к файлу, номером строки, сообщением, тегами, SCM-автором;
- история изменений замечания;
- описания правил Sonar (включая HTML/markdown);
- фрагменты исходного кода вокруг локаций замечаний (через `getIssueSnippets`);
- Security Hotspots и их детали.

Перед подключением к внешнему или облачному AI-клиенту проверьте внутренние правила компании: исходный код может содержать коммерческую тайну.

## Диагностика

Проверка окружения:

```bash
java -version
echo "$SONARQUBE_URL"
test -n "$SONARQUBE_TOKEN" && echo "SONARQUBE_TOKEN is set"
```

Проверка доступа к SonarQube web-api (HTTP Basic с токеном-как-username и пустым паролем):

```bash
curl -u "$SONARQUBE_TOKEN:" "$SONARQUBE_URL/api/components/search?qualifiers=TRK&p=1&ps=1"
```

Ожидаемый ответ — JSON со списком проектов. `401 Unauthorized` означает, что токен невалидный или истёкший; `403` — у пользователя нет прав на API.

Проверка сборки:

```bash
./gradlew test
./gradlew build
```

Проверка интеграционных тестов с живым SonarQube:

```bash
SONARQUBE_URL=<url> SONARQUBE_TOKEN=<token> ./gradlew integrationTest
```

Интеграционные тесты требуют доступный SonarQube и реальные данные. Unit-тесты по умолчанию исключают тесты с тегом `integration`.

### Известные эксплуатационные ограничения

- HTTP-таймауты и retry-политика сейчас не настраиваются отдельно.
- Sonar API использует страничную пагинацию (`p`/`ps`); инструменты принимают `offset`/`limit`, и offset, не кратный limit, округляется вниз до ближайшей границы страницы. Sonar также ограничивает `ps` сверху значением 500.
- `pathPrefix` передаётся в Sonar как `componentKeys=<projectKey>:<pathPrefix>`. Это работает и для конкретного файла, и для каталога (рекурсивно вниз). Java-нотация (`ru.foo.bar`) сознательно не поддерживается — указывайте путь в виде, как он лежит в репозитории (например `src/main/java/ru/foo/bar`).
- Поле `author` у `Issue` — это SCM-автор строки, в которой возникло замечание (заполняется Sonar при настроенном SCM-провайдере). Отдельных полей `scmAuthor`/`scmDate` Sonar в `issues/search` не отдаёт; для строкового SCM используйте `getIssueSnippets`.

## Структура проекта

```
├── src/main/java/ru/it_spectrum/ai/sonar/mcp/
│   ├── SonarMcpServerApplication.java   — точка входа Spring Boot
│   ├── api/                              — стабильный MCP wire format: records, возвращаемые tools/services
│   │   ├── Issue.java, IssuePage.java, IssueDetails.java, IssueLocation.java, IssueFlow.java
│   │   ├── Project.java, ProjectPage.java
│   │   ├── RuleDetails.java, RuleSection.java
│   │   ├── Hotspot.java, HotspotDetails.java, HotspotPage.java, HotspotRule.java
│   │   ├── SourceSnippet.java, SnippetLine.java, IssueSnippets.java
│   │   ├── ChangelogEntry.java, ChangelogDiff.java
│   │   ├── TextRange.java, FacetCount.java
│   │   └── ProjectIssuesSummary.java
│   ├── client/
│   │   ├── SonarClient.java              — обёртка над SonarQube web-api
│   │   └── model/                        — raw DTO SonarQube web-api, не экспортируются напрямую в MCP
│   │       └── Sonar*.java
│   ├── config/
│   │   ├── SonarClientProperties.java   — url + token из env
│   │   ├── SonarMcpProperties.java      — все runtime-настройки sonar-mcp.*
│   │   ├── SonarConfig.java             — RestClient с Basic auth
│   │   ├── McpServerConfig.java         — stdio MCP customizer с immediateExecution(true)
│   │   └── JsonConfig.java              — ObjectMapper для MCP JSON
│   ├── service/
│   │   ├── ProjectService.java
│   │   ├── IssueService.java
│   │   ├── RuleService.java             — c in-memory кэшем правил
│   │   ├── SnippetService.java
│   │   ├── HotspotService.java
│   │   ├── PaginationHelper.java        — offset/limit -> p/ps
│   │   └── SonarMappers.java            — маппинг client.model -> api
│   └── tools/
│       ├── ProjectTools.java            — 1 MCP-инструмент
│       ├── IssueTools.java              — 4 MCP-инструмента
│       ├── RuleTools.java               — 1 MCP-инструмент
│       ├── HotspotTools.java            — 2 MCP-инструмента
│       └── ToolLogger.java
└── src/main/resources/
    ├── application.yml                  — конфигурация MCP-сервера (stdio)
    └── logback-spring.xml               — конфигурация логирования
```

## Troubleshooting

- **«Gradle requires JVM 17 or later»** — установить `JAVA_HOME` на JDK 25+.
- **Connection refused / 401** — проверить URL и токен. Тест: `curl -u "$SONARQUBE_TOKEN:" "$SONARQUBE_URL/api/components/search?qualifiers=TRK&p=1&ps=1"`.
- **403 Forbidden** — у пользователя токена нет прав на проект или на web-api. Проверить роль в SonarQube.
- **Empty pathPrefix не работает / возвращает 0** — путь должен совпадать с тем, как файлы лежат в репозитории. Если у вас multi-module Gradle/Maven, имя модуля идёт в начале (например `apps/ssj/backend/bc/...`).
