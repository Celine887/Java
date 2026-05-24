# Link Tracker

Приложение для отслеживания обновлений ссылок GitHub и StackOverflow через Telegram бота.

## Технологии

- Java 21
- Spring Boot 3.4.1
- PostgreSQL 16
- Apache Kafka 3.8.0 (KRaft mode)
- Valkey 7.2 (Redis-compatible in-memory cache)
- Liquibase (миграции БД)
- Spring Data JPA / JDBC
- Spring Kafka
- Telegram Bot API
- gRPC
- Docker & Docker Compose
- Testcontainers
- Maven 3.9.11

## Архитектура

Проект состоит из трех основных сервисов:

```
Scrapper Service → Kafka (link.raw-updates) → AI Agent Service → Kafka (link.processed-updates) → Bot Service → Telegram API
```

### AI Agent Service (HW-9)

AI Agent Service - промежуточный сервис для интеллектуальной обработки обновлений:

**Функции:**
1. **Фильтрация** - отсеивает spam, короткие сообщения, исключенных авторов
2. **Суммаризация** - сокращает длинные описания (>500 символов)
3. **Приоритизация** - определяет важность (HIGH/MEDIUM/LOW) по ключевым словам
4. **Группировка** - объединяет связанные обновления в одно сообщение

**Конфигурация** (`ai-agent/src/main/resources/application.yaml`):

```yaml
app:
  ai-agent:
    filtering:
      stop-words: [spam, ads, promo]
      excluded-authors: [bot-user]
      min-length: 20
    summarization:
      threshold: 500
      mode: stub  # stub или ai-api
    prioritization:
      high-keywords: [critical, urgent, breaking, security]
      low-keywords: [minor, typo, chore, docs]
    grouping:
      window-ms: 30000  # 30 секунд
```

**Топики Kafka:**
- Входной: `link.raw-updates` (от Scrapper)
- Выходной: `link.processed-updates` (к Bot)

## Требования

- Java 21+
- Maven 3.9.11+
- Docker & Docker Compose
- Telegram Bot Token

## Быстрый старт

### 1. Клонирование репозитория

```bash
git clone <repository-url>
cd link-tracker
```

### 2. Настройка переменных окружения

Создайте файл `.env` в корне проекта:

```properties
TELEGRAM_BOT_TOKEN=your_telegram_bot_token_here
TELEGRAM_BOT_NAME=your_bot_name
```

### 3. Запуск инфраструктуры (PostgreSQL + Kafka)

```bash
docker-compose up -d
```

