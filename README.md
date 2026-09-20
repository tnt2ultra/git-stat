[TOC]

# Git stat

Сервис для анализа репозиториев корпоративного GitLab и построения статистики по количеству строк Java-кода и Java-тестов.

Сервис принимает CSV-реестр сервисов, клонирует или обновляет указанные Git-репозитории, анализирует историю коммитов и текущее состояние выбранной ветки, а затем показывает статистику по группам, сервисам и сотрудникам.

## Возможности сервиса

### Интерфейс

- веб-страница со статистикой;
- разделы «Импорт реестра» и «Синхронизация» расположены в две колонки;
- на узких экранах колонки автоматически складываются в одну;
- импорт CSV-файла через форму;
- импорт предустановленного файла `catena.csv` одной кнопкой;
- выбор ветки для анализа:
  - по умолчанию;
  - `develop`;
  - `develop-spimex`;
  - `master`;
  - `main`;
  - ввести вручную;
- запуск синхронизации всех сервисов;
- запуск синхронизации отдельного сервиса;
- просмотр задач синхронизации и ошибок;
- просмотр сводки, групп, сервисов, сотрудников и истории задач.

### Импорт реестра

- импорт CSV-реестра через REST API и веб-интерфейс;
- импорт предустановленного файла `catena.csv` из `src/main/resources`;
- проверка наличия `catena.csv` перед активацией кнопки импорта;
- строгий режим импорта: если найдены ошибки, не сохраняется ни одна строка;
- валидация заголовков CSV;
- валидация названий групп и сервисов;
- валидация URL репозитория;
- запрет URL с учетными данными;
- проверка разрешённого хоста GitLab;
- проверка дубликатов пары «группа + сервис»;
- проверка дубликатов URL;
- upsert-режим: создание новых и обновление существующих групп и сервисов;
- удаление локального кэша репозитория при изменении URL сервиса.

### Синхронизация и анализ Git

- клонирование bare-репозиториев через нативный `git`;
- обновление существующих bare-репозиториев;
- использование GitLab access token с правом `read_repository`;
- маскирование токена в логах и сообщениях об ошибках;
- анализ только файлов с расширением `.java`;
- анализ выбранной ветки;
- если ветка не указана, используется default branch репозитория;
- проверка существования выбранной ветки перед анализом;
- валидация вручную введённого имени ветки;
- получение истории коммитов через `git log --numstat`;
- исключение merge-коммитов из авторской статистики;
- получение текущего дерева файлов через `git ls-tree -r`;
- подсчёт строк через `git cat-file --batch` по oid объектов;
- фильтрация `.java`-файлов на стороне Java, без зависимости от семантики git pathspec;
- сохранение commit-based статистики по сотрудникам;
- сохранение текущего снимка LOC по сервису;
- перезапись снимка при каждой успешной синхронизации;
- история снимков LOC не хранится.

### Классификация Java-файлов

Сервис разделяет `.java`-файлы на категории:

- `CODE` — продуктовый код;
- `TEST` — тесты;
- `EXCLUDED` — исключённые файлы.

К `TEST` относятся файлы, которые находятся в тестовых директориях или имеют тестовые суффиксы, например:

```text
src/test/java/...
src/it/java/...
src/e2e/java/...
*Test.java
*Tests.java
*IT.java
*TestCase.java
*Spec.java
*Specification.java
```

К `EXCLUDED` относятся generated code, protobuf, openapi-generated, swagger-generated, миграции Flyway/Liquibase и подобные артефакты, например пути, содержащие:

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

Исключённые файлы не входят в метрики `CODE` и `TEST`, но их объём может сохраняться в снимке как `generated_excluded_lines`.

### Статистика

#### Текущий LOC

Для каждого сервиса сохраняется текущий снимок по проанализированной ветке:

- количество строк продуктового кода;
- количество строк тестов;
- количество строк в исключённых файлах;
- количество файлов кода;
- количество тестовых файлов;
- количество исключённых файлов;
- имя ветки;
- SHA HEAD;
- дата синхронизации.

#### Commit-based статистика по сотрудникам

Для каждого автора по каждому сервису сохраняется:

- email автора;
- имя автора;
- количество коммитов;
- добавленные строки кода;
- удалённые строки кода;
- добавленные строки тестов;
- удалённые строки тестов;
- дата последнего коммита.

