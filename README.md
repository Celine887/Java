# Link Tracker - Упрощённая версия для SRE задания

Простое клиент-серверное приложение для отслеживания обновлений ссылок GitHub и StackOverflow через Telegram бота.

## Технологии

- Java 21
- Spring Boot 3.4.1
- PostgreSQL 16
- Liquibase (миграции БД)
- Spring Data JPA / JDBC
- Telegram Bot API (внешний клиент)
- REST API (HTTP/1.1)
- Docker & Docker Compose
- Maven 3.9.11

## Архитектура

Приложение состоит из одного бэкенд-сервиса (Scrapper) с REST API и фронтенда (Telegram Bot).

```
Telegram Bot (клиент) → HTTP REST API → Scrapper (бэкенд) → PostgreSQL
```

## Требования

- Java 21+
- Maven 3.9.11+
- Docker & Docker Compose
- Telegram Bot Token

## Быстрый старт

### 1. Клонирование репозитория

```bash
git clone https://github.com/Celine887/Java.git
cd Java
git checkout sre
```

### 2. Настройка переменных окружения

Создайте файл `.env` в корне проекта:

```properties
TELEGRAM_TOKEN=your_telegram_bot_token_here
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

### 5. Запуск приложения

```bash
mvn spring-boot:run -pl scrapper
```

Scrapper будет доступен на `http://localhost:8080`

## Конфигурация

### Выбор способа работы с БД

В файле `scrapper/src/main/resources/application.yaml` можно настроить способ доступа к БД:

```yaml
app:
  database:
    access-type: jpa  # или jdbc
```

Доступные варианты:
- `jpa` - использование Spring Data JPA (ORM)
- `jdbc` - использование чистого JDBC (raw SQL)

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
    interval: 60s
    batch-size: 100
```

## Структура проекта

```
link-tracker/
├── scrapper/              # Бэкенд сервис (REST API)
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   ├── controller/     # REST контроллеры
│       │   │   ├── service/        # Бизнес-логика
│       │   │   ├── repository/     # Доступ к БД (JDBC/JPA)
│       │   │   ├── entity/         # JPA сущности
│       │   │   ├── model/          # DTO модели
│       │   │   ├── client/         # HTTP клиенты (GitHub, StackOverflow)
│       │   │   └── scheduler/      # Планировщик проверки обновлений
│       │   └── resources/
│       │       └── db/
│       │           └── changelog/  # Liquibase миграции
│       └── test/
├── migrations/            # SQL миграции
├── docker-compose.yml     # Docker Compose (PostgreSQL)
├── Отчёт.md              # Отчёт о соответствии 12-факторной методологии
└── pom.xml               # Maven конфигурация
```

## Миграции базы данных

Миграции выполняются автоматически при запуске приложения через Liquibase.

### Схема базы данных

Основные таблицы:
- `chats` - зарегистрированные пользователи
- `links` - отслеживаемые ссылки
- `tags` - теги для ссылок
- `link_chat` - связь пользователей и ссылок (many-to-many)
- `link_tag` - связь ссылок и тегов (many-to-many)

## REST API

### Регистрация чата

```http
POST /tg-chat/{id}
```

Регистрирует нового пользователя (Telegram чат).

### Удаление чата

```http
DELETE /tg-chat/{id}
```

Удаляет пользователя и все его ссылки.

### Получение всех ссылок

```http
GET /links
Header: Tg-Chat-Id: {chatId}
```

Возвращает список всех отслеживаемых ссылок пользователя с тегами.

### Добавление ссылки

```http
POST /links
Header: Tg-Chat-Id: {chatId}
Content-Type: application/json

{
  "url": "https://github.com/user/repo",
  "tags": ["java", "spring"]
}
```

Добавляет новую ссылку для отслеживания с опциональными тегами.

### Удаление ссылки

```http
DELETE /links
Header: Tg-Chat-Id: {chatId}
Content-Type: application/json

{
  "url": "https://github.com/user/repo"
}
```

Удаляет ссылку из отслеживания.

## Тестирование

### Запуск всех тестов

```bash
mvn clean test
```

### Запуск тестов scrapper

```bash
mvn test -pl scrapper
```

Тесты включают:
- Unit-тесты сервисов
- Integration-тесты с PostgreSQL (через Testcontainers)
- Тесты миграций БД
- Тесты JDBC и JPA репозиториев

## Поддерживаемые ссылки

### GitHub

- Репозитории: `https://github.com/user/repo`
- Отслеживает: новые Issues и Pull Requests

### StackOverflow

- Вопросы: `https://stackoverflow.com/questions/{id}`
- Отслеживает: новые ответы и комментарии

## Telegram Bot (фронтенд)

Пользователи взаимодействуют с приложением через Telegram бота. Бот использует REST API для управления ссылками:

- `/start` - начать работу с ботом
- `/track <url>` - добавить ссылку для отслеживания
- `/untrack <url>` - удалить ссылку
- `/list` - показать все отслеживаемые ссылки

## Разработка

### Запуск из IDE

1. Запустите PostgreSQL через Docker Compose
2. Запустите `ScrapperApplication` из IDE
3. Приложение будет доступно на `http://localhost:8080`

### Переключение между JDBC и JPA

Измените в `application.yaml`:

```yaml
app:
  database:
    access-type: jdbc  # или jpa
```

Перезапустите приложение. Изменения применятся автоматически благодаря Spring Profiles.

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
