# Отчёт по проекту (Этап 1)

## NotifyHub — реактивная система уведомлений

**Предметная область:** система уведомлений  
**Реализация:** Project Reactor / Spring WebFlux + GUI  
**Дата:** 07.02.2026

| Параметр | Значение |
|---|---|
| Команда | 2 студента (совместная разработка) |
| Цель проекта | Приложение с GUI и потоковой доставкой данных, демонстрирующее преимущества реактивного подхода |
| Ключевое ограничение | Пользователь получает данные от источника без блокирующих вызовов на промежуточных этапах обработки |

---

## 1. Описание системы

NotifyHub — система, которая принимает события из нескольких источников (GitHub API, RSS, внутренний генератор), обрабатывает их потоками (Flux) с правилами, дедупликацией и контролем частоты, а затем доставляет пользователям уведомления по нескольким каналам (real-time в GUI + Telegram или Email).

GUI предназначен для: (1) настройки подписок/правил, (2) просмотра потока уведомлений в реальном времени, (3) анализа статусов доставки и ошибок, (4) просмотра истории уведомлений (лог).

### 1.1 Типовой пользовательский сценарий

> **Статус: полностью реализован и работает end-to-end.**

1. Пользователь регистрируется через GUI (Login/Register).
2. Выбирает источник (например, GitHub Releases), задаёт параметры (репозиторий) и включает подписку.
3. Пользователь добавляет правила: приоритет, ограничение частоты, «не беспокоить» по времени.
4. Система начинает получать поток событий, преобразует их в единый формат и применяет правила.
5. Если событие проходит фильтры — формируется уведомление, сохраняется в журнале и отправляется в UI и выбранные каналы.
6. В GUI пользователь видит новые уведомления мгновенно (SSE) и может фильтровать/искать историю.

---

## 2. Цели и задачи

### 2.1 Задачи, демонстрирующие реактивность

| Задача | Статус | Где в коде |
|---|---|---|
| Неблокирующий приём данных из источников: WebClient + Flux.interval / push-потоки | ✅ Реализовано | `IngestService` — `Flux.interval(Duration.ofSeconds(60))`, коннекторы используют `WebClient` |
| Неблокирующая обработка: операторы map/filter/flatMap/window/buffer/debounce, разделение потоков по пользователям | ✅ Реализовано | `RuleEngine` — filter по keyword, `IngestService` — flatMap с группировкой подписок, `NotificationSinkService` — filter по userId |
| Backpressure: ограничение потребления и буферизация с лимитами, предотвращение перегрузки GUI и каналов доставки | ✅ Реализовано | `NotificationSinkService` — `Sinks.many().multicast().onBackpressureBuffer(1000)`, `IngestService` — `flatMap(..., 4)` для ограничения concurrency |
| Обработка ошибок как часть пайплайна: retryWhen(backoff), timeout, onErrorResume, изоляция источников | ✅ Реализовано | `DeliveryService` — `Retry.backoff(3, Duration.ofSeconds(2))`, все коннекторы — `.onErrorResume()` для изоляции ошибок |
| Параллельная доставка по каналам с ограничением concurrency | ✅ Реализовано | `IngestService` — `flatMap(event -> processEvent(event, subs), 4)` — параллельность 4 |

### 2.2 Границы проекта (MVP)

| Требование | Статус | Комментарий |
|---|---|---|
| Авторизация (JWT), базовые роли (user/admin — admin опционально) | ✅ Реализовано | JWT с claims `userId`, `role`, `username`. Роль `USER` по умолчанию. Разграничение admin пока не реализовано на уровне UI — в БД поле `role` есть, токен его содержит |
| Минимум 2 источника + генератор событий для демонстрации нагрузки | ✅ Реализовано | **GitHub Releases** (WebClient), **RSS/Atom** (WebClient + ROME), **Event Generator** (Flux.generate) — всего 3 коннектора |
| Минимум 2 канала доставки (UI real-time + Telegram или Email) | ⚠️ Частично | **UI real-time (SSE)** — полностью работает. **Telegram** — код написан (`TelegramDeliveryChannel`), использует WebClient, но требует настройки `TELEGRAM_BOT_TOKEN` и `telegram_chat_id` у пользователя, **UI для настройки chat_id пока нет**. **Email** — код написан (`EmailDeliveryChannel`), обёрнут в `boundedElastic`, но требует SMTP-настроек |
| Персистентное хранение: пользователи, подписки/правила, события, уведомления и статусы доставки | ✅ Реализовано | PostgreSQL 16 + R2DBC, 5 таблиц с индексами, автосоздание схемы через `ConnectionFactoryInitializer` |
| GUI: панели «Лента», «Подписки», «Правила», «История», «Мониторинг» | ✅ Реализовано | React + TypeScript + Vite, 6 экранов: Dashboard, Subscriptions, Rules, History, Monitoring + Login/Register |