Агрегированная статистика по сотрудникам показывает:

- имя;
- email;
- количество коммитов;
- добавлено кода;
- удалено кода;
- добавлено тестов;
- удалено тестов;
- количество сервисов, где участвовал автор.

#### Агрегация по группам и сервисам

Интерфейс и API показывают:

- количество групп;
- количество синхронизированных сервисов;
- суммарный текущий код;
- суммарные текущие тесты;
- общее количество строк;
- отношение тестов к коду;
- статистику по каждой группе;
- статистику по каждому сервису;
- статус последней синхронизации сервиса;
- фактическую ветку, по которой был сделан последний снимок.

## Технологический стек

- Java 25
- Spring Boot 4
- Spring Web
- Spring Data JPA
- Lombok
- H2
- Apache Commons CSV
- нативный `git`

## Ограничения сервиса

- анализируются только `.java`-файлы;
- за одну синхронизацию анализируется одна ветка;
- используется только одна ветка на момент синхронизации, мультиветковой сравнительной аналитики нет;
- история снимков LOC не сохраняется, при каждой синхронизации снимок перезаписывается;
- статистика по сотрудникам основана на коммитах, `git blame` не используется;
- merge-коммиты исключаются из авторской статистики;
- H2 используется как единственная БД и подходит для использования с ограниченным количеством сервисов;
- импорт CSV является строгим: при наличии ошибок не сохраняется ни одна строка;
- импорт CSV выполняет upsert и не удаляет сервисы, которых нет в новом файле;
- для работы требуется установленный нативный `git`;
- точность классификации тестов и generated code зависит от настроенных правил;
- если выбранная ветка отсутствует в конкретном репозитории, синхронизация этого сервиса завершается ошибкой.

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

### 2. Положите предустановленный реестр, если он нужен

Файл `catena.csv` должен находиться здесь:

```text
src/main/resources/catena.csv
```

Кодировка файла — UTF-8.

Формат тот же, что и для обычного CSV-импорта.

Если файл отсутствует, кнопка «Импортировать catena.csv» в интерфейсе будет заблокирована, а обычный импорт CSV через форму продолжит работать.

### 3. Соберите проект

```bash
mvn clean package
```

### 4. Запустите приложение

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

## Важно после обновления модели данных

Если приложение уже запускалось ранее и база H2 была создана со старой структурой таблиц, после изменения JPA-сущностей может потребоваться пересоздание схемы.

Особенно это касается таблицы `service_loc_snapshots`.

Самый простой вариант:

1. остановить приложение;
2. удалить каталог:

```text
./data
```

3. запустить приложение заново.

Hibernate создаст схему с нуля.

Альтернативный вариант без потери реестра:

1. открыть H2 console;
2. выполнить:

```sql
DROP TABLE IF EXISTS service_loc_snapshots;
```

3. перезапустить приложение.

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
- пара «группа + сервис» должна быть уникальной;
- дубликаты URL запрещены.

## Предварительно загруженный реестр catena.csv

Для быстрого старта в проект можно положить файл:

```text
src/main/resources/catena.csv
```

Тогда в интерфейсе станет доступна кнопка:

```text
Импортировать catena.csv
```

Она вызывает backend-эндпоинт:

```http
POST /api/v1/registry/import-default
```

Backend читает файл из classpath и выполняет тот же строгий импорт, что и для пользовательского CSV.

Перед активацией кнопка запрашивает статус файла:

```http
GET /api/v1/registry/default-status
```

Пример ответа:

```json
{
  "filename": "catena.csv",
  "available": true
}
```

Если файл отсутствует:

```json
{
  "filename": "catena.csv",
  "available": false
}
```

В этом случае кнопка блокируется, а пользователь может импортировать CSV через обычную форму.

## Выбор ветки для анализа

В разделе «Синхронизация» доступен выбор ветки:

```text
по умолчанию
develop
develop-spimex
master
main
Ввести вручную
```

### Поведение вариантов

- `по умолчанию` — сервис определяет default branch каждого репозитория и анализирует её;
- `develop`, `develop-spimex`, `master`, `main` — сервис пытается найти указанную ветку в каждом репозитории;
- `Ввести вручную` — появляется текстовое поле для произвольного имени ветки.

### Валидация вручную введённой ветки

Для ручного ввода разрешены безопасные имена веток, например:

