# LinkTracker

Telegram-бот для отслеживания изменений на GitHub и StackOverflow.

## Архитектура

Проект состоит из двух микросервисов:

- **Bot** (порт 8080) — Telegram-бот для взаимодействия с пользователями
- **Scrapper** (порт 8081) — сервис для периодической проверки обновлений ссылок

## Как запустить

### 1. Получить токен

Создайте бота через [@BotFather](https://t.me/BotFather) в Telegram командой `/newbot` и скопируйте токен.

### 2. Настроить токен

Создайте файл `.env` в корне проекта:

```
TELEGRAM_TOKEN="ваш_токен"
```

### 3. Собрать проект

```bash
./mvnw clean package -DskipTests
```

Для Windows с Java 24:

```powershell
mvnw clean package -DskipTests "-Denforcer.skip=true"
```

### 4. Запустить сервисы

В отдельных терминалах:

```bash
# Scrapper
./mvnw spring-boot:run -pl scrapper

# Bot
./mvnw spring-boot:run -pl bot
```

Или через jar-файлы:

```bash
java -jar scrapper/target/scrapper-0.0.1.jar
java -jar bot/target/bot-0.0.1.jar
```

## Команды бота

- `/start` — регистрация в системе
- `/help` — список команд
- `/track` — начать отслеживание ссылки (диалог с запросом URL и тегов)
- `/untrack <url>` — прекратить отслеживание ссылки
- `/list` — показать все отслеживаемые ссылки
- `/list <tag>` — показать ссылки с определенным тегом
- `/cancel` — отменить текущую операцию
- `/skip` — пропустить добавление тегов

## Поддерживаемые ссылки

- GitHub репозитории: `https://github.com/owner/repo`
- StackOverflow вопросы: `https://stackoverflow.com/questions/123456/title`

## REST API

### Scrapper API (порт 8081)

```bash
# Зарегистрировать чат
POST /tg-chat/{id}

# Добавить ссылку
POST /links
Headers: Tg-Chat-Id: {chatId}
Body: {"url": "...", "tags": ["tag1", "tag2"]}

# Получить список ссылок
GET /links?tag={tag}
Headers: Tg-Chat-Id: {chatId}

# Удалить ссылку
DELETE /links
Headers: Tg-Chat-Id: {chatId}
Body: {"url": "..."}

# Удалить чат
DELETE /tg-chat/{id}
```

### Bot API (порт 8080)

```bash
# Отправить уведомление об обновлении
POST /updates
Body: {"id": 1, "url": "...", "description": "...", "tgChatIds": [123]}
```

## Тестирование

Используйте скрипт для тестирования API:

```powershell
powershell -ExecutionPolicy Bypass -File test-api.ps1
```

Swagger UI:

- Scrapper: http://localhost:8081/swagger-ui.html
- Bot: http://localhost:8080/swagger-ui.html

## Архитектурные паттерны

- **Command Pattern** — команды бота
- **Repository Pattern** — работа с данными
- **Strategy Pattern** — проверка разных типов ссылок (GitHub/StackOverflow)
- **State Machine** — диалог для команды /track
- **Dependency Injection** — Spring IoC

## Структура проекта

```
bot/
├── command/          # Команды бота (Command Pattern)
├── service/          # Бизнес-логика, State Machine
├── repository/       # In-memory хранилище
├── controller/       # REST API для получения уведомлений
└── model/            # Модели данных

scrapper/
├── controller/       # REST API для управления ссылками
├── service/          # Бизнес-логика, валидация
│   └── checker/      # Strategy Pattern для проверки ссылок
├── repository/       # In-memory хранилище
├── scheduler/        # Периодическая проверка обновлений
├── client/           # HTTP клиенты (GitHub, StackOverflow, Bot)
├── model/            # Модели данных
└── exception/        # Кастомные исключения
```

## Особенности реализации

- **State Machine** для диалогового взаимодействия при добавлении ссылок
- **Планировщик** проверяет обновления каждые 10 секунд (настраивается в application.yaml)
- **Валидация** URL через регулярные выражения
- **Теги** для группировки и фильтрации ссылок
- **Кастомные исключения** для бизнес-логики (ChatNotFoundException, LinkNotFoundException и др.)
- **In-memory хранилище** на основе ConcurrentHashMap
- **HTTP клиенты** для интеграции с внешними API

## SOLID принципы

Проект следует принципам SOLID и GRASP (оценка 9.5/10). Подробности в [SOLID_IMPROVEMENTS.md](SOLID_IMPROVEMENTS.md).

## Стек технологий

- Java 24
- Spring Boot 4.0.2
- Maven 3.9.11
- Telegram Bot API (pengrad)
- Lombok
- Validation API