---

## 3. Источники данных и формат событий

### Реализованные источники

| Источник | Тип | Неблокирующий способ получения | Статус |
|---|---|---|---|
| GitHub Releases | HTTP JSON | `WebClient` + периодический опрос (`Flux.interval`); поддержка `GITHUB_TOKEN` для увеличения rate limit | ✅ Работает |
| RSS/Atom | HTTP XML | `WebClient` + реактивный парсинг через ROME (`boundedElastic` для блокирующего парсера) | ✅ Работает |
| OpenWeather (опц.) | HTTP JSON | WebClient + poll | ❌ Не реализовано (опционально по ТЗ) |
| Event Generator | внутренний | `Flux.interval` + генерация случайных событий (1-3 за тик) | ✅ Работает |

### 3.1 Нормализованный формат Event

> **Статус: полностью реализован.** Класс `com.notifyhub.model.Event`, таблица `events`.

| Поле | Описание | Реализация |
|---|---|---|
| `id` | внутренний UUID | `UUID`, генерируется БД через `gen_random_uuid()` |
| `sourceType` | тип источника (GITHUB/RSS/GEN) | `String`, заполняется коннектором |
| `externalId` | идентификатор события в источнике (для дедупликации) | `String`, формат: `"github:owner/repo:id"`, `"rss:url:entry-uri"`, `"gen:uuid"` |
| `title` | краткий заголовок | `String` |
| `payloadJson` | исходные данные события (JSON) | `TEXT` в БД, `String` в Java |
| `priority` | LOW/MEDIUM/HIGH (может вычисляться правилом) | `String`, по умолчанию `MEDIUM`, переопределяется `RuleEngine` |
| `createdAt` | время поступления | `LocalDateTime` |

---

## 4. Требования

### 4.1 Функциональные требования

