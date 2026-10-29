# Источники контента «МОЙ ГОРОД»

Предоставлены пользователем 29.10.2026. Это реестр источников, а не заявление об уже работающем подключении.

| ID | Источник | Раздел | География/примечание |
|---|---|---|---|
| makeevka_news | https://t.me/Novosti_Makeevka | Свет, вода, местные новости | Макеевка; географию каждого поста проверять по тексту |
| fuel_dnr | https://t.me/fueldnr | Топливо | Географию определять по публикации |
| benz_dnr | https://t.me/benz_dnr | Топливо | Географию определять по публикации |
| electricity_max | https://max.ru/id6163089292_biz | Свет, графики отключений | Донецк, Макеевка; проверять доступность официального способа чтения MAX |
| voda_dnr | https://t.me/vodavdnr | Вода, подвоз, графики отключений | Донецк, Макеевка; проверять город/район/адрес |
| vesti_max | https://max.ru/vesti | Новости | Проверять доступность официального способа чтения MAX |

Контрольный пост для настройки распознавания топлива: https://t.me/fueldnr/4161 . Значение слова «триггер» необходимо определить по содержанию поста и ожидаемому действию; не запускать массовые push по одному совпадению.

## Правила обработки
1. Хранить идентификатор источника, URL публикации, время публикации, время получения, извлечённые город/район, категорию и исходный текст либо разрешённый фрагмент.
2. Дедупликация по паре источник + идентификатор публикации; обновления одного поста обрабатывать отдельно.
3. Дата графика, период действия, адреса, отмена и восстановление должны извлекаться явно. Если не определены — статус «требует проверки», без объявления об актуальном отключении.
4. Не приписывать пост всем городам по умолчанию. Если география не установлена, направлять в общую ленту либо на модерацию, но не в адресные push.
5. MAX подключать только после проверки доступного и разрешённого метода интеграции; не выдавать ссылки на каналы за рабочий API.
6. Никакие новости о топливе/воде/свете не преобразовывать в сигнал опасности. Тревоги обрабатываются отдельным контуром.
7. Для каждого пользовательского уведомления сохранять ссылку на первоисточник и время проверки.


## Verified municipal and utility source candidates (2026-10-29)

Scope: electricity and water reports for Donetsk and Makeevka. These are discovery links, **not** an implemented ingestion connection. Verify the technical and legal access method before automatic collection. Keep publication URL, source, timestamp, municipality, district/address, moderation status; only confirmed geographically relevant items enter public outages.

- Donetsk municipal administration Telegram: https://t.me/gorodDonetsk ; city head Telegram: https://t.me/kulemzin_donetsk ; city head MAX: https://max.ru/kulemzin_donetsk
- Makeevka municipal administration Telegram: https://t.me/Novosti_Makeevka ; municipal MAX: https://max.ru/makeevka_ru
- Makeevka district administration MAX channels: Kirovsky https://max.ru/id9311026128_gos ; Chervonogvardeysky https://max.ru/id9311026110_gos ; Central-City https://max.ru/id9311026093_gos ; Gornyatsky https://max.ru/id9311026103_gos ; Sovetsky https://max.ru/id9311026086_gos
- Water Donbassa (utility): official website https://vodadonbassa.ru/ ; MAX https://max.ru/vodadonbassa
- Ministry of Construction DNR: Telegram https://t.me/minstroydnr ; MAX https://max.ru/minstroydnr

Verification evidence: https://dontimes.ru/oficzialnye-resursy-municzipalnyh-obrazovanij-doneczkoj-narodnoj-respubliki-v-soczialnyh-setyah/ ; https://t.me/s/Novosti_Makeevka?after=88901 ; https://news-donetsk.ru/economy/2026/10/22/495765.html . For additional cities, use the official administration directory, verify each account before enabling.

Fuel: single shared feed for Donetsk and Makeevka only. Do not fabricate station availability, prices or queues. For other selected cities show scope notice, not city-specific fuel data.

## Shared news source

User-selected single MAX channel for the city-independent News section: https://max.ru/vesti . The Android app currently opens the channel via external link; automated post ingestion is NOT implemented. Do not invent feed items or imply a working MAX API.
