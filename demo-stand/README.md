# Демонстрационный стенд

Небольшой интернет-магазин из двух сервисов. Его задача — давать реалистичные
трассировки для разработки и для эксперимента.

| Сервис | Порт | Что делает |
| --- | --- | --- |
| catalog-service | 8081 | каталог товаров: фиксированный набор из 24 товаров в 4 категориях |
| order-service | 8082 | заказы: создание, добавление товаров, подтверждение; название и цену товара запрашивает у каталога |

Данные хранятся в памяти и сбрасываются перезапуском сервиса.

## API

catalog-service

| Запрос | Что делает |
| --- | --- |
| `GET /api/products` | все товары |
| `GET /api/products?category=books` | товары категории: `books`, `electronics`, `home`, `sport` |
| `GET /api/products/{id}` | товар по номеру |

order-service

| Запрос | Что делает |
| --- | --- |
| `POST /api/orders?customerId=42` | создать пустой заказ покупателя |
| `GET /api/orders?customerId=42` | заказы покупателя |
| `GET /api/orders/{id}` | заказ по идентификатору |
| `POST /api/orders/{id}/items?productId=7&quantity=2` | добавить товар в заказ |
| `POST /api/orders/{id}/checkout` | подтвердить заказ |

Все входные данные передаются в пути и строке запроса, тел запросов нет:
профиль нагрузки параметризует только их.
Ошибки возвращаются в формате RFC 9457 (`application/problem+json`).

## Запуск локально

После сборки проекта (`./mvnw verify` в корне) сервисы запускаются
из двух терминалов:

```
java -jar demo-stand/services/catalog-service/target/catalog-service.jar
java -jar demo-stand/services/order-service/target/order-service.jar
```

Адрес каталога для сервиса заказов задаётся свойством `catalog.url`
(по умолчанию `http://localhost:8081`).

Пример сценария покупки:

```
curl "http://localhost:8081/api/products?category=electronics"
curl -X POST "http://localhost:8082/api/orders?customerId=42"
curl -X POST "http://localhost:8082/api/orders/{id}/items?productId=7&quantity=2"
curl -X POST "http://localhost:8082/api/orders/{id}/checkout"
curl "http://localhost:8082/api/orders/{id}"
```

## Трассировка

Оба сервиса подключают OpenTelemetry Spring Boot starter: он создаёт спаны
для входящих HTTP-запросов и для вызова каталога из сервиса заказов
и отправляет их по OTLP на `http://localhost:4318`. Другой адрес задаётся
переменной окружения `OTEL_EXPORTER_OTLP_ENDPOINT`.

Посмотреть трассы локально проще всего в Jaeger:

```
docker run --rm -p 16686:16686 -p 4318:4318 jaegertracing/jaeger:2.21.0
```

После нескольких запросов к сервисам трассы видны на http://localhost:16686.

Чтобы запросы одного пользователя можно было собрать в сессию, клиент передаёт
её идентификатор в заголовке `baggage`. Значение попадает в атрибут `session.id`
всех спанов трассы, включая вызов каталога из сервиса заказов:

```
curl -H "baggage: session.id=7c1d2a3e" "http://localhost:8081/api/products"
```