| ID | Требование | Статус | Детали реализации |
|---|---|---|---|
| **F1** | Регистрация и вход пользователя (JWT) | ✅ Реализовано | `AuthController` — POST `/api/auth/register`, POST `/api/auth/login`. Пароль хешируется BCrypt. Токен содержит `userId`, `role`, `username`. Срок жизни 24ч |
| **F2** | CRUD подписок: источник + параметры | ✅ Реализовано | `SubscriptionController` — GET/POST/PUT/DELETE `/api/subscriptions`. Параметры передаются как JSON-строка. Проверка принадлежности по userId |
| **F3** | CRUD правил: фильтры по ключевым словам, дедуп по времени, ограничение частоты, приоритет | ✅ Реализовано | `RuleController` — GET/POST/PUT/DELETE `/api/rules`. `RuleEngine` применяет: keyword filter (через запятую), rate limit per hour (подсчёт из БД), quiet hours (с поддержкой перехода через полночь), приоритет. **Примечание:** dedup_window_minutes записывается в БД, но активная проверка по временному окну пока не подключена — дедупликация работает глобально по `externalId` |
| **F4** | Поток уведомлений в GUI в реальном времени (SSE) | ✅ Реализовано | `StreamController` — GET `/api/stream/notifications` (text/event-stream). `NotificationSinkService` — `Sinks.Many` с `onBackpressureBuffer(1000)`. Heartbeat каждые 30с. Фильтрация по userId. На фронтенде — `EventSource` API с автообновлением ленты |
| **F5** | Доставка по внешнему каналу: Telegram или Email, с сохранением статуса попыток | ⚠️ Частично | Код Telegram (`TelegramDeliveryChannel`) и Email (`EmailDeliveryChannel`) **написан полностью**, включая retry с backoff и статусы CREATED→QUEUED→SENT/FAILED. **Но:** (1) Telegram требует env-переменные `TELEGRAM_BOT_TOKEN` + `TELEGRAM_ENABLED=true` и заполненный `telegram_chat_id` у пользователя — **в GUI пока нет формы для ввода chat_id**; (2) Email требует SMTP-настройки (`MAIL_HOST`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_ENABLED=true`) — **настройка только через env-переменные** |

---

## 5. Технологический стек

| Компонент | Технология | Статус |
|---|---|---|
| Backend | Spring Boot 3.3.5 + WebFlux (Netty) | ✅ Работает |
| DB | PostgreSQL 16 + R2DBC (реактивный драйвер `org.postgresql:r2dbc-postgresql`) | ✅ Работает |
| Message broker (опц.) | Kafka / RabbitMQ | ❌ Не реализовано (опционально по ТЗ, заменён внутренним `Sinks.Many`) |
| Cache (опц.) | Redis reactive | ❌ Не реализовано (опционально по ТЗ, дедупликация через БД) |
| GUI | React 18 + TypeScript + Vite | ✅ Работает |
| Real-time transport | SSE (Server-Sent Events) | ✅ Работает |
| Real-time transport (альт.) | WebSocket | ❌ Не реализовано (опционально, SSE покрывает задачу) |
| Delivery | Telegram Bot API (WebClient) / Email (JavaMailSender + boundedElastic) | ⚠️ Код написан, требует настройки (см. F5) |
| Observability | Micrometer + Actuator | ✅ Работает (`/actuator/health`, `/actuator/metrics`, `/api/monitoring/stats`) |
| Testing | JUnit 5, StepVerifier, Testcontainers | ❌ Тесты пока не написаны |

---

## 6. Архитектура

Система разделяется на модули: **Ingest** (коннекторы), **Rule Engine**, **Notification Service**, **Delivery**, **Storage** (R2DBC), **Real-time API** (SSE) и **GUI**. В текущей реализации — один backend-сервис с пакетами/модулями.

### Структура пакетов

```
com.notifyhub/
├── config/          SecurityConfig, DatabaseConfig, WebClientConfig, CorsConfig
├── security/        JwtUtil, JwtAuthenticationFilter
├── model/           User, Subscription, Rule, Event, Notification + enums
├── dto/             AuthRequest, AuthResponse, RegisterRequest, SubscriptionDto, RuleDto, NotificationDto
├── repository/      UserRepository, SubscriptionRepository, RuleRepository, EventRepository, NotificationRepository
├── service/         AuthService, SubscriptionService, RuleService, NotificationService, NotificationSinkService
├── ingest/          IngestService, SourceConnector (interface), GitHubSourceConnector, RssSourceConnector, EventGeneratorConnector
├── engine/          RuleEngine
├── delivery/        DeliveryService, TelegramDeliveryChannel, EmailDeliveryChannel
├── controller/      AuthController, SubscriptionController, RuleController, NotificationController, StreamController, MonitoringController
└── NotifyHubApplication.java
```

### 6.1 Последовательность обработки одного события

```
Source (Flux<Event>)
  → IngestService: poll через WebClient / Flux.interval
    → map + normalize (в единый формат Event)
      → dedup по externalId (проверка в БД)
        → save Event в PostgreSQL (R2DBC)
          → для каждой подписки:
            → загрузить правила из БД
              → RuleEngine: filter/keyword/rateLimit/quietHours
                → если прошло: создать Notification (UI + TELEGRAM)
                  → save Notification в PostgreSQL
                    → push в SSE Sink (для UI)
                    → DeliveryService: retry/backoff → Telegram/Email
                      → update статус: SENT / FAILED
```

---

## 7. Реактивные пайплайны и демонстрация преимуществ

### 7.1 Контроль нагрузки и частоты

| Механизм | Где используется | Код |
|---|---|---|
| `Flux.interval` для периодического приёма данных | `IngestService` — основной цикл опроса | `Flux.interval(Duration.ofSeconds(5), Duration.ofSeconds(pollIntervalSeconds))` |
| `onBackpressureBuffer(limit)` при перегрузке | `NotificationSinkService` — буфер SSE-уведомлений | `Sinks.many().multicast().onBackpressureBuffer(1000)` |
| Rate limit для ограничения частоты уведомлений (анти-спам) | `RuleEngine.checkRateAndDedup()` — подсчёт уведомлений за последний час | `notificationRepository.countByUserIdAndChannelSince(userId, "UI", oneHourAgo)` |

> **Примечание:** debounce / throttleFirst и window(Duration) / buffer для агрегирования событий в пакетные уведомления — зарезервированы в архитектуре, но в текущей версии не задействованы.

### 7.2 Надёжность и работа с ошибками

| Механизм | Где используется | Код |
|---|---|---|
| `retryWhen(backoff)` для временных ошибок каналов | `DeliveryService` — Telegram и Email | `Retry.backoff(3, Duration.ofSeconds(2))` — 3 попытки с экспоненциальным backoff |
| `onErrorResume` для деградации и продолжения работы | Все коннекторы, IngestService | `.onErrorResume(e -> { log.error(...); return Flux.empty(); })` — ошибка одного источника не останавливает остальные |
| Изоляция потоков | `IngestService.processAllSubscriptions()` | Каждая группа подписок обрабатывается независимо, ошибка одной не влияет на другие |
| Статус попыток доставки | `Notification.attempts`, `Notification.lastError` | Инкрементируется при каждой попытке, текст ошибки сохраняется |

### 7.3 Параллельность

| Механизм | Где используется | Код |
|---|---|---|
| `flatMap` с ограничением concurrency при доставке | `IngestService` | `flatMap(entry -> ..., 4)` — максимум 4 параллельных опроса |
| `subscribeOn(boundedElastic)` для разделения I/O и блокирующего кода | `RssSourceConnector` (ROME-парсинг), `EmailDeliveryChannel` (SMTP) | Блокирующие операции изолированы на `Schedulers.boundedElastic()` |

---

## 8. Модель данных

> **Статус: полностью реализована.** Схема создаётся автоматически через `ConnectionFactoryInitializer` + `schema.sql`.

| Таблица | Назначение | Статус |
|---|---|---|
| `users` | Учётные записи пользователей (username, email, password BCrypt, role, telegram_chat_id) | ✅ |
| `subscriptions` | Подписки на источники + параметры (user_id, source_type, params JSON, enabled) | ✅ |
| `rules` | Набор правил на подписку (keyword_filter, dedup_window, rate_limit, priority, quiet_hours) | ✅ |
| `events` | Нормализованные события (UUID, source_type, external_id, title, payload_json, priority) | ✅ |
| `notifications` | Уведомления + канал + статус + попытки + ошибка (user_id, event_id, channel, status, attempts, last_error) | ✅ |

Индексы: `user_id`, `subscription_id`, `enabled`, `status`, `source_type`, `external_id`, `created_at`.

### 8.1 Статусы доставки

| Статус | Описание | Реализован |
|---|---|---|
| CREATED | Уведомление сформировано и записано в БД | ✅ |
| QUEUED | Поставлено на отправку (в DeliveryService) | ✅ |
| SENT | Успешно отправлено в канал | ✅ |
| FAILED | Ошибка (last_error, attempts) | ✅ |

---

## 9. Внешние интерфейсы (API)

| Тип | Маршрут | Описание | Статус |
|---|---|---|---|
| REST | POST `/api/auth/register`, POST `/api/auth/login` | Регистрация и вход (JWT) | ✅ |
| REST | GET/POST/PUT/DELETE `/api/subscriptions` | Управление подписками | ✅ |
| REST | GET/POST/PUT/DELETE `/api/rules` | Управление правилами | ✅ |
| REST | GET `/api/notifications?page=&size=&status=` | Поиск по истории уведомлений | ✅ |
| STREAM | GET `/api/stream/notifications` (SSE) | Поток уведомлений в реальном времени | ✅ |
| STREAM | WS `/api/ws` (опц.) | Альтернатива SSE (WebSocket) | ❌ Не реализовано |
| OBS | GET `/actuator/health`, `/actuator/metrics` | Наблюдаемость | ✅ |
| OBS | GET `/api/monitoring/stats` | Статистика системы | ✅ |

### 9.1 Основные экраны GUI

| Экран | Описание | Статус |
|---|---|---|
| Login / Register | Формы входа и регистрации | ✅ |
| Dashboard | Real-time лента уведомлений (SSE) + цветовая маркировка по приоритету + бейджи источников + индикатор подключения Live/Disconnected | ✅ |
| Subscriptions | Таблица подписок + модальное окно создания/редактирования (выбор типа, JSON-параметры, вкл/выкл) | ✅ |
| Rules | Выбор подписки → таблица правил → создание/редактирование (keywords, rate limit, priority, quiet hours) | ✅ |
| History | Журнал уведомлений с фильтрацией по статусу, пагинацией, отображением ошибок доставки | ✅ |
| Monitoring | 4 счётчика (users, subscriptions, events, notifications) + System Health из Actuator + список коннекторов. Автообновление каждые 10с | ✅ |

---

## 10. Что пока НЕ доделано / требует доработки

| Пункт | Описание | Сложность |
|---|---|---|
| **Telegram-бот: UI для chat_id** | Код `TelegramDeliveryChannel` написан и работает, но в GUI нет формы для ввода Telegram chat_id пользователя. Сейчас можно задать только напрямую в БД. Нужно добавить страницу профиля | Низкая |
| **Email: настройка через UI** | Код `EmailDeliveryChannel` написан, обёрнут в `boundedElastic`. Работает при наличии SMTP-настроек в env-переменных. В GUI нет переключателя «получать Email» | Низкая |
| **Admin-роль** | Поле `role` есть в модели и JWT-токене, но разграничение прав admin/user в контроллерах не реализовано | Низкая |
| **WebSocket** | В ТЗ указан как альтернатива SSE. Не реализован — SSE полностью покрывает задачу real-time доставки | Средняя |
| **Kafka / RabbitMQ** | В ТЗ указан как опциональный. Заменён внутренним `Sinks.Many` (multicast с backpressure). Для масштабирования на несколько инстансов потребуется message broker | Высокая |
| **Redis** | В ТЗ указан как опциональный. Дедупликация и rate-limit проверки идут через PostgreSQL (R2DBC). Redis ускорит эти операции при высокой нагрузке | Средняя |
| **Тесты** | JUnit 5, StepVerifier, Testcontainers — пока не написаны | Средняя |
| **dedup_window_minutes** | Поле есть в таблице `rules` и в DTO, но активная проверка по временному окну не реализована — дедупликация работает глобально по externalId | Низкая |
| **debounce / window / buffer** | Указаны в ТЗ для агрегирования событий в пакетные уведомления. Архитектура позволяет добавить, но пока не задействованы | Средняя |
| **OpenWeather** | Опциональный источник по ТЗ. Не реализован — достаточно 2 внешних + генератор | Низкая |

---

## 11. Как запустить

### Предусловия
- Java 17+ (JDK)
- Node.js 18+
- PostgreSQL 16 (через Docker или Homebrew)

### Запуск

```bash
# 1. PostgreSQL
brew services start postgresql@16
# Или: docker-compose up -d

# 2. Создать БД (если первый запуск)
createuser -s notifyhub
createdb -O notifyhub notifyhub
psql -d notifyhub -c "ALTER USER notifyhub WITH PASSWORD 'notifyhub';"

# 3. Backend
export JAVA_HOME=$(/usr/libexec/java_home)
./gradlew bootRun

# 4. Frontend (в отдельном терминале)
cd frontend
npm install
npm run dev
```

### Открыть

**http://localhost:5173** — GUI приложения.

### Быстрый тест

1. Зарегистрироваться или войти (`demo` / `demo123` если уже создан)
2. Subscriptions → **+ New Subscription** → Event Generator → `{}` → Save
3. Перейти на Dashboard — через ~60 секунд начнут появляться уведомления в реальном времени
4. Для GitHub: Subscriptions → GitHub Releases → `{"repo":"spring-projects/spring-boot"}` → Save
5. History — полный журнал со статусами доставки

### Опционально: включить Telegram

```bash
export TELEGRAM_BOT_TOKEN=your_bot_token
export TELEGRAM_ENABLED=true
# + в БД: UPDATE users SET telegram_chat_id = 'your_chat_id' WHERE username = 'demo';
./gradlew bootRun
```

### Опционально: включить Email

```bash
export MAIL_ENABLED=true
export MAIL_HOST=smtp.gmail.com
export MAIL_USERNAME=your@gmail.com
export MAIL_PASSWORD=your_app_password
./gradlew bootRun
```
