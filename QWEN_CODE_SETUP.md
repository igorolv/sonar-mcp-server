# Подключение Sonar MCP Server к Qwen Code

Эта инструкция описывает только установку и подключение **Sonar MCP Server**. Предполагается, что Qwen Code уже установлен и настроен для работы с выбранной моделью.

## Что понадобится

- Установленный и настроенный Qwen Code.
- Доступ к корпоративному SonarQube из той среды, где запускается Qwen Code.
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

Токен показывается один раз — скопируйте его сразу.

## 3. Проверить запуск сервера

Перед подключением к Qwen Code полезно проверить, что JAR запускается с теми же переменными окружения.

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

## 4. Подключить сервер к Qwen Code

Вариант через конфигурационный файл: создайте или обновите `C:\Users\<user>\.qwen\settings.json`.

```json
{
  "mcpServers": {
    "sonar": {
      "command": "java",
      "args": [
        "-jar",
        "C:/absolute/path/to/sonar-mcp-server/build/libs/sonar-mcp-server.jar"
      ],
      "env": {
        "SONARQUBE_URL": "http://sonar.example.com",
        "SONARQUBE_TOKEN": "your_token"
      }
    }
  }
}
```

Если `SONARQUBE_URL` и `SONARQUBE_TOKEN` уже заданы как системные переменные окружения, секцию `env` можно не указывать.

На Windows используйте прямые слеши (`/`) или двойные обратные (`\\`) в путях. Если `java` недоступна из окружения Qwen Code, укажите полный путь к `java.exe`:

```json
"command": "C:/Program Files/Java/jdk-25/bin/java.exe"
```

Альтернативный вариант — зарегистрировать сервер через CLI Qwen Code:

```bash
qwen mcp add --scope user \
  -e SONARQUBE_URL=http://sonar.example.com \
  -e SONARQUBE_TOKEN=your_token \
  sonar java -jar "C:/absolute/path/to/sonar-mcp-server/build/libs/sonar-mcp-server.jar"
```

Проверить регистрацию:

```bash
qwen mcp list
```

Удалить регистрацию:

```bash
qwen mcp remove --scope user sonar
```

## 5. Проверить в Qwen Code

1. Перезапустите Qwen Code после изменения MCP-конфигурации.
2. Убедитесь, что при старте Qwen Code сервер `sonar` подключился без ошибок.

Примеры запросов:

```text
Найди и исправь Sonar-замечания в проекте asv-ssj
```

```text
Покажи топ-10 правил по числу открытых замечаний в проекте asv-ssj
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

**Сервер добавлен, но не появился в Qwen Code**

Полностью перезапустите Qwen Code и проверьте список серверов командой `qwen mcp list`. Если конфигурация задана вручную, проверьте, что она находится именно в пользовательском файле настроек Qwen Code.
