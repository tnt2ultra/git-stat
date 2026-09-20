# Техническое задание на Git stat

## 1. Основание и цель

Техническое задание подготовлено для фиксации реализованного объёма.

Цель сервиса:

- предоставить внутренний инструмент для анализа Java-репозиториев корпоративного GitLab;
- получать статистику по количеству строк кода и тестов;
- агрегировать статистику по группам, сервисам и сотрудникам;
- минимизировать инфраструктурную сложность за счёт H2 и нативного `git`.

---

## 2. Технологический стек

Обязательный стек:

- Java 25;
- Spring Boot 4;
- Spring Web;
- Spring Data JPA;
- Lombok;
- H2;
- Apache Commons CSV;
- нативный `git`.

Допустимое окружение:

- Windows, Linux или macOS;
- Docker при наличии установленного `git` в образе.

---

## 3. Требования к окружению

Для сборки:

- JDK 25;
- Maven 3.9+.

Для запуска:

- JRE/JDK 25;
- нативный `git` в `PATH`;
- доступ к корпоративному GitLab;
- GitLab access token с правом `read_repository`.

Переменные окружения:

```text
GITLAB_TOKEN
GITLAB_ALLOWED_HOST
```

Пример:

```bash
export GITLAB_TOKEN=glpat-xxxxxxxxxxxxxxxxxxxx
export GITLAB_ALLOWED_HOST=gitlab.corp.example
```

---

## 4. Функциональный объём

### 4.1. Реестр сервисов

Система должна хранить:

- группы сервисов;
- сервисы;
- URL репозиториев;
- статус последней синхронизации.

Источник реестра:

- CSV-файл, загружаемый пользователем;
- предустановленный файл `catena.csv` из `src/main/resources`.

CSV-формат:

```csv
"название группы";"название сервиса";"url репозитория в корпоративном GitLab"
```

Требования:

- разделитель `;`;
- кодировка UTF-8;
- заголовок обязателен;
- строгая валидация;
- атомарный импорт: при ошибке не сохраняется ни одна строка;
- upsert групп и сервисов;
- запрет дубликатов пары группа + сервис;
- запрет дубликатов URL;
- HTTPS only;
- allowlist host;
- запрет учетных данных в URL.

### 4.2. Синхронизация Git

Система должна:

- клонировать bare-репозиторий при первом запуске;
- обновлять существующий bare-репозиторий;
- использовать токен из конфигурации;
- маскировать токен в ошибках;
- поддерживать выбор ветки;
- проверять существование ветки;
- анализировать только `.java`-файлы.

Поддерживаемые ветки в UI:

- по умолчанию;
- develop;
- develop-spimex;
- master;
- main;
- ввести вручную.

### 4.3. Анализ истории коммитов

Источник commit-based статистики:

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

Требования:

- merge-коммиты исключаются;
- пути фильтруются по `.java` на стороне Java;
- файлы классифицируются как CODE, TEST, EXCLUDED;
- EXCLUDED не участвуют в авторской статистике;
- для каждого автора накапливаются:
  - commits;
  - added code lines;
  - removed code lines;
  - added test lines;
  - removed test lines.

### 4.4. Snapshot текущего LOC

Источник списка файлов:

```bash
git -c core.quotePath=false ls-tree -r <branch>
```

Формат строки:

```text
<mode> <type> <oid> TAB <path>
```

Требования:

- оставлять только `blob`;
- фильтровать `.java` на стороне Java;
- не использовать pathspec;
- получать содержимое через:

```bash
git cat-file --batch
```

- запрашивать объекты по oid;
- считать количество строк;
- классифицировать файлы;
- сохранять итоговый снимок сервиса.

### 4.5. Хранение результатов

Результаты сохраняются в H2.

Сущности:

- `ServiceGroup`;
- `GitService`;
- `SyncJob`;
- `AuthorServiceStat`;
- `ServiceLocSnapshot`.

Требования:

- `ServiceLocSnapshot` имеет суррогатный первичный ключ `id`;
- `service_id` в снимке является уникальным внешним ключом;
- статистика авторов по сервису перезаписывается при каждой успешной синхронизации;
- снимок LOC перезаписывается при каждой успешной синхронизации;
- история снимков не хранится.

### 4.6. Веб-интерфейс

UI должен предоставлять:

- импорт CSV;
- импорт `catena.csv`;
- выбор ветки;
- запуск синхронизации всех сервисов;
- запуск синхронизации отдельного сервиса;
- сводку;
- таблицы групп;
- таблицы сервисов;
- таблицу сотрудников;
- таблицу задач синхронизации.

Разделы «Импорт реестра» и «Синхронизация» должны располагаться в две колонки на широких экранах.

### 4.7. REST API

Необходимые endpoints:

```text
POST /api/v1/registry/import
POST /api/v1/registry/import-default
GET  /api/v1/registry/default-status

POST /api/v1/sync/services/{serviceId}
POST /api/v1/sync/all
GET  /api/v1/sync/jobs

GET /api/v1/stats/summary
GET /api/v1/stats/groups
GET /api/v1/stats/services
GET /api/v1/stats/services/{serviceId}
GET /api/v1/stats/authors
```

Параметр `branch` для sync endpoints необязателен.

---

## 5. Нефункциональные требования

### 5.1. Объём данных

- до 50 сервисов;
- репозитории типично 10 МБ, максимально 50 МБ.

### 5.2. Производительность

- UI и API должны отвечать в пределах нескольких секунд для агрегированных запросов;
- синхронизация может занимать длительное время;
- таймаут Git-команд конфигурируется.

