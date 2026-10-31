# Профиль нагрузки по данным распределённой трассировки

Построение профиля нагрузки по данным распределённой трассировки и генерация
плана нагрузочного тестирования для Apache JMeter.

Выпускная квалификационная работа (бакалаврская работа), Университет «Дубна»,
направление 09.03.04 «Программная инженерия».
Тема: «Разработка системы автоматизированного построения профилей нагрузки
по данным распределённой трассировки для тестирования микросервисных
приложений».
Автор: Рудой Вячеслав. Руководитель: Тимонина Любовь Сергеевна.

## О проекте

Система принимает трассировки OpenTelemetry, группирует их в пользовательские
сессии и строит по ним профиль нагрузки: состав операций с долями,
наблюдавшиеся последовательности операций с весами, интенсивность прихода
сессий, статистики пауз между запросами и спецификации параметров. По профилю
генерируется план теста в формате `.jmx` для стандартного Apache JMeter 5.6
без установки расширений.

Профиль нагрузки — промежуточный результат, который хранится как читаемый
и редактируемый артефакт. Его можно просмотреть, исправить, положить в систему
контроля версий и объяснить.

## Чем отличается от существующих решений

Строить нагрузочные тесты по данным о реальном использовании системы
предлагают с 2014 года. WESSBAS, ContinuITy и LWS извлекают модель
нагрузки из журналов запросов и генерируют по ней сценарий. Speedscale,
proxymock и GoReplay записывают трафик и воспроизводят его как есть.

Здесь источником служат трассировки OpenTelemetry: в них видно, как запрос
проходит через сервисы и к какой сессии относится, — из журналов доступа
это приходится восстанавливать косвенно. Промежуточный результат — профиль
нагрузки, читаемый и редактируемый артефакт, а не слепок трафика: список
операций с долями, типичные цепочки действий, интенсивность и паузы. План
теста генерируется для JMeter 5.6 без плагинов и расширений; WESSBAS,
например, требует установить Markov4JMeter.

Вместо марковской цепи в профиле хранятся топ-N реально наблюдавшихся
последовательностей операций с весами: модель хуже обобщает непройденные
пути, зато сохраняет реальные цепочки действий целиком. Точность
воспроизведения не декларируется, а измеряется: генератор активности
на стенде работает по заранее известному профилю, и восстановленный из
трассировок профиль сравнивается с ним.

## Стек

- Java 21, Spring Boot 3.x, Maven (многомодульный проект)
- PostgreSQL, Flyway
- OpenTelemetry (OTLP), OpenTelemetry Collector, Jaeger
- Apache JMeter 5.6.x — целевой формат плана теста (`.jmx`)
- Freemarker — шаблоны XML-фрагментов плана теста
- Picocli — интерфейс командной строки
- JUnit 5, AssertJ, Testcontainers
- Docker Compose — демонстрационный стенд

## Структура репозитория

```
profiler/          основное приложение: приём трассировок, сессионизация,
                   построение профиля, генерация плана теста
demo-stand/        демонстрационный стенд: микросервисы и генератор
                   пользовательской активности
```

## Сборка и запуск

Требуется JDK 21. Maven устанавливать не нужно: в репозитории есть Maven
Wrapper, который скачивает нужную версию сам.

```
./mvnw verify
```

В Windows вместо `./mvnw` используется `mvnw.cmd`. Команда собирает оба модуля
и выполняет тесты. Собранное приложение запускается так:

```
java -jar profiler/target/profiler.jar --help
```

Демонстрационный стенд — микросервисы, OpenTelemetry Collector и Jaeger —
поднимается целиком через Docker Compose (подробнее в demo-stand/README.md):

```
docker compose up --build --detach --wait
```

## Использование

Профиль строится по трассировкам, собранным за заданный интервал времени
(границы указываются в формате RFC 3339):

```
java -jar profiler/target/profiler.jar build-profile \
  --from 2026-10-31T10:00:00Z --to 2026-10-31T10:05:00Z --name run-a \
  --db-url jdbc:postgresql://localhost:15432/profile \
  --db-user postgres --db-password postgres
```

Команда печатает идентификатор сохранённого профиля. По нему генерируется
план теста для JMeter:

```
java -jar profiler/target/profiler.jar generate --profile-id 1 \
  --host localhost --port 18080 --out results/plan-b \
  --db-url jdbc:postgresql://localhost:15432/profile \
  --db-user postgres --db-password postgres
```

Разница двух профилей — например, эталонного и восстановленного по
трассировкам — оценивается командой compare:

```
java -jar profiler/target/profiler.jar compare --first 1 --second 2 \
  --db-url jdbc:postgresql://localhost:15432/profile \
  --db-user postgres --db-password postgres
```

Полный экспериментальный цикл (прогон генератора активности, построение
профиля, генерация плана, нагрузочные прогоны, сравнение с эталоном)
выполняется одной командой `scripts/run-experiment.sh`. Учётные данные
выше — локальные значения демонстрационного стенда из docker-compose.yml.

## Лицензия

Код распространяется по лицензии MIT, текст — в файле LICENSE.

## Контакты

Автор: Рудой Вячеслав — https://github.com/VyacheslavRudoy

---

## English summary

**vkr-load-profile** builds a workload profile from OpenTelemetry distributed
traces and generates an executable load-test plan for Apache JMeter 5.6.

Traces are grouped into user sessions. The profile captures operation shares,
the most frequent observed operation sequences with their weights, the session
arrival rate, think-time statistics and parameter specifications (correlated
values and feeder values). The profile is a readable, editable artifact that is
stored in PostgreSQL and can be reviewed and versioned. The generated `.jmx`
plan runs on stock JMeter without plugins. Reproduction accuracy is verified
experimentally against a known reference profile.

Stack: Java 21, Spring Boot 3, PostgreSQL, Flyway, OpenTelemetry, Jaeger,
Freemarker, Picocli, JUnit 5, Testcontainers, Docker Compose.

This is a graduation thesis project in Software Engineering.
