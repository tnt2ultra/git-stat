```markdown
# Git stat

MVP-сервис для анализа репозиториев корпоративного GitLab и построения статистики по количеству строк Java-кода и Java-тестов.

Сервис принимает CSV-реестр сервисов, клонирует или обновляет указанные Git-репозитории, анализирует историю коммитов и текущее состояние default branch, а затем показывает статистику по группам, сервисам и сотрудникам.

## Возможности MVP

- импорт реестра сервисов из CSV-файла;
- хранение списка групп и сервисов в H2;
- клонирование и обновление bare-репозиториев через нативный `git`;
- анализ только файлов с расширением `.java`;
- разделение Java-файлов на категории:
  - `CODE` — продуктовый код;
  - `TEST` — тесты;
  - `EXCLUDED` — generated code, protobuf, openapi-generated, миграции Flyway/Liquibase и другие исключённые файлы;
- расчёт текущего количества строк кода и тестов по default branch;
- расчёт commit-based статистики по сотрудникам:
  - количество коммитов;
  - добавленные строки кода;
  - удаленные строки кода;
  - добавленные строки тестов;
  - удаленные строки тестов;
- агрегация статистики по:
  - общему портфелю сервисов;
  - группам;
  - отдельным сервисам;
  - сотрудникам;
- простой веб-интерфейс на статических страницах;
- REST API для импорта, синхронизации и получения статистики;
- H2 console для отладки и просмотра данных.

## Технологический стек

- Java 25
- Spring Boot 4
- Spring Web
- Spring Data JPA
- Lombok
- H2
- Apache Commons CSV
- нативный `git`

## Ограничения MVP

- анализируются только `.java`-файлы;
- используется только default branch;
- история снимков LOC не сохраняется, при каждой синхронизации снимок перезаписывается;
- статистика по сотрудникам основана на коммитах, `git blame` не используется;
- merge-коммиты исключаются из авторской статистики;
- H2 используется как единственная БД и подходит только для MVP;
- импорт CSV является строгим: если найдены ошибки, не сохраняется ни одна строка;
- для работы требуется установленный нативный `git`.

## Требования

Для сборки и запуска необходимы:

- JDK 25;
- Maven 3.9 или новее;
- установленный `git` в системе или в контейнере;
- доступ к корпоративному GitLab;
- GitLab access token с правом `read_repository`.

## Быстрый старт

### 1. Установите переменные окружения

Для Linux или macOS:

```bash
export GITLAB_TOKEN=ваш_токен_read_repository
export GITLAB_ALLOWED_HOST=gitlab.corp.example
```

Для Windows PowerShell:

```powershell
$env:GITLAB_TOKEN="ваш_токен_read_repository"
$env:GITLAB_ALLOWED_HOST="gitlab.corp.example"
```

Где:

- `GITLAB_TOKEN` — токен для доступа к репозиториям GitLab;
- `GITLAB_ALLOWED_HOST` — разрешённый хост корпоративного GitLab.

Если хост не задан, по умолчанию используется значение из `application.yml`.

### 2. Соберите проект

```bash
mvn clean package
```

### 3. Запустите приложение

Вариант через JAR:

```bash
java -jar target/git-stat-0.0.1-SNAPSHOT.jar
```

Вариант через Maven:

```bash
mvn spring-boot:run
```

После запуска приложение будет доступно по адресу:

```text
http://localhost:8080
```

## Пример CSV-реестра

Файл должен быть в кодировке UTF-8, с разделителем `;` и первой строкой-заголовком.

Пример `registry.csv`:

```csv
"название группы";"название сервиса";"url репозитория в корпоративном GitLab"
"payments";"payment-api";"https://gitlab.corp.example/payments/payment-api.git"
"payments";"payment-worker";"https://gitlab.corp.example/payments/payment-worker.git"
"frontend";"web-app";"https://gitlab.corp.example/frontend/web-app.git"
"mobile";"android-app";"https://gitlab.corp.example/mobile/android-app.git"
```

Требования к CSV:

- заголовки должны точно совпадать;
- разделитель — точка с запятой;
- значения должны быть заключены в двойные кавычки;
- URL должен использовать протокол HTTPS;
- хост URL должен совпадать с `GITLAB_ALLOWED_HOST`;
- URL не должен содержать логин, пароль или токен;
- пара `группа + сервис` должна быть уникальной;
- дубликаты URL запрещены.

## Использование через веб-интерфейс

### 1. Откройте главную страницу

```text
http://localhost:8080
```

### 2. Импортируйте CSV-реестр

В блоке `Импорт реестра`:

1. выберите файл `registry.csv`;
2. нажмите `Импортировать CSV`.

При успешном импорте будут созданы или обновлены группы и сервисы.

При ошибке интерфейс покажет список проблемных строк и полей.

### 3. Запустите синхронизацию

Нажмите кнопку:

```text
Синхронизировать все сервисы
```

Или синхронизируйте отдельный сервис кнопкой в таблице `Сервисы`.

Во время синхронизации сервис:

1. клонирует или обновляет bare-репозиторий;
2. определяет default branch;
3. читает историю коммитов по `.java`-файлам;
4. считает добавленные и удаленные строки;
5. классифицирует файлы как код, тесты или исключенные;
6. считает текущее количество строк в файлах default branch;
7. сохраняет статистику в H2.

### 4. Посмотрите статистику

После синхронизации доступны блоки:

- `Сводка`;
- `Группы`;
- `Сервисы`;
- `Сотрудники`;
- `Задачи синхронизации`.

## REST API

### Импорт CSV

```http
POST /api/v1/registry/import
Content-Type: multipart/form-data
```

Пример через curl:

```bash
curl -X POST http://localhost:8080/api/v1/registry/import \
  -F "file=@registry.csv"
