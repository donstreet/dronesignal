package com.example.dronesignal

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

private val UtilityText = Color(0xFF1A1A1A)
private val UtilityBg = Color(0xFFF5F5F7)
private val UtilityRed = Color(0xFFFC3F1D)
private val UtilityRedSoft = Color(0xFFFFE9E4)
private val UtilityGreen = Color(0xFF22A65A)
private val UtilityGreenSoft = Color(0xFFE7F6EE)
private val UtilityNeutral = Color(0xFFF4F4F5)
private val UtilityMuted = Color(0xFF8C8C8C)
private val UtilityLine = Color(0xFFEDEDED)
private val UtilityBlue = Color(0xFF2587D9)
private val UtilityAmber = Color(0xFFE5A000)

/** A normalized record for a future official or local outage feed. */
data class UtilityOutage(
    val id: String,
    val utility: String,
    val territory: String,
    val city: String,
    val district: String = "",
    val streets: String = "",
    val status: String = "unknown",
    val outageType: String = "unknown",
    val startsAt: String? = null,
    val expectedRestoreAt: String? = null,
    val schedule: String? = null,
    val sourceName: String,
    val sourceUrl: String? = null,
    val publishedAt: String? = null,
    val officialSource: Boolean = false
)

@Composable
private fun UtilitySection(
    title: String,
    description: String,
    territory: String,
    onTerritoryChange: (String) -> Unit,
    cityQuery: String,
    onCityQueryChange: (String) -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val electricityDnr = "https://www.uzesk-dnr.ru/pages/new.php/?id=469"
    val electricityLnr = "https://luganskenergo.ru/planovye-otkljucheniya/"
    val waterDnr = "https://vodadonbassa.ru/"
    val waterLnr = "https://www.lugvoda.ru/grafik-podachi-vody/"
    YCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (title == "Электричество") "⚡" else "💧", fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = UtilityText)
                Text(description, fontSize = 13.sp, color = UtilityMuted)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Территория", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = UtilityText)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("Все", "ДНР", "ЛНР", "Другие").forEach { option ->
                if (territory == option) {
                    YPrimary(option, { onTerritoryChange(option) }, Modifier.weight(1f))
                } else {
                    YSecondary(option, { onTerritoryChange(option) }, Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = cityQuery,
            onValueChange = onCityQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Город или район") },
            placeholder = { Text("Например, Мариуполь") }
        )
    }

    YCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(13.dp))
                    .background(if (title == "Электричество") Color(0xFFFFF3D6) else Color(0xFFE3F2FF)),
                contentAlignment = Alignment.Center
            ) {
                Text(if (title == "Электричество") "⚡" else "💧", fontSize = 22.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Отключения и графики", fontWeight = FontWeight.SemiBold, color = UtilityText)
                Spacer(Modifier.height(4.dp))
                Text("Источник данных ещё не подключён", fontSize = 13.sp, color = UtilityMuted)
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(UtilityLine))
        Spacer(Modifier.height(12.dp))
        Text(
            "После подключения источников здесь будут отображаться адреса, плановые и аварийные отключения, время начала и ожидаемого восстановления.",
            fontSize = 13.sp,
            color = UtilityMuted
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "График и время включения будут показаны только при наличии опубликованных данных.",
            fontSize = 12.sp,
            color = UtilityMuted
        )
        Spacer(Modifier.height(12.dp))
        if (territory == "Все" || territory == "ДНР") {
            YSecondary(
                "Официальный источник ДНР",
                { uriHandler.openUri(if (title == "Электричество") electricityDnr else waterDnr) },
                Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
        }
        if (territory == "Все" || territory == "ЛНР") {
            YSecondary(
                "Официальный источник ЛНР",
                { uriHandler.openUri(if (title == "Электричество") electricityLnr else waterLnr) },
                Modifier.fillMaxWidth()
            )
        }
        if (territory == "Другие") {
            Text("Для других территорий официальный источник ещё не настроен.", fontSize = 12.sp, color = UtilityMuted)
        }
    }

    YCard {
        Text("Как читать данные", fontWeight = FontWeight.SemiBold, color = UtilityText)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Top) {
            Text("●", color = UtilityGreen, fontSize = 14.sp)
            Spacer(Modifier.width(8.dp))
            Text("Официально опубликовано — сведения коммунальной службы или администрации.", fontSize = 12.sp, color = UtilityMuted)
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Top) {
            Text("●", color = UtilityAmber, fontSize = 14.sp)
            Spacer(Modifier.width(8.dp))
            Text("Местное сообщение — источник будет указан; сведения могут требовать подтверждения.", fontSize = 12.sp, color = UtilityMuted)
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Top) {
            Text("●", color = UtilityBlue, fontSize = 14.sp)
            Spacer(Modifier.width(8.dp))
            Text("Время восстановления — прогноз, если источник не подтвердил точное время.", fontSize = 12.sp, color = UtilityMuted)
        }
    }
}

