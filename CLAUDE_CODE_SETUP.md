# Подключение Sonar MCP Server к Claude Code

Эта инструкция описывает только установку и подключение **Sonar MCP Server**. Предполагается, что Claude Code уже установлен, авторизация выполнена, а сетевой доступ к корпоративному SonarQube настроен.

## Что понадобится

- Установленный и настроенный Claude Code.
- Доступ к корпоративному SonarQube из той среды, где запускается Claude Code.
- JDK 25+ для сборки и запуска сервера.
- URL SonarQube, например `http://sonar.example.com`.
- User token SonarQube.

## 1. Собрать MCP-сервер

В корне репозитория:

```bash
./gradlew build
```

На Windows можно использовать:

```powershell
.\gradlew.bat build
```

Если JDK 25+ не является JDK по умолчанию, перед сборкой задайте `JAVA_HOME`.

Результат сборки:

```text
build/libs/sonar-mcp-server.jar
```

## 2. Получить параметры SonarQube

URL — это адрес SonarQube без пути к конкретной странице.

| В браузере | Значение URL |
|---|---|
| `https://sonar.example.com/dashboard?id=...` | `https://sonar.example.com` |
| `http://192.168.1.50:9000/projects` | `http://192.168.1.50:9000` |
| `http://10.0.0.5/sonar/projects` | `http://10.0.0.5/sonar` |

User token генерируется в SonarQube: **My Account** -> **Security** -> **Generate Tokens** -> ввести имя, выбрать тип **User Token**, нажать **Generate**.

Токен показывается один раз — скопируйте его сразу. Если потерян, придётся сгенерировать заново.

## 3. Проверить запуск сервера

Перед подключением к Claude Code полезно проверить, что JAR запускается с теми же переменными окружения.

Linux/macOS:

```bash
SONARQUBE_URL=http://sonar.example.com SONARQUBE_TOKEN=your_token \
  java -jar build/libs/sonar-mcp-server.jar
```

Windows PowerShell:

```powershell
$env:SONARQUBE_URL="http://sonar.example.com"
$env:SONARQUBE_TOKEN="your_token"
java -jar .\build\libs\sonar-mcp-server.jar
```

Сервер работает через `stdio`, поэтому после успешного старта он не открывает HTTP-порт и ждёт MCP-запросы через stdin/stdout.
Логи пишутся в `${SONAR_MCP_DATA_DIR:-~/.sonar-mcp-server}/logs/sonar-mcp-server.log`.

## 4. Подключить сервер к Claude Code

Рекомендуемый вариант — зарегистрировать сервер через CLI Claude Code:

```bash
claude mcp add --scope user \
  --env SONARQUBE_URL=http://sonar.example.com \
  --env SONARQUBE_TOKEN=your_token \
  -- sonar java -jar "C:/absolute/path/to/sonar-mcp-server/build/libs/sonar-mcp-server.jar"
```

Если `java` недоступна из окружения Claude Code, укажите полный путь к `java.exe`:

```bash
claude mcp add --scope user \
  --env SONARQUBE_URL=http://sonar.example.com \
  --env SONARQUBE_TOKEN=your_token \
  -- sonar "C:/Program Files/Java/jdk-25/bin/java.exe" -jar "C:/absolute/path/to/sonar-mcp-server/build/libs/sonar-mcp-server.jar"
```

Флаг `--scope user` добавляет сервер в пользовательскую конфигурацию.

Проверить регистрацию:

```bash
claude mcp list
```

Удалить регистрацию:

```bash
claude mcp remove --scope user sonar
```

## 5. Проверить в Claude Code

1. Перезапустите Claude Code после изменения MCP-конфигурации.
2. Выполните команду:

```text
/mcp
```

В списке должен быть сервер `sonar` со статусом подключения.

Примеры запросов:

```text
Найди и исправь Sonar-замечания в проекте asv-ssj
```

```text
Покажи топ-10 правил по числу открытых замечаний в проекте asv-ssj
```

```text
Покажи замечания Sonar в каталоге apps/sbp/backend/bc/src/main/java/ru/foo
```

## Доступные инструменты

Сервер предоставляет **8 read-only MCP tools**. Он не изменяет данные в SonarQube.

| Группа | Инструменты |
|---|---|
| Проекты | `listProjects` |
| Замечания | `listIssues`, `getIssue`, `getIssueSnippets`, `getProjectIssuesSummary` |
| Правила | `getRule` |
| Security Hotspots | `listHotspots`, `getHotspot` |

Полный список и параметры см. в [README.md](README.md).

## Устранение проблем

**`java` не найден**

Проверьте `JAVA_HOME` и `Path`. Для MCP-конфигурации можно указать полный путь к `java.exe`.

**MCP-сервер не подключается**

Проверьте абсолютный путь к `sonar-mcp-server.jar`, наличие JDK 25+ и корректность `SONARQUBE_URL` / `SONARQUBE_TOKEN`.

**SonarQube возвращает 401 или connection refused**

Проверьте доступность SonarQube и токен:

```bash
curl -u "your_token:" http://sonar.example.com/api/components/search?qualifiers=TRK&p=1&ps=1
```

Обратите внимание: пароль для Basic auth — пустой, токен передаётся как username с двоеточием на конце.

**Сервер добавлен, но не появился в Claude Code**

Полностью перезапустите Claude Code и проверьте список серверов командой `claude mcp list`.
