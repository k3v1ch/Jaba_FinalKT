# Event Platform — E2E тесты (JUnit 5)

Чёрноящичные интеграционные тесты, которые бьют по **запущенному** API платформы
и проверяют сквозные сценарии:

| Класс | Что проверяет |
|---|---|
| `AuthTests` | регистрация, валидация, подтверждение email, логин (верный/неверный пароль), профиль с токеном и без, refresh, логин админа |
| `EmailTests` | регистрация формирует токен письма верификации; переход по ссылке активирует аккаунт |
| `EventTests` | создание мероприятия админом, доступ без токена, список/получение/обновление, валидация даты |
| `ApplicationTests` | подача заявки участником, повторная заявка, приём админом (APPROVED), счётчик `approvedCount`, отмена своей заявки |
| `AdminTests` | список пользователей (админ 200 / пользователь 403), статистика, смена роли с кодом безопасности |

> Реальная доставка письма на SMTP (Beget) подтверждается в логах `auth-service`
> строкой `EMAIL: sent to ...`. Тесты проверяют функциональную цепочку письма
> (токен верификации + активация аккаунта) через БД `authdb`.

## Требования

- Запущенный стек (`docker compose up -d` в корне проекта).
- `authdb` доступна тесту (по умолчанию `localhost:5432`, порт опубликован в compose).
  Если БД недоступна — шаги, зависящие от верификации email, помечаются `SKIP`.
- Сид-админ `admin@event.com` (создаётся `DataSeeder` при старте auth-service,
  пароль задаётся через `SEED_ADMIN_PASSWORD` в `.env`; передайте его тестам
  через `-Dadmin.password=...`).

## Запуск

### Если есть локальный Maven + JDK 21
```bash
cd e2e-tests

mvn test                              # все группы
mvn test -Dtest=AuthTests             # только авторизация/регистрация
mvn test -Dtest=EmailTests            # письмо
mvn test -Dtest=EventTests            # мероприятия
mvn test -Dtest=ApplicationTests      # заявки на участие
mvn test -Dtest=AdminTests            # админ-функции

# один метод:
mvn test -Dtest=ApplicationTests#approvedCount
```

### На сервере без локального JDK/Maven (через Docker)
```bash
docker run --rm --network host \
  -v /root/Jaba_FinalKT:/build -w /build/e2e-tests \
  -v jaba_m2_cache:/root/.m2 \
  maven:3.9-eclipse-temurin-21 \
  mvn test                            # добавьте -Dtest=AuthTests для одной группы
```
`--network host` нужен, чтобы из контейнера были видны `localhost:5432` (authdb)
и публичный домен.

## Параметры (-D)

| Свойство | По умолчанию | Назначение |
|---|---|---|
| `base.url` | `https://event.vernovpn.com` | базовый URL API (можно `http://localhost:8000` — напрямую в gateway) |
| `admin.email` / `admin.password` | `admin@event.com` / — | учётка админа (пароль = `SEED_ADMIN_PASSWORD` из `.env`) |
| `admin.security.code` | — | код безопасности для смены ролей (= `ADMIN_SECURITY_CODE` из `.env`) |
| `db.url` / `db.user` / `db.pass` | `jdbc:postgresql://localhost:5432/authdb` / `postgres` / `postgres` | доступ к authdb для верификации email |

Пример прогона напрямую через gateway без TLS:
```bash
mvn test -Dbase.url=http://localhost:8000
```