private data class AnnouncementItem(
    val id: String,
    val title: String,
    val body: String,
    val category: String,
    val imageUrl: String?,
    val actionUrl: String?,
    val actionPhone: String?,
    val publishedAt: String?
)

@Composable
private fun NewsSection() {
    val ctx = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val prefs = remember { ctx.getSharedPreferences("news_settings", 0) }
    var apiBase by remember { mutableStateOf(prefs.getString("api_base_url", "https://dnrsignal.duckdns.org").orEmpty().ifBlank { "https://dnrsignal.duckdns.org" }) }
    var draftBase by remember { mutableStateOf(apiBase) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }
    var announcements by remember { mutableStateOf(emptyList<AnnouncementItem>()) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        val base = apiBase.trim().trimEnd('/')
        if (base.isBlank()) {
            error = "Укажите публичный адрес сервера API. Адрес 127.0.0.1 на телефоне не подойдёт."
            loaded = true
            return
        }
        scope.launch {
            loading = true
            error = ""
            try {
                val result = withContext(Dispatchers.IO) {
                    val connection = (URL("$base/api/announcements").openConnection() as HttpURLConnection)
                    try {
                        connection.requestMethod = "GET"
                        connection.connectTimeout = 8000
                        connection.readTimeout = 10000
                        connection.setRequestProperty("Accept", "application/json")
                        val code = connection.responseCode
                        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                        val response = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                        if (code !in 200..299) throw IllegalStateException("Сервер вернул HTTP $code")
                        val trimmedResponse = response.trim()
                        val array = if (trimmedResponse.startsWith("[")) {
                            JSONArray(trimmedResponse)
                        } else {
                            JSONObject(trimmedResponse).optJSONArray("announcements") ?: JSONArray()
                        }
                        buildList {
                            for (i in 0 until array.length()) {
                                val item = array.optJSONObject(i) ?: continue
                                add(
                                    AnnouncementItem(
                                        id = item.optString("id", i.toString()),
                                        title = item.optString("title", "Без заголовка"),
                                        body = item.optString("body", ""),
                                        category = item.optString("category", "general"),
                                        imageUrl = item.optString("image_url").takeIf { it.isNotBlank() && it != "null" },
                                        actionUrl = item.optString("action_url").takeIf { it.isNotBlank() && it != "null" },
                                        actionPhone = item.optString("action_phone").takeIf { it.isNotBlank() && it != "null" },
                                        publishedAt = item.optString("published_at").takeIf { it.isNotBlank() && it != "null" }
                                    )
                                )
                            }
                        }
                    } finally {
                        connection.disconnect()
                    }
                }
                announcements = result
                loaded = true
            } catch (e: Exception) {
                error = e.message ?: "Не удалось загрузить объявления"
                loaded = true
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(apiBase) {
        if (apiBase.isNotBlank()) refresh()
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        YCard {
            Text("Новости и объявления", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = UtilityText)
            Spacer(Modifier.height(4.dp))
            Text("Публикации сервера: новости, вода, электричество и важные сообщения.", fontSize = 13.sp, color = UtilityMuted)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = draftBase,
                onValueChange = { draftBase = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Публичный адрес сервера API") },
                placeholder = { Text("https://ваш-домен") }
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                YPrimary(
                    "Сохранить адрес",
                    {
                        apiBase = draftBase.trim().trimEnd('/')
                        prefs.edit().putString("api_base_url", apiBase).apply()
                        refresh()
                    },
                    Modifier.weight(1f)
                )
                YSecondary(
                    if (loading) "Загрузка…" else "Обновить",
                    { if (!loading) refresh() },
                    Modifier.weight(1f)
                )
            }
            if (error.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(error, color = UtilityRed, fontSize = 13.sp)
            }
        }
        if (loading && announcements.isEmpty()) {
            YCard { Text("Загружаю публикации…", color = UtilityMuted) }
        } else if (loaded && announcements.isEmpty() && error.isBlank()) {
            YCard {
                Text("Пока нет опубликованных новостей", fontWeight = FontWeight.SemiBold, color = UtilityText)
                Spacer(Modifier.height(4.dp))
                Text("Новые публикации появятся здесь после публикации на сервере.", fontSize = 13.sp, color = UtilityMuted)
            }
        }
        announcements.forEach { item ->
            YCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.clip(RoundedCornerShape(8.dp)).background(UtilityRedSoft).padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Text(
                            when (item.category.lowercase(Locale.ROOT)) {
                                "emergency" -> "Важно"
                                "electricity" -> "Электричество"
                                "water" -> "Вода"
                                "news" -> "Новости"
                                else -> "Объявление"
                            },
                            color = UtilityRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    item.publishedAt?.let { Text(it.replace("T", " ").take(16), fontSize = 11.sp, color = UtilityMuted) }
                }
                Spacer(Modifier.height(10.dp))
                Text(item.title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = UtilityText)
                if (item.body.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(item.body, fontSize = 14.sp, color = UtilityText)
                }
                if (item.actionUrl != null || item.actionPhone != null) {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        item.actionUrl?.let { link ->
                            YSecondary("Подробнее", { runCatching { uriHandler.openUri(link) } }, Modifier.weight(1f))
                        }
                        item.actionPhone?.let { phone ->
                            YPrimary("Позвонить", {
                                runCatching { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) }
                            }, Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewAppScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var active by remember { mutableStateOf(AlertState.isActive(ctx)) }
    var since by remember { mutableStateOf(AlertState.since(ctx)) }
    var source by remember { mutableStateOf(AlertState.source(ctx)) }
    var expire by remember { mutableStateOf(AlertState.expireMinutes(ctx)) }
    var lastPost by remember { mutableStateOf("—") }
    var lastKind by remember { mutableStateOf("—") }
    var lastTime by remember { mutableStateOf("") }
    var netError by remember { mutableStateOf(false) }
    var probe by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    var section by remember { mutableStateOf("Безопасность") }
    var territory by remember { mutableStateOf("Все") }
    var cityQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            active = AlertState.isActive(ctx)
            since = AlertState.since(ctx)
            source = AlertState.source(ctx)
            val posts = withContext(Dispatchers.IO) { TelegramAlarmSource.fetchAll() }
            if (posts == null) {
                netError = true
            } else {
                netError = false
                posts.maxByOrNull { it.id }?.let {
                    lastPost = it.text.take(160)
                    lastTime = if (it.timeMs > 0) SimpleDateFormat("HH:mm", Locale("ru")).format(Date(it.timeMs)) else ""
                    lastKind = when (AlarmClassifier.classify(it.text).kind) {
                        PostKind.ALARM -> "Тревога"
                        PostKind.CLEAR -> "Отбой"
                        PostKind.IGNORE -> "Обычный пост"
                    }
                }
            }
            delay(15_000)
        }
    }

    Surface(Modifier.fillMaxSize(), color = UtilityBg) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                    Text("ДНР Сигнал", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = UtilityText)
                    Text("Безопасность и коммунальная информация", fontSize = 13.sp, color = UtilityMuted)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("Безопасность", "Электричество", "Вода", "Новости").forEach { tab ->
                        if (section == tab) YPrimary(tab, { section = tab }, Modifier.weight(1f))
                        else YSecondary(tab, { section = tab }, Modifier.weight(1f))
                    }
                }
            }

            if (section == "Безопасность") {
                item {
                    val mins = ((System.currentTimeMillis() - since) / 60_000).coerceAtLeast(0)
                    StatusHero(
                        active,
                        if (active) "Опасность БПЛА" else "Всё спокойно",
                        if (active) "$mins мин · источник $source" else "Ожидание новых сообщений канала тревог"
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (active) {
                            YDanger("Заглушить", {
                                AlertState.stop(ctx, "вручную")
                                active = false
                                AlertVoiceService.stop(ctx)
                            }, Modifier.weight(1f))
                        } else {
                            YPrimary("Включить слежение", {
                                FastPollService.start(ctx)
                                AlertCheckWorker.schedule(ctx)
                            }, Modifier.weight(1f))
                        }
                        YSecondary(if (active) "Сброс" else "Тест голоса", {
                            if (active) {
                                AlertState.stop(ctx, "вручную")
                                active = false
                                AlertVoiceService.stop(ctx)
                            } else {
                                AlertState.start(ctx, "вручную")
                                active = true
                                AlertVoiceService.start(ctx, "Проверка голосового предупреждения об опасности БПЛА.")
                            }
                        }, Modifier.weight(1f))
                    }
                }
                item {
                    YCard {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Канал тревог", fontWeight = FontWeight.SemiBold, color = UtilityText)
                            Text("t.me/" + TelegramAlarmSource.CHANNEL, fontSize = 12.sp, color = UtilityMuted)
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val tagPair = when (lastKind) {
                                "Тревога" -> UtilityRed to UtilityRedSoft
                                "Отбой" -> UtilityGreen to UtilityGreenSoft
                                else -> UtilityMuted to UtilityNeutral
                            }
                            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(tagPair.second).padding(horizontal = 10.dp, vertical = 5.dp)) {
                                Text(lastKind, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = tagPair.first)
                            }
                            Spacer(Modifier.width(8.dp))
                            if (lastTime.isNotBlank()) Text(lastTime, fontSize = 12.sp, color = UtilityMuted)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(lastPost, fontSize = 14.sp, color = UtilityText)
                        if (netError) {
                            Spacer(Modifier.height(8.dp))
                            Text("Канал недоступен · повтор при следующем опросе", fontSize = 12.sp, color = UtilityRed)
                        }
                    }
                }
                item {
                    YCard {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Настройки", fontWeight = FontWeight.SemiBold, color = UtilityText)
                            Text(
                                if (showSettings) "Свернуть" else "Развернуть",
                                fontSize = 13.sp,
                                color = UtilityRed,
                                modifier = Modifier.clickableNoRipple { showSettings = !showSettings }
                            )
                        }
                        AnimatedVisibility(visible = showSettings) {
                            Column {
                                Spacer(Modifier.height(8.dp))
                                Box(Modifier.fillMaxWidth().height(1.dp).background(UtilityLine))
                                Spacer(Modifier.height(12.dp))
                                Text("Автоснятие через $expire мин", fontSize = 14.sp, color = UtilityText)
                                Slider(
                                    value = expire.toFloat(),
                                    onValueChange = { expire = it.toInt() },
                                    onValueChangeFinished = { AlertState.setExpireMinutes(ctx, expire) },
                                    valueRange = 10f..180f,
                                    steps = 33,
                                    colors = SliderDefaults.colors(thumbColor = UtilityRed, activeTrackColor = UtilityRed)
                                )
                                Text("Тревога снимается, если за это время не было подтверждения", fontSize = 12.sp, color = UtilityMuted)
                                Spacer(Modifier.height(12.dp))
                                YSecondary("Замерить историю канала", {
                                    scope.launch {
                                        probe = "Считаю…"
                                        probe = withContext(Dispatchers.IO) { HistoryProbe.analyze(10) }
                                    }
                                }, Modifier.fillMaxWidth())
                                if (probe.isNotBlank()) {
                                    Spacer(Modifier.height(10.dp))
                                    Text(probe, fontSize = 12.sp, color = UtilityMuted)
                                }
                            }
                        }
                    }
                }
            } else if (section == "Электричество") {
                item {
                    UtilitySection(
                        "Электричество",
                        "Отключения света, графики и ожидаемое восстановление",
                        territory,
                        { territory = it },
                        cityQuery,
                        { cityQuery = it }
                    )
                }
            } else if (section == "Вода") {
                item {
                    UtilitySection(
                        "Вода",
                        "Отключения водоснабжения и графики подачи воды",
                        territory,
                        { territory = it },
                        cityQuery,
                        { cityQuery = it }
                    )
                }
            } else {
                item { NewsSection() }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}
