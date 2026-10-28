# Переработка приложения «ДНР Сигнал»

## Цель
Удалить радио как пользовательскую функцию, сохранив существующие предупреждения об опасности БПЛА, и добавить два отдельных информационных раздела: «Электричество» и «Вода».

## Правила безопасного изменения
- Не менять AlertState, AlarmClassifier, AlarmGuard и Telegram-источник тревог без отдельного обоснования и тестов.
- Не удалять голосовое предупреждение и вибрацию, пока не проверено, что они не зависят от радиоплеера; при необходимости отделить их от аудиопотока.
- Все изменения выполнять в feature-ветке; main остаётся рабочей версией до проверки.
- Не включать push-уведомления по коммунальным данным. Они только отображаются в интерфейсе.
- Не показывать предполагаемое время восстановления как подтверждённое. Отображать источник, время публикации и статус подтверждения.

## Предлагаемая структура интерфейса
1. **Безопасность** — статус тревоги БПЛА, территория, время обновления и последние сообщения.
2. **Электричество** — аварийные и плановые отключения, населённый пункт, район/улицы, начало, ожидаемое восстановление, график и источник.
3. **Вода** — отключения водоснабжения, территория, начало, ожидаемое восстановление, график подачи и источник.

## Модель записи об отключении
- `id`: устойчивый идентификатор записи;
- `utility`: `electricity` или `water`;
- `territory`, `city`, `district`, `streets`: география;
- `status`: `planned`, `active`, `restored`, `unknown`;
- `outage_type`: `planned`, `emergency`, `unknown`;
- `starts_at`, `expected_restore_at`: необязательные даты и время;
- `schedule`: необязательные интервалы включения/отключения;
- `source_name`, `source_url`, `published_at`, `fetched_at`;
- `verification`: `official`, `local_report`, `unverified`.

## Порядок реализации
1. Получить и сохранить исходный App.kt из архива, затем зафиксировать исходную сборку.
2. Найти и изолировать радио-плеер/станции/кнопки, не затрагивая систему тревог.
3. Добавить разделы коммунальных услуг и пустые состояния, если данные ещё не подключены.
4. Подключать источники по одному, начиная с официальных; местные сообщения помечать по уровню подтверждения.
5. Собрать APK в GitHub Actions и проверить сценарии тревоги, отбоя и таймаута, а также пустые/неполные данные коммунальных услуг.

## Ограничения текущего этапа
Коммунальные данные не выдумываются и не захардкоживаются как актуальные. Пока источник для территории не подключён, раздел должен сообщать, что данные недоступны или источник ещё не подключён.

## Candidate sources reviewed (28 October 2026)

### Electricity
- Donetsk region: Donetskenergo / UZESK-DNR, current temporary outage schedule: https://www.uzesk-dnr.ru/pages/new.php/?id=469
- Luhansk region: Luganskenergo planned outages: https://luganskenergo.ru/planovye-otkljucheniya/

### Water
- Donetsk region: State Enterprise «Вода Донбасса»: https://vodadonbassa.ru/
- Luhansk region: Luganskvoda published water-supply schedules by locality: https://www.lugvoda.ru/grafik-podachi-vody/

### Ingestion rules
- Prefer the utility's own website or official government/utility publication.
- Local channels are secondary sources and must retain the original post URL, publication time, and an explicit unverified/locally reported label until corroborated.
- Some schedules may be published as images or PDFs rather than machine-readable tables. Do not silently guess addresses or time ranges; show a source link when reliable extraction is unavailable.
- These are candidate sources, not proof that a live data feed has already been implemented. The first UI build intentionally shows an empty-state message until ingestion/API integration is complete.