```

Успешный ответ имеет вид:

```json
{
  "groupsCreated": 2,
  "servicesCreated": 4,
  "servicesUpdated": 0,
  "errors": []
}
```

При ошибках валидации возвращается HTTP 400 и список ошибок:

```json
{
  "groupsCreated": 0,
  "servicesCreated": 0,
  "servicesUpdated": 0,
  "errors": [
    {
      "line": 3,
      "field": "url репозитория в корпоративном GitLab",
      "message": "Хост не входит в список разрешённых: gitlab.corp.example"
    }
  ]
}
```

### Синхронизация всех сервисов

```http
POST /api/v1/sync/all
```

Пример:

```bash
curl -X POST http://localhost:8080/api/v1/sync/all
```

### Синхронизация одного сервиса

```http
POST /api/v1/sync/services/{serviceId}
```

Пример:

```bash
curl -X POST http://localhost:8080/api/v1/sync/services/1
```

### Сводная статистика

```http
GET /api/v1/stats/summary
```

Пример:

```bash
curl http://localhost:8080/api/v1/stats/summary
```

Пример ответа:

```json
{
  "groupCount": 3,
  "serviceCount": 10,
  "codeLines": 250000,
  "testLines": 80000,
  "totalLines": 330000,
  "testToCodeRatio": 0.32
}
```

### Статистика по группам

```http
GET /api/v1/stats/groups
```

Пример:

```bash
curl http://localhost:8080/api/v1/stats/groups
```

### Статистика по сервисам

```http
GET /api/v1/stats/services
```

С фильтром по группе:

```http
GET /api/v1/stats/services?groupId=1
```

Пример:

```bash
curl "http://localhost:8080/api/v1/stats/services?groupId=1"
```

### Статистика по одному сервису

```http
GET /api/v1/stats/services/{serviceId}
```

Пример:

```bash
curl http://localhost:8080/api/v1/stats/services/1
```

### Статистика по сотрудникам

```http
GET /api/v1/stats/authors
```

С фильтрами:

```http
GET /api/v1/stats/authors?groupId=1
GET /api/v1/stats/authors?serviceId=2
```

Пример:

```bash
curl "http://localhost:8080/api/v1/stats/authors?serviceId=2"
```

### Список задач синхронизации

```http
GET /api/v1/sync/jobs
```

Пример:

```bash
curl http://localhost:8080/api/v1/sync/jobs
```

## H2 console

Приложение использует файловую H2-базу.

Консоль доступна по адресу:

```text
http://localhost:8080/h2-console
```

Параметры подключения:

```text
JDBC URL: jdbc:h2:file:./data/gitloc;AUTO_SERVER=TRUE
Username: sa
Password: пусто
```

В H2 console можно просматривать таблицы:

- `service_groups`;
- `git_services`;
- `sync_jobs`;
- `author_service_stats`;
- `service_loc_snapshots`.

## Конфигурация

Основные настройки находятся в `src/main/resources/application.yml`.

Пример:

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:h2:file:./data/gitloc;AUTO_SERVER=TRUE
    driver-class-name: org.h2.Driver
    username: sa
    password:

  jpa:
    hibernate:
      ddl-auto: update
    open-in-view: false

  h2:
    console:
      enabled: true
      path: /h2-console

app:
  git:
    storage-path: ./git-repos
    timeout-seconds: 600
    token: ${GITLAB_TOKEN:}
    allowed-host: ${GITLAB_ALLOWED_HOST:gitlab.corp.example}
```

### Ключевые параметры

| Параметр | Назначение |
|---|---|
| `server.port` | HTTP-порт приложения |
| `spring.datasource.url` | URL H2-базы |
| `spring.jpa.hibernate.ddl-auto` | Режим автообновления схемы JPA |
| `app.git.storage-path` | Каталог для локальных bare-репозиториев |
| `app.git.timeout-seconds` | Таймаут выполнения Git-команд |
| `app.git.token` | Токен GitLab |
| `app.git.allowed-host` | Разрешённый хост GitLab |

## Классификация Java-файлов

### TEST

Файл считается тестовым, если путь содержит одни из маркеров:

```text
/test/
/tests/
/src/test/
/it/
/e2e/
/integration-test/
/integration-tests/
/functional-test/
/functional-tests/
```

Либо имя файла заканчивается на:

```text
Test.java
Tests.java
IT.java
TestCase.java
Spec.java
Specification.java
```

### EXCLUDED

Файл исключается из статистики, если путь содержит маркеры generated code или миграций, например:

