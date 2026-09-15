# vkr-load-profile

Построение профиля нагрузки по данным распределённой трассировки и генерация
плана нагрузочного тестирования для Apache JMeter.

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

Проект выполняется как выпускная квалификационная работа по направлению
«Программная инженерия».

## Чем решение отличается от существующих

Подход «строить нагрузочный тест по данным о реальном использовании системы»
известен и развивается с 2014 года. Новизна самой идеи не заявляется.

- Академические работы — WESSBAS (Vögele и др., 2014–2016), ContinuITy
  (Schulz и др., 2019–2021), LWS (2023) — извлекают модель нагрузки
  из журналов запросов.
- Промышленные инструменты — Speedscale, proxymock, GoReplay — перехватывают
  и воспроизводят сам трафик. Промежуточная модель нагрузки не строится
  и человеку не предъявляется.

Отличия данной работы:

1. Источником служит распределённая трассировка OpenTelemetry, а не журналы
   доступа. Это даёт межсервисную структуру вызовов, сквозной контекст запроса
   и стандартизованную семантику атрибутов.
2. Профиль нагрузки — читаемый и редактируемый артефакт, а не бинарный слепок
   трафика.
3. План теста генерируется для стандартного JMeter 5.6 без расширений
   (WESSBAS требует Markov4JMeter).
4. Вместо марковской цепи хранятся топ-N реально наблюдавшихся
   последовательностей с весами. Компромисс осознанный: хуже обобщение
   на непронаблюдённые пути, лучше сохранение реальных цепочек действий.
5. Точность воспроизведения проверяется экспериментом с заранее известным
   эталонным профилем, а не декларируется.

Не заявляется как новое: сама идея построения теста по реальному трафику,
автоматическая корреляция параметров, сессионная модель нагрузки.

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
