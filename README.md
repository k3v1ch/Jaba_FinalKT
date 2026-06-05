# Event Platform — Сервис заявок на мероприятия

Микросервисное приложение на Spring Boot 3.4.3 / Java 21.

## Архитектура

```
api-gateway  :8000
├── auth-service    :8080  (PostgreSQL authdb)
├── event-service   :8081  (PostgreSQL eventdb)
└── notification-service :8082

RabbitMQ :5672 (management :15672)
```

### Сервисы

| Сервис | Порт | Роль |
|---|---|---|
| api-gateway | 8000 | Единая точка входа, маршрутизация, CORS |
| auth-service | 8080 | Регистрация, JWT, 2FA (TOTP), профиль, роли |
| event-service | 8081 | Мероприятия и заявки |
| notification-service | 8082 | Отправка email через RabbitMQ |

## Запуск

```bash
cp .env.example .env
# Заполните MAIL_USERNAME и MAIL_PASSWORD в .env

# Собрать и запустить
docker-compose up --build
```

> **Требования:** Docker Desktop, JDK 21 (для локального запуска)

## API Endpoints

### Auth-service (через gateway: `http://localhost:8000`)

#### Аутентификация
| Метод | URL | Описание | Доступ |
|---|---|---|---|
| POST | `/api/auth/register` | Регистрация | Публичный |
| GET | `/api/auth/verify?token=...` | Подтверждение email | Публичный |
| POST | `/api/auth/login` | Вход (JWT) | Публичный |
| POST | `/api/auth/refresh` | Обновление токена | Публичный |
| POST | `/api/auth/logout` | Выход | USER+ |

#### 2FA (TOTP / Google Authenticator)
| Метод | URL | Описание | Доступ |
|---|---|---|---|
| POST | `/api/auth/2fa/setup` | Получить QR-код | USER+ |
| POST | `/api/auth/2fa/confirm` | Включить 2FA | USER+ |
| POST | `/api/auth/2fa/disable` | Отключить 2FA | USER+ |

#### Профиль
| Метод | URL | Описание | Доступ |
|---|---|---|---|
| GET | `/api/users/me` | Мой профиль | USER+ |
| PUT | `/api/users/me` | Обновить профиль | USER+ |
| POST | `/api/users/me/images` | Загрузить фото | USER+ |
| DELETE | `/api/users/me/images/{id}` | Удалить фото | USER+ |

#### Администрирование
| Метод | URL | Описание | Доступ |
|---|---|---|---|
| GET | `/api/admin/users` | Список пользователей | ADMIN |
| PUT | `/api/admin/users/{id}/role` | Сменить роль (требует securityCode) | ADMIN |
| GET | `/api/admin/stats` | Статистика | ADMIN |

### Event-service (через gateway: `http://localhost:8000`)

#### Мероприятия
| Метод | URL | Описание | Доступ |
|---|---|---|---|
| GET | `/api/events` | Открытые мероприятия | Публичный |
| GET | `/api/events/all` | Все мероприятия | AUTH |
| GET | `/api/events/{id}` | Мероприятие по ID | Публичный |
| POST | `/api/events` | Создать мероприятие | MODERATOR/ADMIN |
| PUT | `/api/events/{id}` | Обновить мероприятие | MODERATOR/ADMIN |
| DELETE | `/api/events/{id}` | Удалить мероприятие | ADMIN |

#### Заявки
| Метод | URL | Описание | Доступ |
|---|---|---|---|
| POST | `/api/applications` | Подать заявку | USER+ |
| GET | `/api/applications/my` | Мои заявки | USER+ |
| GET | `/api/applications/event/{id}` | Заявки на мероприятие | MODERATOR/ADMIN |
| PUT | `/api/applications/{id}/review` | Рассмотреть заявку | MODERATOR/ADMIN |
| DELETE | `/api/applications/{id}` | Отозвать заявку | USER+ |

## Примеры запросов

### Регистрация
```json
POST /api/auth/register
{
  "email": "user@example.com",
  "password": "password123",
  "name": "Иван Иванов"
}
```

### Вход
```json
POST /api/auth/login
{
  "email": "user@example.com",
  "password": "password123"
}
// Ответ: { "accessToken": "...", "refreshToken": "...", "tokenType": "Bearer" }
```

### Создание мероприятия (MODERATOR/ADMIN)
```json
POST /api/events
Authorization: Bearer <token>
{
  "title": "Spring Boot Conference 2025",
  "description": "Конференция по Spring Boot",
  "eventDate": "2025-09-01T10:00:00",
  "maxParticipants": 200,
  "type": "CONFERENCE"
}
```

### Подача заявки
```json
POST /api/applications
Authorization: Bearer <token>
{
  "eventId": 1,
  "comment": "Очень хочу участвовать!"
}
```

### Смена роли (второй уровень защиты)
```json
PUT /api/admin/users/5/role
Authorization: Bearer <admin-token>
{
  "role": "MODERATOR",
  "securityCode": "admin-secure-2025"
}
```

## Типы мероприятий

`CONFERENCE`, `MASTERCLASS`, `OLYMPIAD`, `CONTEST`, `MEETUP`, `CONSULTATION`, `OTHER`

## Тестовые учётные данные

После запуска зарегистрируйте пользователей через API. Для первого ADMIN — используйте SQL:
```sql
-- authdb
UPDATE users SET role = 'ADMIN' WHERE email = 'your@email.com';
```

## Технологии

- **Spring Boot 3.4.3**, Java 21
- **Spring Security** + JWT (JJWT 0.12.3), Stateless
- **TOTP 2FA** (totp-spring-boot-starter 1.7.1, совместим с Google Authenticator)
- **Spring Data JPA** + PostgreSQL 16
- **Spring AMQP** (RabbitMQ) — TopicExchange для нотификаций
- **@Async** + ThreadPoolTaskExecutor для отправки email
- **@EnableMethodSecurity** + @PreAuthorize для ролевого доступа
- **Docker Compose** — единый запуск всех сервисов
- **Mockito** — unit-тесты сервисного слоя
