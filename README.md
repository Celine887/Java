# Link Tracker

Приложение для отслеживания обновлений ссылок GitHub и StackOverflow через Telegram бота.

## Технологии

- Java 21
- Spring Boot 3.4.1
- PostgreSQL 16
- Liquibase (миграции БД)
- Spring Data JPA / JDBC
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

### 3. Запуск PostgreSQL

```bash
docker-compose up -d
```

Это запустит PostgreSQL на порту 5432 с базой данных `link_tracker`.

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

### Настройка планировщика

```yaml
app:
  scheduler:
    enabled: true
    interval: 60s  # интервал проверки обновлений
```

## Структура проекта

```
link-tracker/
├── bot/                    # Telegram бот
│   └── src/
│       ├── main/
│       └── test/
├── scrapper/              # Бэкенд сервис
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   └── resources/
│       │       └── db/
│       │           └── changelog/  # Liquibase миграции
│       └── test/
├── migrations/            # SQL миграции (корневая папка)
├── docker-compose.yml     # Docker Compose конфигурация
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

- GitHub репозитории: `https://github.com/user/repo`
- StackOverflow вопросы: `https://stackoverflow.com/questions/{id}`

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