```text
develop
main
feature/my-branch
bugfix/JIRA-123
release/1.2.3
```

Не пройдут валидацию имена:

- с пробелами;
- с недопустимыми спецсимволами;
- начинающиеся с дефиса;
- начинающиеся или заканчивающиеся слэшем;
- содержащие `..`;
- содержащие `//`;
- заканчивающиеся на `.lock`;
- слишком длинные.

### Если ветка отсутствует в репозитории

Если выбранная ветка не найдена в конкретном репозитории, задача синхронизации этого сервиса завершится со статусом `FAILED` и сообщением вида:

```text
Ветка не найдена в репозитории: develop
```

При синхронизации всех сервисов ошибка одного репозитория не останавливает обработку остальных.

### Что сохраняется в статистике

В таблице «Сервисы» колонка «Ветка» показывает фактическую ветку, по которой был сделан последний успешный снимок LOC.

Если синхронизация завершилась ошибкой, предыдущий снимок может сохраниться, а новая ветка не будет записана как успешный анализ.

## Использование через веб-интерфейс

### 1. Откройте главную страницу

```text
http://localhost:8080
```

### 2. Импортируйте реестр

В левой колонке находится блок «Импорт реестра».

Доступны два способа:

1. выбрать CSV-файл на компьютере и нажать «Импортировать выбранный CSV»;
2. нажать «Импортировать catena.csv», если файл доступен в classpath.

При успешном импорте будут созданы или обновлены группы и сервисы.

При ошибке интерфейс покажет список проблемных строк и полей.

### 3. Выберите ветку

В правой колонке находится блок «Синхронизация».

В выпадающем списке выберите ветку для анализа.

Если выбрано «Ввести вручную», укажите имя ветки в появившемся поле.

### 4. Запустите синхронизацию

Нажмите:

```text
Синхронизировать все сервисы
```

Или синхронизируйте отдельный сервис кнопкой в таблице «Сервисы».

Во время синхронизации сервис:

1. клонирует или обновляет bare-репозиторий;
2. определяет или проверяет выбранную ветку;
3. получает SHA HEAD ветки;
4. читает историю коммитов по `.java`-файлам;
5. считает добавленные и удалённые строки;
6. классифицирует файлы как код, тесты или исключённые;
7. получает список `.java`-файлов в ветке;
8. считает текущее количество строк через `git cat-file --batch`;
9. сохраняет статистику в H2.

### 5. Посмотрите статистику

После синхронизации доступны блоки:

- `Сводка`;
- `Группы`;
- `Сервисы`;
- `Сотрудники`;
- `Задачи синхронизации`.

## REST API

### Импорт CSV, загруженного пользователем

```http
POST /api/v1/registry/import
Content-Type: multipart/form-data
```

Пример через curl:

```bash
curl -X POST http://localhost:8080/api/v1/registry/import \
  -F "file=@registry.csv"
```

Успешный ответ:

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

### Импорт предустановленного catena.csv

```http
POST /api/v1/registry/import-default
```

Пример:

```bash
curl -X POST http://localhost:8080/api/v1/registry/import-default
```

Ответ имеет тот же формат, что и обычный импорт CSV.

### Статус предустановленного файла

```http
GET /api/v1/registry/default-status
```

Пример:

```bash
curl http://localhost:8080/api/v1/registry/default-status
```

Ответ:

```json
{
  "filename": "catena.csv",
  "available": true
}
```

### Синхронизация всех сервисов

```http
POST /api/v1/sync/all
```

Необязательный параметр:

```text
branch
```

Примеры:

```bash
curl -X POST http://localhost:8080/api/v1/sync/all
```

```bash
curl -X POST "http://localhost:8080/api/v1/sync/all?branch=develop"
```

```bash
curl -X POST "http://localhost:8080/api/v1/sync/all?branch=feature/my-branch"
```

Если параметр `branch` не передан или передан пустым, используется default branch каждого репозитория.

### Синхронизация одного сервиса

```http
POST /api/v1/sync/services/{serviceId}
```

Необязательный параметр:

```text
branch
```

Примеры:

```bash
curl -X POST http://localhost:8080/api/v1/sync/services/1
```

```bash
curl -X POST "http://localhost:8080/api/v1/sync/services/1?branch=main"
```

### Список задач синхронизации

```http
GET /api/v1/sync/jobs
```

Пример:

