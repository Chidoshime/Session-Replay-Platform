# Session Replay Platform

Платформа для управления и анализа пользовательских сессий (Session Replay) на стеке Java + Spring, React, PostgreSQL, RabbitMQ.

## Архитектура

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│   Frontend      │────▶│   Backend        │────▶│   RabbitMQ      │
│   (React SDK)   │     │   (Spring Boot)  │     │   (Queue)       │
└─────────────────┘     └──────────────────┘     └─────────────────┘
                               │                        │
                               ▼                        ▼
                        ┌──────────────────┐     ┌─────────────────┐
                        │   PostgreSQL     │◀────│   Listener      │
                        │   (Sessions &    │     │   (Processor)   │
                        │    Events)       │     └─────────────────┘
                        └──────────────────┘
```

## Компоненты

### 1. Backend (Java + Spring Boot)

**Расположение:** `/backend`

#### Технологии:
- Spring Boot 3.2
- Spring Data JPA
- Spring AMQP (RabbitMQ)
- PostgreSQL с JSONB
- Lombok

#### Структура:
```
backend/
├── src/main/java/com/sessionreplay/
│   ├── SessionReplayApplication.java    # Точка входа
│   ├── config/
│   │   └── RabbitMQConfig.java          # Конфигурация RabbitMQ
│   ├── controller/
│   │   └── SessionController.java       # REST API endpoints
│   ├── model/
│   │   ├── Session.java                 # Entity сессии
│   │   └── SessionEvent.java            # Entity события
│   ├── repository/
│   │   ├── SessionRepository.java       # Репозиторий сессий
│   │   └── SessionEventRepository.java  # Репозиторий событий
│   ├── service/
│   │   ├── SessionService.java          # Бизнес-логика сессий
│   │   └── SessionEventListener.java    # Обработчик из RabbitMQ
│   └── event/
│       └── SessionEventBatch.java       # DTO для пакетов событий
└── src/main/resources/
    └── application.yml                  # Конфигурация
```

#### API Endpoints:

| Метод | Путь | Описание |
|-------|------|----------|
| POST | `/api/v1/sessions/events` | Принять пакет событий сессии |
| GET | `/api/v1/sessions/{sessionId}` | Получить информацию о сессии |
| GET | `/api/v1/sessions/{sessionId}/events` | Получить все события сессии |
| POST | `/api/v1/sessions/{sessionId}/end` | Завершить сессию |

#### Конфигурация (application.yml):
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/session_replay_db
    username: ems-load-tests
    password: 66778899
  
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

### 2. Frontend SDK (TypeScript)

**Расположение:** `/frontend/sdk`

#### Технологии:
- TypeScript
- rrweb (для записи DOM событий)
- React (хук useSessionReplay)

#### Установка:
```bash
cd frontend/sdk
npm install
npm run build
```

#### Использование в React:

```tsx
import { useSessionReplay } from '@sessionreplay/sdk';

function App() {
  const { sessionId, addCustomEvent } = useSessionReplay({
    backendUrl: 'http://localhost:8080',
    autoStart: true,
    maskAllInputs: true,
    bufferSize: 100,
    flushIntervalMs: 5000,
  });

  return (
    <div>
      <h1>Session ID: {sessionId}</h1>
      <button onClick={() => addCustomEvent('click', { button: 'test' })}>
        Track Click
      </button>
    </div>
  );
}
```

#### Прямое использование:

```ts
import { init } from '@sessionreplay/sdk';

const recorder = init({
  backendUrl: 'http://localhost:8080',
  apiKey: 'your-api-key', // опционально
  bufferSize: 100,
  flushIntervalMs: 5000,
  maskAllInputs: true,
});

recorder.start();

// Добавление кастомного события
recorder.addCustomEvent('purchase', { amount: 100 });

// Остановка записи
recorder.stop();
```

### 3. Frontend Player (TypeScript + React)

**Расположение:** `/frontend/player`

#### Технологии:
- TypeScript
- React
- rrweb-player (для воспроизведения)

#### Установка:
```bash
cd frontend/player
npm install
npm run build
```

#### Использование:

```tsx
import { SessionPlayer } from '@sessionreplay/player';

function SessionViewer() {
  return (
    <SessionPlayer
      backendUrl="http://localhost:8080"
      sessionId="sess_1234567890_abc"
      width={800}
      height={600}
      autoPlay={true}
      speed={1.5}
    />
  );
}
```

## Развертывание

### 1. Подготовка базы данных PostgreSQL

```sql
CREATE DATABASE session_replay_db;
-- База данных уже создана с пользователем ems-load-tests
-- Таблицы автоматически создадутся при первом запуске приложения благодаря ddl-auto: update
```

### 2. Настройка RabbitMQ

Убедитесь, что RabbitMQ запущен:
```bash
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```

### 3. Запуск Backend

```bash
cd backend
./mvnw spring-boot:run
```

Или соберите JAR:
```bash
./mvnw clean package
java -jar target/session-replay-backend-1.0.0-SNAPSHOT.jar
```

### 4. Интеграция в ваш проект

1. Скопируйте SDK в ваш проект или опубликуйте в npm registry
2. Добавьте зависимость в package.json
3. Инициализируйте SDK в корневом компоненте приложения

## Конфигурация безопасности

### Маскирование чувствительных данных

SDK автоматически маскирует:
- Поля ввода с type="password"
- Email поля
- Телефонные номера
- Номера кредитных карт

Для блокировки записи определенных элементов добавьте класс `sr-block`:
```html
<div class="sr-block">Этот контент не будет записан</div>
```

Для игнорирования элементов (не видны в записи, но влияют на layout):
```html
<div class="sr-ignore">Этот элемент будет пропущен</div>
```

## Мониторинг и логи

Логи приложения выводятся в консоль и могут быть собраны через:
- Logback с отправкой в ELK Stack (Kibana уже есть в вашем стеке)
- Настройте паттерн логирования в application.yml

Пример конфигурации для Kibana:
```yaml
logging:
  level:
    com.sessionreplay: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

## Производительность

### Оптимизация SDK:
- Буферизация событий (по умолчанию 100 событий)
- Периодическая отправка (по умолчанию каждые 5 секунд)
- Отправка при уходе со страницы (beforeunload)
- Сжатие данных через JSON

### Оптимизация Backend:
- Асинхронная обработка через RabbitMQ
- Пакетная запись в БД
- Индексы на sessionId и timestamp
- JSONB для гибкого хранения событий

## Расширение функциональности

### Возможные улучшения:
1. **Сжатие событий** - использовать gzip при отправке
2. **Выборка сессий** - записывать только N% сессий
3. **Триггеры записи** - начинать запись по определенным событиям
4. **Аналитика** - дашборды в Kibana по метаданным сессий
5. **Поиск по сессиям** - полнотекстовый поиск по URL, событиям
6. **Экспорт сессий** - выгрузка в формате HAR или видео

## Лицензия

MIT