### 5.3. Надёжность

- ошибка одного сервиса не останавливает syncAll;
- статусы задач сохраняются в БД;
- транзакционное сохранение результатов анализа.

### 5.4. Безопасность

- токен только из ENV;
- токен не логируется;
- allowlist GitLab host;
- HTTPS only;
- branch name validation;
- bare repositories;
- отсутствие исполнения кода из репозиториев.

### 5.5. Сопровождаемость

- логирование через SLF4J;
- DEBUG-логи для snapshot;
- H2 console для отладки;
- понятные сообщения об ошибках в `sync_jobs`.

---

## 6. Модель данных

### 6.1. service_groups

| Колонка | Тип | Назначение |
|---|---|---|
| id | BIGINT | PK |
| name | VARCHAR | Название группы |

### 6.2. git_services

| Колонка | Тип | Назначение |
|---|---|---|
| id | BIGINT | PK |
| group_id | BIGINT | FK к service_groups |
| name | VARCHAR | Название сервиса |
| repo_url | VARCHAR | URL репозитория |
| default_branch | VARCHAR | Последняя определённая ветка |
| last_head_sha | VARCHAR | Последний HEAD |
| last_sync_at | TIMESTAMP | Время последней синхронизации |
| sync_status | VARCHAR | Статус |

Уникальность:

```text
group_id + name
```

### 6.3. sync_jobs

| Колонка | Тип | Назначение |
|---|---|---|
| id | BIGINT | PK |
| service_id | BIGINT | FK к git_services |
| status | VARCHAR | PENDING/RUNNING/SUCCESS/FAILED |
| started_at | TIMESTAMP | Начало |
| finished_at | TIMESTAMP | Окончание |
| commits_processed | BIGINT | Обработанные коммиты |
| files_processed | BIGINT | Обработанные файлы |
| error_message | VARCHAR | Ошибка |

### 6.4. author_service_stats

| Колонка | Тип | Назначение |
|---|---|---|
| id | BIGINT | PK |
| service_id | BIGINT | FK |
| author_email | VARCHAR | Email автора |
| author_name | VARCHAR | Имя автора |
| commits | BIGINT | Коммиты |
| added_code_lines | BIGINT | Добавлено кода |
| removed_code_lines | BIGINT | Удалено кода |
| added_test_lines | BIGINT | Добавлено тестов |
| removed_test_lines | BIGINT | Удалено тестов |
| last_commit_at | TIMESTAMP | Последний коммит |

Уникальность:

```text
service_id + author_email
```

### 6.5. service_loc_snapshots

| Колонка | Тип | Назначение |
|---|---|---|
| id | BIGINT | PK |
| service_id | BIGINT | Unique FK |
| code_lines | BIGINT | Строки кода |
| test_lines | BIGINT | Строки тестов |
| generated_excluded_lines | BIGINT | Исключённые строки |
| code_files | INTEGER | Файлы кода |
| test_files | INTEGER | Файлы тестов |
| generated_files | INTEGER | Исключённые файлы |
| branch | VARCHAR | Ветка |
| head_sha | VARCHAR | HEAD |
| synced_at | TIMESTAMP | Время снимка |

---

## 7. Конфигурация

Пример `application.yml`:

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

---

## 8. Требования к тестированию

Минимальный набор:

- контекст Spring загружается;
- CSV-валидация работает на корректных и некорректных файлах;
- классификатор Java-файлов корректно разделяет CODE/TEST/EXCLUDED;
- синхронизация одного репозитория сохраняет snapshot и author stats;
- отсутствующая ветка даёт FAILED job с понятной ошибкой.

В текущем сервисе реализован базовый context load test. Расширенные интеграционные тесты рекомендуются для дальнейшего развития.

---

## 9. Деплой

Сборка:

```bash
mvn clean package
```

Запуск:

```bash
java -jar target/git-stat-0.0.1-SNAPSHOT.jar
```

Для Docker необходимо использовать образ с установленным `git`.

Пример Dockerfile:

```dockerfile
FROM eclipse-temurin:25-jre

RUN apt-get update \
 && apt-get install -y --no-install-recommends git ca-certificates \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY target/git-stat-0.0.1-SNAPSHOT.jar app.jar

ENV GIT_STORAGE_PATH=/var/lib/git-stat/repos
RUN mkdir -p /var/lib/git-stat/repos

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 10. Критерии приёмки

Приёмка сервиса проходит, если:

1. Приложение собирается без ошибок.
2. Приложение запускается.
3. Импорт CSV работает и валидирует ошибки.
4. Импорт `catena.csv` работает при наличии файла.
5. Синхронизация всех сервисов завершается без падения процесса.
6. Для корректных репозиториев заполняются `service_loc_snapshots`.
7. Для корректных репозиториев заполняются `author_service_stats`.
8. UI показывает ненулевой code LOC и test LOC для репозиториев с Java-файлами.
9. Выбор ветки влияет на анализируемый ref.
10. Ошибки по конкретному сервису отображаются в задачах синхронизации.

---

## 11. Будущее развитие

Рекомендуемые следующие шаги:

- добавить PostgreSQL и Flyway;
- добавить авторизацию и роли;
- реализовать параллельную синхронизацию;
- добавить историю snapshot LOC;
- добавить поддержку других языков;
- добавить `.mailmap` или manual author mapping;
- добавить экспорт в CSV/XLSX;
- добавить метрики Micrometer;
- добавить интеграционные тесты на реальных мини-репозиториях.