```bash
curl http://localhost:8080/api/v1/sync/jobs
```

Пример элемента ответа:

```json
{
  "id": 6,
  "serviceId": 2,
  "serviceName": "pg-compare",
  "status": "SUCCESS",
  "startedAt": "2026-09-20T15:04:24.137491Z",
  "finishedAt": "2026-09-20T15:04:26.178697Z",
  "commitsProcessed": 5,
  "filesProcessed": 100,
  "errorMessage": null
}
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
  "groupCount": 1,
  "serviceCount": 2,
  "codeLines": 12000,
  "testLines": 3500,
  "totalLines": 15500,
  "testToCodeRatio": 0.2916666666666667
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

  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 5MB

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

## Особенности реализации Git-анализа

### Чтение истории коммитов

Для commit-based статистики используется эквивалент команды:

```bash
git -c core.quotePath=false log \
  --no-merges \
  --no-renames \
  --reverse \
  --date=iso-strict \
  --pretty=format:"%H%x09%ad%x09%ae%x09%an%x09%s" \
  --numstat \
  <branch>
```

Особенности:

- `--no-merges` исключает merge-коммиты из авторской статистики;
- `--no-renames` упрощает парсинг путей;
- `--reverse` обрабатывает историю от старых коммитов к новым;
- `core.quotePath=false` предотвращает экранирование не-ASCII путей;
- pathspec `-- "*.java"` намеренно не используется;
- фильтрация `.java` выполняется на стороне Java, чтобы история и снимок использовали один и тот же критерий отбора файлов.

### Чтение текущего дерева файлов

Для snapshot LOC используется эквивалент команды:

```bash
git -c core.quotePath=false ls-tree -r <branch>
```

Особенности:

- pathspec не используется;
- вывод разбирается в формате `<mode> <type> <oid> TAB <path>`;
- оставляются только объекты типа `blob`;
- фильтрация по суффиксу `.java` выполняется на стороне Java.

### Подсчёт строк

Подсчёт строк выполняется через:

```bash
git cat-file --batch
```

Особенности:

- запросы выполняются по oid объектов из `ls-tree`;
- не используется резолвинг вида `<branch>:<path>`, который мог молча давать `missing` и нули;
- для каждого blob считается количество переводов строки;
- пустой файл имеет 0 строк;
- файл без завершающего перевода строки всё равно считает последнюю строку.

### Почему это важно

Раньше snapshot мог возвращать нули даже при успешной синхронизации, потому что `ls-tree` с pathspec и `cat-file` с `HEAD:path` вели себя нестабильно на некоторых репозиториях и платформах.

Текущая реализация исключает эти точки отказа.

## Модель данных H2

### service_groups

Хранит группы сервисов.

Основные поля:

- `id`;
- `name`.

### git_services

Хранит сервисы и ссылки на репозитории.

Основные поля:

- `id`;
- `group_id`;
- `name`;
- `repo_url`;
- `default_branch`;
- `last_head_sha`;
- `last_sync_at`;
- `sync_status`.

Примечание: Java-поле связано с группой через имя `serviceGroup`, потому что слово `group` является зарезервированным в JPQL. Имя колонки в БД остаётся `group_id`.

### sync_jobs

Хранит задачи синхронизации.

Основные поля:

- `id`;
- `service_id`;
- `status`;
- `started_at`;
- `finished_at`;
- `commits_processed`;
- `files_processed`;
- `error_message`.

### author_service_stats

Хранит commit-based статистику автора по сервису.

Основные поля:

- `id`;
- `service_id`;
- `author_email`;
- `author_name`;
- `commits`;
- `added_code_lines`;
- `removed_code_lines`;
- `added_test_lines`;
- `removed_test_lines`;
- `last_commit_at`.

Уникальность:

```text
service_id + author_email
```

### service_loc_snapshots

Хранит текущий снимок LOC сервиса.

Основные поля:

- `id`;
- `service_id`;
- `code_lines`;
- `test_lines`;
- `generated_excluded_lines`;
- `code_files`;
- `test_files`;
- `generated_files`;
- `branch`;
- `head_sha`;
- `synced_at`.

Важно: у снимка используется суррогатный первичный ключ `id`, а `service_id` является уникальным внешним ключом.

Такая схема выбрана потому, что варианты с `@MapsId` или с одной колонкой как `@Id` и как FK вызывали ошибки Hibernate/Spring Data в текущем стеке.

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

1. Подготовьте CSV-файл со списком сервисов или положите `catena.csv` в `src/main/resources`.
2. Запустите приложение с переменными `GITLAB_TOKEN` и `GITLAB_ALLOWED_HOST`.
3. Откройте `http://localhost:8080`.
4. Импортируйте реестр через форму или кнопкой `catena.csv`.
5. Выберите ветку анализа.
6. Нажмите «Синхронизировать все сервисы».
7. Дождитесь завершения задач синхронизации.
8. Просмотрите сводку, группы, сервисы и сотрудников.
9. При необходимости синхронизируйте отдельный сервис повторно.