Это запустит:
- PostgreSQL на порту 5432 с базой данных `link_tracker`
- Kafka кластер из 3 брокеров (порты 9092, 9094, 9096)
- Kafka UI на порту 8090 (http://localhost:8090)

Дождитесь полного запуска Kafka (около 30-60 секунд). Проверить статус можно через Kafka UI.

### 4. Сборка проекта

```bash
mvn clean install
```

### 5. Запуск приложений

#### Запуск Scrapper (бэкенд)

```bash
mvn spring-boot:run -pl scrapper
```

Scrapper будет доступен на `http://localhost:8081`

#### Запуск AI Agent (обработка обновлений)

```bash
mvn spring-boot:run -pl ai-agent
```

AI Agent будет доступен на `http://localhost:8082`

#### Запуск Bot (Telegram бот)

```bash
mvn spring-boot:run -pl bot
```

Bot будет доступен на `http://localhost:8080`

**Порядок запуска:**
1. Сначала запустите инфраструктуру (PostgreSQL + Kafka)
2. Затем Scrapper
3. Затем AI Agent
4. И наконец Bot

## Конфигурация

### Выбор способа работы с БД

В файле `scrapper/src/main/resources/application.yaml` можно настроить способ доступа к БД:

```yaml
app:
  database:
    access-type: jpa  # или jdbc, или memory
```

Доступные варианты:
- `jpa` - использование Spring Data JPA (ORM)
- `jdbc` - использование чистого JDBC (raw SQL)
- `memory` - in-memory хранилище (для разработки)

### Настройка базы данных

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/link_tracker
    username: postgres
    password: postgres
```

### Настройка Kafka

#### Выбор транспорта для уведомлений

В файле `scrapper/src/main/resources/application.yaml`:

```yaml
app:
  notification:
    transport: kafka  # kafka (default), http, или grpc
```

Доступные варианты:
- `kafka` - асинхронная отправка через Apache Kafka (рекомендуется, по умолчанию)
- `http` - синхронная отправка через REST API
- `grpc` - синхронная отправка через gRPC

#### Конфигурация Kafka

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092,localhost:9094,localhost:9096
    producer:
      acks: all
      retries: 3
    consumer:
      group-id: bot-consumer-group
      auto-offset-reset: earliest

app:
  kafka:
    topic:
      link-updates: link-updates
      link-updates-dlq: link-updates-dlq
```

#### Kafka Cluster

Кластер состоит из:
- 3 брокера Kafka (отказоустойчивость)
- KRaft mode (без Zookeeper)
- Топик `link-updates`: 3 партиции, replication factor 3
- Топик `link-updates-dlq`: 1 партиция для failed messages
- Kafka UI для мониторинга: http://localhost:8090

Подробности конфигурации см. в [KAFKA_CONFIGURATION.md](KAFKA_CONFIGURATION.md)

### Настройка Valkey (Redis-compatible cache)

#### Включение кэширования

В файле `scrapper/src/main/resources/application.yaml`:

```yaml
app:
  valkey:
    enabled: true
    host: localhost
    port: 6379
    password:  # оставьте пустым для локальной разработки
    ttl-seconds: 300  # время жизни кэша (5 минут)
    client-side-caching:
      enabled: false  # экспериментальная функция CSC
```

#### Запуск Valkey

Valkey запускается автоматически через Docker Compose:

```bash
docker-compose up -d
```

Или вручную:

```bash
docker run --name valkey-standalone -p 6379:6379 valkey/valkey:7.2-alpine
```

#### Что кэшируется?

- **GET /links** - список отслеживаемых ссылок пользователя
- Ключ кэша: `Tg-Chat-Id` (ID чата пользователя)
- Формат: JSON с полным списком ссылок и тегов
- TTL: 300 секунд (5 минут)

#### Инвалидация кэша

Кэш автоматически инвалидируется при:
- Добавлении новой ссылки (POST /links)
- Удалении ссылки (DELETE /links)
- Истечении TTL

#### Client-Side Caching (CSC)

CSC - это продвинутая функция Valkey/Redis, которая кэширует данные на стороне клиента (в памяти приложения) и автоматически инвалидирует их при изменении на сервере через CLIENT TRACKING protocol.

**Преимущества CSC:**
- Нулевая задержка для кэшированных данных (данные в памяти приложения)
- Автоматическая инвалидация через Pub/Sub
- Снижение сетевого трафика между приложением и Valkey
- Дополнительное ускорение поверх обычного кэширования

**Для включения CSC:**

```yaml
app:
  valkey:
    client-side-caching:
      enabled: true
```

**Как это работает:**
1. При первом запросе данные загружаются из Valkey и кэшируются локально
2. Последующие запросы возвращаются мгновенно из локального кэша
3. При изменении данных на сервере Valkey отправляет invalidation message
4. Локальный кэш автоматически очищается
5. Следующий запрос снова загружает свежие данные

**Требования:**
- Valkey 7.2+ или Redis 6.0+
- Lettuce client с поддержкой CLIENT TRACKING
- Два соединения: data connection + pub/sub connection

**Graceful Degradation:**
Если CLIENT TRACKING не поддерживается сервером, CSC автоматически переключается в manual mode с ручной инвалидацией при записи.

### Настройка планировщика

```yaml
app:
  scheduler:
    enabled: true
    interval: 60s              # интервал проверки обновлений
    batch-size: 1000           # размер батча для загрузки ссылок
    sub-batch-size: 250        # размер sub-batch для параллельной обработки
    thread-pool-size: 4        # количество потоков для обработки
    force-check-delay: 10s     # задержка перед принудительной проверкой
```

#### Многопоточная обработка

Планировщик реализует многопоточную пакетную обработку:
1. Загружает батч из 1000 ссылок
2. Делит батч на 4 sub-batches по 250 ссылок
3. Обрабатывает каждый sub-batch параллельно в отдельном потоке
4. Изолирует ошибки - сбой одной ссылки не влияет на другие
5. Логирует failed links для мониторинга

## Тестирование

### Запуск всех тестов

```bash
mvn clean test
```

### Интеграционный тест Scrapper → Kafka → Bot

Для проверки полного пути сообщения запустите:

```bash
mvn test -pl scrapper -Dtest=ScrapperToKafkaToBotIntegrationTest
```

Этот тест:
1. Запускает Kafka через Testcontainers
2. Отправляет update через Scrapper
3. Проверяет получение сообщения в Kafka
4. Симулирует обработку Bot'ом

### Тесты с Kafka

Все тесты с Kafka используют Testcontainers и автоматически поднимают Kafka кластер.
Первый запуск может занять время на скачивание Docker образа.

## Структура проекта

```
link-tracker/
├── bot/                    # Telegram бот
│   └── src/
│       ├── main/
│       │   └── java/
│       │       └── kafka/  # Kafka consumer
│       └── test/
├── scrapper/              # Бэкенд сервис
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── client/
│       │   │       ├── KafkaUpdateClient.java  # Kafka producer
│       │   │       ├── BotClient.java          # HTTP client
│       │   │       └── GrpcUpdateClient.java   # gRPC client
│       │   └── resources/
│       │       └── db/
│       │           └── changelog/  # Liquibase миграции
│       └── test/
│           └── integration/
│               └── ScrapperToKafkaToBotIntegrationTest.java
├── ai-agent/              # AI Agent сервис (HW-9)
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   ├── service/
│       │   │   │   ├── FilteringService.java       # Фильтрация
│       │   │   │   ├── SummarizationService.java   # Суммаризация
│       │   │   │   ├── PrioritizationService.java  # Приоритизация
│       │   │   │   └── GroupingService.java        # Группировка
│       │   │   ├── kafka/
│       │   │   │   └── AiAgentKafkaConsumer.java   # Kafka consumer/producer
│       │   │   └── model/
│       │   │       ├── LinkRawUpdateEvent.java     # Входное событие
│       │   │       ├── LinkProcessedUpdateEvent.java # Выходное событие
│       │   │       └── Priority.java               # HIGH/MEDIUM/LOW
│       │   └── resources/
│       │       └── application.yaml  # Конфигурация AI Agent
│       └── test/
│           ├── service/
│           │   ├── PrioritizationServiceTest.java  # Unit-тесты
│           │   └── GroupingServiceTest.java        # Unit-тесты
│           └── integration/
│               └── KafkaIntegrationTest.java       # Integration-тесты
├── migrations/            # SQL миграции (корневая папка)
├── docker-compose.yml     # Docker Compose (PostgreSQL + Kafka)
├── KAFKA_CONFIGURATION.md # Обоснование настроек Kafka
├── MR_CHECKLIST.md        # Чек-лист для Merge Request
└── pom.xml               # Maven конфигурация
```

## Миграции базы данных

Миграции выполняются автоматически при запуске приложения через Liquibase.

Файлы миграций находятся в:
- `scrapper/src/main/resources/db/changelog/`
- `migrations/` (корневая папка проекта)

### Схема базы данных

Основные таблицы:
- `chats` - зарегистрированные пользователи
- `links` - отслеживаемые ссылки
- `tags` - теги для ссылок
- `link_chat` - связь пользователей и ссылок
- `link_tag` - связь ссылок и тегов

## API

### Scrapper API

#### Регистрация чата

```http
POST /tg-chat/{id}
```

#### Удаление чата

```http
DELETE /tg-chat/{id}
```

#### Получение всех ссылок

```http
GET /links
Header: Tg-Chat-Id: {chatId}
```

#### Добавление ссылки

```http
POST /links
Header: Tg-Chat-Id: {chatId}
Content-Type: application/json

{
  "url": "https://github.com/user/repo",
  "tags": ["java", "spring"]
}
```

#### Удаление ссылки

```http
DELETE /links
Header: Tg-Chat-Id: {chatId}
Content-Type: application/json

{
  "url": "https://github.com/user/repo"
}
```

## Тестирование

### Запуск всех тестов

```bash
mvn clean test
```

### Запуск тестов конкретного модуля

```bash
mvn test -pl scrapper
mvn test -pl bot
mvn test -pl ai-agent
```

### AI Agent тесты

#### Unit-тесты

```bash
# Тесты приоритизации
mvn test -pl ai-agent -Dtest=PrioritizationServiceTest

# Тесты группировки
mvn test -pl ai-agent -Dtest=GroupingServiceTest
```

#### Integration-тесты с Kafka

```bash
mvn test -pl ai-agent -Dtest=KafkaIntegrationTest
```

Тесты проверяют:
- TC-3.1: Успешную публикацию с HIGH/MEDIUM/LOW приоритетом
- TC-3.2: Фильтрацию spam, коротких сообщений, исключенных авторов

### Интеграционный тест Scrapper → Kafka → Bot

Для проверки полного пути сообщения запустите:

```bash
mvn test -pl scrapper -Dtest=ScrapperToKafkaToBotIntegrationTest
```

Этот тест:
1. Запускает Kafka через Testcontainers
2. Отправляет update через Scrapper
3. Проверяет получение сообщения в Kafka
4. Симулирует обработку Bot'ом

### Тесты с Kafka

Все тесты с Kafka используют Testcontainers и автоматически поднимают Kafka кластер.
Первый запуск может занять время на скачивание Docker образа.

## Команды бота

- `/start` - начать работу с ботом
- `/help` - показать справку
- `/track` - добавить ссылку для отслеживания
- `/untrack` - удалить ссылку из отслеживания
- `/list` - показать все отслеживаемые ссылки

## Поддерживаемые ссылки

### GitHub

- Репозитории: `https://github.com/user/repo`
- Отслеживает: новые Issues и Pull Requests

### StackOverflow

- Вопросы: `https://stackoverflow.com/questions/{id}`
- Отслеживает: новые ответы и комментарии

### Формат уведомлений

Уведомления содержат:
- Название Issue/PR или текст вопроса
- Имя автора
- Время создания
- Превью описания (первые 200 символов)
- Ссылку на обновление

## Разработка

### Запуск из IDE

1. Запустите PostgreSQL и Kafka через Docker Compose
2. Запустите `ScrapperApplication` (порт 8081)
3. Запустите `AiAgentApplication` (порт 8082)
4. Запустите `BotApplication` (порт 8080)

### Переключение между JDBC и JPA

Измените в `application.yaml`:

```yaml
app:
  database:
    access-type: jdbc  # или jpa
```

Перезапустите приложение.

## Troubleshooting

### База данных не запускается

Проверьте, что Docker запущен:

```bash
docker ps
```

Перезапустите контейнер:

```bash
docker-compose down
docker-compose up -d
```

### Ошибка подключения к БД

Убедитесь, что PostgreSQL запущен и доступен на порту 5432:

```bash
docker-compose ps
```

### Тесты падают

Убедитесь, что Docker запущен (для Testcontainers):

```bash
docker ps
```

## Лицензия

Этот проект создан в образовательных целях.

## Нагрузочное тестирование

Проект включает полный набор для нагрузочного тестирования с Apache JMeter.

### Быстрый старт

1. Установите Apache JMeter 5.6.3+
2. Сгенерируйте тестовые данные (100,000 ссылок):

```bash
psql -U postgres -d link_tracker -f load-testing/generate-test-data.sql
```

3. Запустите автоматизированные тесты:

```powershell
cd load-testing
.\run-load-tests.ps1
```

Скрипт автоматически запустит два теста:
- Тест БЕЗ кэширования
- Тест С кэшированием Valkey

### Результаты нагрузочного тестирования

Подробные результаты и сравнение производительности см. в [load-testing/README.md](load-testing/README.md)

#### Конфигурация тестов

- **Concurrent Users**: 16 потоков (2 * CPU cores)
- **Ramp-up Period**: 60 секунд
- **Test Duration**: 300 секунд (5 минут)
- **Operations Ratio**: 100:1 (GET:POST/DELETE)
- **Test Data**: 1000 пользователей, 100,000 ссылок

#### Ожидаемые улучшения с кэшем

При использовании Valkey кэширования ожидается:
- Увеличение RPS (requests per second) в 5-10 раз
- Снижение среднего времени ответа на 80-90%
- Снижение 99th percentile на 85-95%
- Снижение нагрузки на PostgreSQL

Точные результаты зависят от конфигурации системы и будут добавлены после выполнения тестов.
