# Event Platform — Сервис заявок на мероприятия

Микросервисное приложение на Spring Boot 3.4.3 / Java 21.

## Архитектура

```
frontend (nginx, SPA)
   └── /api → api-gateway :8000
              ├── auth-service        :8080  (PostgreSQL authdb)
              ├── event-service       :8081  (PostgreSQL eventdb)
              └── notification-service :8082

RabbitMQ :5672 (management :15672)
```

Все порты сервисов, БД и RabbitMQ **доступны только внутри docker-сети** —
наружу ничего не публикуется. Единственная внешняя точка входа — frontend
(nginx), который проксирует `/api` на gateway и обычно стоит за обратным
прокси (Caddy/Nginx) с TLS.

### Сервисы

| Сервис | Порт (внутр.) | Роль |
|---|---|---|
| frontend | 80 | SPA (HTML/JS), nginx; проксирует `/api` на gateway |
| api-gateway | 8000 | Единая точка входа, маршрутизация |
| auth-service | 8080 | Регистрация, JWT, 2FA (TOTP), профиль, роли |
| event-service | 8081 | Мероприятия и заявки |
| notification-service | 8082 | Отправка email через RabbitMQ |

## Запуск

```bash
cp .env.example .env
# Заполните секреты в .env: сильные DB_PASSWORD, RABBITMQ_*, JWT_SECRET,
# ADMIN_SECURITY_CODE, а также SMTP_* для отправки писем.

# Собрать и запустить (jar-ы собираются внутри Docker, локальный Maven не нужен)
docker compose up -d --build
```

Откройте сайт через frontend (порт 80 контейнера, обычно за Caddy/Nginx с TLS).
После изменений в коде достаточно `docker compose up -d --build` —
multi-stage сборка пересоберёт нужные образы из исходников.

> **Требования:** только Docker + Docker Compose. JDK/Maven для запуска не нужны —
> сборка идёт внутри образов.
>
> ⚠️ **Безопасность:** не публикуйте порты БД/RabbitMQ на хост и не используйте
> пароли по умолчанию. `.env` не коммитится (в `.gitignore`).

## API Endpoints

### Auth-service (через gateway: `http://localhost:8000`)

#### Аутентификация
| Метод | URL | Описание | Доступ |
|---|---|---|---|
| POST | `/api/auth/register` | Регистрация | Публичный |
| GET | `/api/auth/verify-email?token=...` | Подтверждение email | Публичный |
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

## Поведение и UX

- **Уведомления о заявках.** При одобрении/отклонении заявитель получает письмо
  на email и всплывающее уведомление прямо в интерфейсе (фронт опрашивает
  `/api/applications/my`), а статус виден на странице «Мои заявки».
- **Чистая авторизация.** Отсутствующий/просроченный/невалидный токен → `401`
  (фронт автоматически разлогинивает и ведёт на вход); нехватка прав по роли → `403`.
- **Удаление мероприятия** каскадно удаляет связанные заявки в одной транзакции.
- **Валидация формы мероприятия** на клиенте — пустую дату нельзя отправить.
- **Кэширование фронта**: `index.html`/`app.js`/`tailwind.css` отдаются с
  `Cache-Control: no-cache` (ревалидация по ETag), поэтому обновления
  подхватываются сразу, без ручного сброса кэша.

## Технологии

- **Spring Boot 3.4.3**, Java 21
- **Spring Security** + JWT (JJWT 0.12.3), Stateless
- **TOTP 2FA** (totp-spring-boot-starter 1.7.1, совместим с Google Authenticator)
- **Spring Data JPA** + PostgreSQL 16
- **Spring AMQP** (RabbitMQ) — TopicExchange для нотификаций
- **@Async** + ThreadPoolTaskExecutor для отправки email
- **@EnableMethodSecurity** + @PreAuthorize для ролевого доступа
- **Frontend** — SPA на ванильном JS + Tailwind (статическая сборка), nginx
- **Docker Compose** + multi-stage сборка (jar-ы собираются внутри образов)
- **Mockito** — unit-тесты сервисного слоя
