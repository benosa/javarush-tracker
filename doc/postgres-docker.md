# PostgreSQL

Для обычного запуска используется сервис `db` из `compose.yaml`.
Параметры берутся из `.env`: DB_USER и DB_PASSWORD, база — jira.
Данные сохраняются в именованном томе. Пароль в репозиторий не записывается.

Тестовая PostgreSQL больше не нужна: `mvn test` использует H2 в памяти.
Для проверки PostgreSQL-миграций: `docker compose up --build -d --wait`.
Подключение к БД внутри контейнера:

```sh
docker compose exec db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```