## Пример полного цикла через curl

### Импорт CSV

```bash
curl -X POST http://localhost:8080/api/v1/registry/import \
  -F "file=@registry.csv"
```

### Импорт catena.csv

```bash
curl -X POST http://localhost:8080/api/v1/registry/import-default
```

### Проверка задач синхронизации до запуска

```bash
curl http://localhost:8080/api/v1/sync/jobs
```

### Запуск синхронизации всех сервисов по default branch

```bash
curl -X POST http://localhost:8080/api/v1/sync/all
```

### Запуск синхронизации всех сервисов по ветке develop

```bash
curl -X POST "http://localhost:8080/api/v1/sync/all?branch=develop"
```

### Запуск синхронизации одного сервиса по ветке main

```bash
curl -X POST "http://localhost:8080/api/v1/sync/services/1?branch=main"
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

Проверьте файл по примеру из раздела «Пример CSV-реестра».

### Кнопка «Импортировать catena.csv» заблокирована

Причины:

- файл `catena.csv` отсутствует в `src/main/resources`;
- файл не попал в classpath после сборки;
- приложение запущено из старого JAR.

Решение:

1. положите файл сюда:

```text
src/main/resources/catena.csv
```

2. пересоберите проект:

```bash
mvn clean package
```

3. перезапустите приложение.

Проверить доступность файла можно через:

```bash
curl http://localhost:8080/api/v1/registry/default-status
```

### Ошибка ветки не найдена

Сообщение вида:

```text
Ветка не найдена в репозитории: develop
```

означает, что выбранная ветка отсутствует в конкретном Git-репозитории.

Решение:

- выберите другую ветку;
- используйте вариант «по умолчанию»;
- убедитесь, что ветка действительно существует в репозитории;
- проверьте имя ветки при ручном вводе.

### Долгая синхронизация

Для MVP ожидаемый размер репозиториев небольшой, но при большом количестве коммитов синхронизация может занимать заметное время.

Можно увеличить таймаут:

```yaml
app:
  git:
    timeout-seconds: 1200
```

### Ошибка блокировки H2

Если приложение уже запущено, второй экземпляр может не получить доступ к файловой базе H2.

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

### Ошибка null identifier ServiceLocSnapshot

Сообщение вида:

```text
org.hibernate.AssertionFailure: null identifier (com.anri.gitloc.domain.ServiceLocSnapshot)
```

обычно означает, что схема H2 осталась от старой версии модели данных.

Решение:

1. остановить приложение;
2. удалить каталог:

```text
./data
```

3. пересобрать проект:

```bash
mvn clean package
```

4. запустить приложение заново.

Если текущая модель использует суррогатный ключ `id` и уникальный FK `service_id`, пересоздание таблицы обязательно после предыдущих экспериментов с `@MapsId` или `@Id` на колонке `service_id`.

### Синхронизация SUCCESS, но количество строк равно нулю

Если задачи синхронизации завершаются со статусом `SUCCESS`, commit-based статистика по сотрудникам есть, а текущие `Код, строк` и `Тесты, строк` равны нулю, причина обычно в snapshot-части анализа.

Текущая реализация устраняет основные причины:

- `ls-tree` вызывается без pathspec;
- фильтрация `.java` выполняется в Java;
- `cat-file --batch` запрашивает объекты по oid, а не по `branch:path`.

Если проблема осталась, включите DEBUG-логирование:

```yaml
logging:
  level:
    com.anri.gitloc.service.GitAnalysisService: DEBUG
