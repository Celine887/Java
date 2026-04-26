# Link Tracker

Приложение для отслеживания обновлений ссылок GitHub и StackOverflow через Telegram бота.

## Технологии

- Java 21
- Spring Boot 3.4.1
- PostgreSQL 16
- Apache Kafka 3.8.0 (KRaft mode)
- Liquibase (миграции БД)
- Spring Data JPA / JDBC
- Spring Kafka
- Telegram Bot API
- gRPC
- Docker & Docker Compose
- Testcontainers
- Maven 3.9.11

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

#### Запуск Bot (Telegram бот)

```bash
mvn spring-boot:run -pl bot
```

Bot будет доступен на `http://localhost:8080`

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
├── migrations/            # SQL миграции (корневая папка)
├── docker-compose.yml     # Docker Compose (PostgreSQL + Kafka)
├── KAFKA_CONFIGURATION.md # Обоснование настроек Kafka
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
mvn test
```

### Запуск тестов конкретного модуля

```bash
mvn test -pl scrapper
mvn test -pl bot
```

### Запуск конкретного теста

```bash
mvn test -Dtest=JdbcRepositoryTest
mvn test -Dtest=JpaRepositoryTest
```

Тесты используют Testcontainers для автоматического запуска PostgreSQL в Docker.

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

1. Запустите PostgreSQL через Docker Compose
2. Запустите `ScrapperApplication` (порт 8081)
3. Запустите `BotApplication` (порт 8080)

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
