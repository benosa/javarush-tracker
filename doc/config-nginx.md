# nginx

`config/nginx.conf` подключён к `/etc/nginx/conf.d/default.conf` сервиса nginx.
Статические файлы берутся из `resources/static`, остальные запросы идут к `app:8080`.
Проверка синтаксиса: `docker compose exec nginx nginx -t`.
После изменения конфигурации: `docker compose exec nginx nginx -s reload`.

Host, X-Forwarded-Host и X-Forwarded-Proto сохраняют внешний адрес для redirect и OAuth callback.
Само приложение не публикует порт наружу. TLS при размещении сервиса нужно настроить
на входном прокси, указав HTTPS в APP_URL и COOKIE_SECURE=true.