```

и посмотрите строку вида:

```text
Snapshot: ветка = ..., найдено Java-файлов = ..., посчитано строк для = ...
```

Диагностика:

- если `найдено Java-файлов = 0`, значит `ls-tree -r <branch>` не возвращает blob-файлы `.java`;
- если файлов найдено много, а посчитанных строк ноль или мало, проверьте доступность `git cat-file --batch` и версию нативного `git`.

Также можно вручную проверить репозиторий:

```bash
git -C ./git-repos/service-1/repo.git ls-tree -r HEAD
```

и отфильтровать вывод по `.java`.

### Ошибка No property groupId found for type GitService

Сообщение вида:

```text
No property 'groupId' found for type 'GitService'
```

означает, что Spring Data пытается разобрать имя метода репозитория и не находит свойство `groupId`.

Причина — переименование Java-поля из `group` в `serviceGroup`.

Решение: использовать явный JPQL-запрос:

```java
@Query("select s from GitService s where s.serviceGroup.id = :groupId and s.name = :name")
Optional<GitService> findByGroupIdAndName(
        @Param("groupId") Long groupId,
        @Param("name") String name
);
```

### Ошибка identifier expected, got group

Сообщение вида:

```text
<identifier> expected, got 'group'
```

в JPQL означает, что используется зарезервированное слово `group` как имя атрибута.

Решение: поле в сущности должно называться:

```java
private ServiceGroup serviceGroup;
```

а в JPQL использоваться:

```java
join s.serviceGroup g
```

и:

```java
s.serviceGroup.id
```

### Ошибка This class does not define an IdClass

Сообщение вида:

```text
This class [class com.anri.gitloc.domain.ServiceLocSnapshot] does not define an IdClass
```

может возникать при использовании `@MapsId` на ассоциации в текущем стеке Spring Boot 4 / Spring Data JPA 4 / Hibernate 7.

Решение: использовать суррогатный первичный ключ:

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

и отдельный уникальный FK:

```java
@OneToOne
@JoinColumn(name = "service_id", nullable = false, unique = true)
private GitService service;
```

### Ошибка Cannot resolve method getLineNumber in CSVRecord

Сообщение вида:

```text
Cannot resolve method 'getLineNumber' in 'CSVRecord'
```

означает, что в Apache Commons CSV использован несуществующий метод.

Правильные варианты:

```java
record.getRecordNumber()
```

или более точный номер строки через:

```java
record.getCharacterPosition()
```

В текущей реализации номер строки вычисляется по символьной позиции записи, чтобы корректно работать даже при переносах строк внутри кавычек.

### Ошибка Variable used in lambda expression should be final or effectively final

Сообщение вида:

```text
Variable used in lambda expression should be final or effectively final
```

в `GitAnalysisService` возникало из-за использования изменяемой переменной `currentName` внутри `computeIfAbsent`.

Решение: не использовать лямбду для создания аккумулятора автора, а применять явную проверку:

```java
AuthorAccumulator acc = accumulators.get(currentEmail);

if (acc == null) {
    acc = new AuthorAccumulator(currentName);
    accumulators.put(currentEmail, acc);
}
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
│   │       ├── catena.csv
│   │       └── static/
│   │           ├── app.js
│   │           ├── index.html
│   │           └── styles.css
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

## Известные особенности текущей реализации

1. Импорт `catena.csv` использует тот же строгий валидатор, что и пользовательский CSV.

2. Импорт реестра не удаляет старые сервисы. Если сервис исчез из CSV, он останется в базе до отдельной операции удаления.

3. Выбор ветки применяется ко всем сервисам в рамках одной синхронизации. Если в одном репозитории ветка есть, а в другом нет, первый проанализируется успешно, второй завершится ошибкой.

4. Статистика по сотрудникам не учитывает merge-коммиты, чтобы не дублировать изменения.

5. Текущий LOC считается по файлам выбранной ветки на момент синхронизации.

6. Если после успешной синхронизации ветка изменилась, но новая синхронизация не запускалась, интерфейс покажет старый снимок.

7. H2 подходит для MVP, но не рекомендуется для production-нагрузки.

8. Для корректной работы нативного `git` в Docker-образе должен быть установлен пакет `git`.

## Дальнейшее развитие

В MVP сознательно не реализованы:

- поддержка нескольких веток одновременно;
- сравнение веток;
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
- поддержка других языков, кроме Java;
- удаление сервисов, отсутствующих в новом CSV;
- гибкая настройка правил классификации через UI.