```text
/generated/
/gen/
generated-sources
generated-test-sources
/target/generated
/build/generated
/openapi/
/swagger/
/protobuf/
/proto/
/grpc/
/flyway/
/liquibase/
/db/migration/
/migration/
/migrations/
```

### CODE

Все остальные `.java`-файлы, не попавшие в `TEST` или `EXCLUDED`, считаются кодом.

## Типичный сценарий использования

1. Подготовьте CSV-файл со списком сервисов.
2. Запустите приложение с переменными `GITLAB_TOKEN` и `GITLAB_ALLOWED_HOST`.
3. Откройте `http://localhost:8080`.
4. Импортируйте CSV.
5. Нажмите `Синхронизировать все сервисы`.
6. Дождитесь завершения задач синхронизации.
7. Просмотрите сводку, группы, сервисы и сотрудников.
8. При необходимости синхронизируйте отдельный сервис повторно.

## Пример полного цикла через curl

### Импорт CSV

```bash
curl -X POST http://localhost:8080/api/v1/registry/import \
  -F "file=@registry.csv"
```

### Запуск синхронизации всех сервисов

```bash
curl -X POST http://localhost:8080/api/v1/sync/all
```

### Проверка задач синхронизации

```bash
curl http://localhost:8080/api/v1/sync/jobs
```

### Получение сводки

```bash
curl http://localhost:8080/api/v1/stats/summary
```

### Получение статистики по сервисам

```bash
curl http://localhost:8080/api/v1/stats/services
```

### Получение статистики по сотрудникам

```bash
curl http://localhost:8080/api/v1/stats/authors
```

## Устранение неполадок

### Не найден git

Сообщение вида:

```text
error=2, No such file or directory
```

означает, что нативный `git` не установлен или недоступен в `PATH`.

Решение:

```bash
git --version
```

Если команда недоступна, установите Git.

### Ошибка клонирования репозитория

Возможные причины:

- неверный токен;
- токен не имеет права `read_repository`;
- указан неразрешённый хост;
- репозиторий недоступен по сети;
- URL указан без `.git`;
- в URL есть учетные данные.

Решение:

1. проверьте `GITLAB_TOKEN`;
2. проверьте `GITLAB_ALLOWED_HOST`;
3. убедитесь, что URL выглядит так:

```text
https://gitlab.corp.example/group/service.git
```

4. попробуйте склонировать репозиторий вручную:

```bash
git clone --bare https://gitlab.corp.example/group/service.git
```

### Ошибка импорта CSV

Частые причины:

- неверный заголовок;
- разделитель не `;`;
- файл не в UTF-8;
- пустые поля;
- дубликаты группы и сервиса;
- дубликаты URL;
- URL содержит логин и пароль;
- хост не совпадает с разрешённым.

Проверьте файл по примеру из раздела `Пример CSV-реестра`.

### Долгая синхронизация

Для MVP ожидаемый размер репозиториев небольшой, но при большом количестве коммитов синхронизация может занимать заметное время.

Можно увеличить таймаут:

```yaml
app:
  git:
    timeout-seconds: 1200
```

### Ошибка блокировки H2

Если приложение уже запущено,second instance может не смоet получить доступ к файловой базе H2.

Решение:

- остановите предыдущий процесс;
- или используйте `AUTO_SERVER=TRUE`, который уже задан в конфигурации.

### Ошибка прав на каталог репозиториев

Приложение использует каталог:

```text
./git-repos
```

Убедитесь, что у пользователя, от имени которого запущено приложение, есть права на создание и запись в этот каталог.

При необходимости измените путь:

```yaml
app:
  git:
    storage-path: /var/lib/git-stat/repos
```

## Структура проекта

```text
git-stat/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── anri/
│   │   │           └── gitloc/
│   │   │               ├── GitLocMvpApplication.java
│   │   │               ├── config/
│   │   │               ├── controller/
│   │   │               ├── domain/
│   │   │               ├── dto/
│   │   │               ├── repository/
│   │   │               └── service/
│   │   └── resources/
│   │       ├── application.yml
│   │       └── static/
│   └── test/
│       ├── java/
│       └── resources/
├── data/
│   └── gitloc.mv.db
└── git-repos/
    └── service-{id}/
        └── repo.git/
```

## Что сохраняется локально

После запуска в рабочей директории появятся:

```text
./data/
```

в ней хранится H2-база, и

```text
./git-repos/
```

в ней хранятся локальные bare-репозитории.

Эти каталоги не нужно коммитить в Git.

Пример `.gitignore`:

```gitignore
target/
data/
git-repos/
*.mv.db
*.trace.db
.idea/
.vscode/
```

## Дальнейшее развитие

В MVP сознательно не реализованы:

- поддержка нескольких веток;
- анализ по тегам и релизам;
- история снимков LOC;
- `git blame` для определения текущего автора строк;
- анализ Pull Request;
- анализ задач и трекера;
- интеграция с PostgreSQL;
- авторизация через SSO или OIDC;
- роли пользователей;
- аудит административных действий;
- экспорт в XLSX;
- поддержка других языков, кроме Java.
```