# Контейнеры

Скопировать `.env.example` в `.env`, задать DB_PASSWORD и APP_URL=http://localhost:8081.
`docker compose up --build -d --wait` запускает три сервиса. Swagger: http://localhost:8081/doc.
База не публикует порт наружу; приложение доступно через nginx. Healthcheck базы
проверяется перед стартом приложения, healthcheck приложения — перед стартом nginx.
`docker compose logs app` показывает ошибки запуска. `docker compose down` сохраняет тома.
`docker compose down -v` удаляет данные и вложения; это подходит только для одноразового окружения.

Образ собирается с выполнением тестов на H2. Runtime использует непривилегированного пользователя.
Каталоги `resources/view` и `resources/mails` нужны работающему приложению, поэтому тоже входят в образ.
Файлы `.env` и `.git` исключены из build context.
