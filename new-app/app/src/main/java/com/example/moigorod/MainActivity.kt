package com.example.moigorod

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*

/** Functional navigation shell. Exact visual styling awaits inspection of approved PNG. */
class MainActivity : Activity() {
    private val bg = Color.rgb(18, 19, 22)
    private val surface = Color.rgb(35, 36, 40)
    private val accent = Color.rgb(252, 63, 29)
    private val muted = Color.rgb(180, 183, 190)
    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()
    private lateinit var content: LinearLayout
    private var city = "Выберите город"
    private var currentPage = "home"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        city = savedInstanceState?.getString("city")
            ?: getPreferences(MODE_PRIVATE).getString("city", "Выберите город")!!
        showHome()
    }

    @Deprecated("Deprecated in Android framework")
    override fun onBackPressed() {
        if (currentPage != "home") showHome() else super.onBackPressed()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("city", city)
        super.onSaveInstanceState(outState)
    }

    private fun text(label: String, size: Float = 17f, color: Int = Color.WHITE, bold: Boolean = false) =
        TextView(this).apply {
            text = label
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
            setPadding(0, 12, 0, 12)
        }

    private fun button(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(surface))
            setOnClickListener { action() }
        }

    private fun shell(title: String) {
        val scroll = ScrollView(this).apply { setBackgroundColor(bg) }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(28))
        }
        scroll.addView(content)
        setContentView(scroll)
        content.addView(text(title, 29f, Color.WHITE, true))
    }

    private fun section(title: String, message: String) {
        content.addView(text(title, 21f, Color.WHITE, true))
        content.addView(text(message, 15f, muted))
    }

    private fun outlinedPanel(): android.graphics.drawable.GradientDrawable =
        android.graphics.drawable.GradientDrawable().apply {
            setColor(surface)
            cornerRadius = dp(18).toFloat()
            setStroke(dp(1), accent)
        }

    private fun card(title: String, description: String, action: (() -> Unit)? = null) {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = outlinedPanel()
            isClickable = action != null
            if (action != null) setOnClickListener { action() }
        }
        panel.addView(text(title, 19f, Color.WHITE, true))
        panel.addView(text(description, 14f, muted))
        content.addView(panel, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(8)
            bottomMargin = dp(5)
        })
    }

    private fun iconTile(icon: String, label: String, description: String, action: () -> Unit): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(12), dp(8), dp(12))
            background = outlinedPanel()
            addView(text(icon, 25f, Color.WHITE, true))
            addView(text(label, 14f, Color.WHITE, true))
            addView(text(description, 11f, muted))
            setOnClickListener { action() }
            isClickable = true
        }

    private fun featureStrip() {
        // Компактная сетка 2×2: все городские разделы видны сразу, без горизонтальной карусели.
        val items = listOf(
            Triple("⚡", "Свет", "Отключения") to { showPage("Свет", "Данные пока не загружены.") },
            Triple("💧", "Вода", "Водоснабжение") to { showPage("Вода", "Данные пока не загружены.") },
            Triple("⛽", "Топливо", "Наличие") to { showPage("Топливо", "Данные пока не загружены.") },
            Triple("▤", "Новости", "Городская лента") to { showPage("Новости", "Новости пока не загружены.") }
        )
        val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        items.chunked(2).forEach { pair ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            pair.forEach { item ->
                val tile = iconTile(item.first.first, item.first.second, item.first.third, item.second).apply {
                    setPadding(dp(10), dp(5), dp(8), dp(5))
                }
                row.addView(tile, LinearLayout.LayoutParams(0, dp(82), 1f).apply {
                    rightMargin = dp(6)
                    bottomMargin = dp(6)
                })
            }
            grid.addView(row, LinearLayout.LayoutParams(-1, -2))
        }
        content.addView(grid, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })
    }

    private fun header() {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val brand = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        brand.addView(text("📍 МОЙ ГОРОД", 24f, Color.WHITE, true))
        brand.addView(text("Безопасность. Информация. Помощь.", 12f, muted))
        row.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(button("🔔") {
            showPage("Уведомления", "Здесь будут отображаться уведомления приложения.")
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        row.addView(button("⚙") {
            showPage("Настройки", "Настройки приложения.")
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        content.addView(row)
    }

    private fun promoBanner() {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(12), dp(14))
            background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.rgb(45, 18, 20), surface)
            ).apply {
                cornerRadius = dp(20).toFloat()
                setStroke(dp(1), accent)
            }
        }
        val words = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        words.addView(text("БУДЬ В КУРСЕ", 13f, Color.WHITE, true))
        words.addView(text("ВАЖНЫХ СОБЫТИЙ", 20f, accent, true))
        words.addView(text("Подпишись на канал и получай новости", 12f, muted))
        words.addView(button("➤ Перейти в Telegram") {
            showPage("Реклама и объявления", "Ссылка на рекламный канал появится после настройки.")
        })
        panel.addView(words, LinearLayout.LayoutParams(0, -2, 1f))
        panel.addView(text("📱", 42f, Color.WHITE, true))
        content.addView(panel, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(14)
            bottomMargin = dp(10)
        })
    }

    private fun sosPanel() {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.rgb(48, 30, 32), Color.rgb(34, 35, 39))
            ).apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = dp(24).toFloat()
                setStroke(dp(1), Color.rgb(155, 55, 45))
            }
            setOnClickListener { showSos() }
            isClickable = true
        }
        val sosBadge = TextView(this).apply {
            text = "SOS"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = dp(9).toFloat()
                setColor(Color.rgb(171, 42, 47))
                setStroke(dp(1), Color.rgb(203, 76, 70))
            }
        }
        panel.addView(sosBadge, LinearLayout.LayoutParams(dp(48), dp(48)))
        val words = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), 0, 0, 0)
        }
        words.addView(text("SOS 112", 22f, Color.WHITE, true))
        words.addView(text("Экстренный вызов", 14f, muted))
        panel.addView(words, LinearLayout.LayoutParams(0, -2, 1f))
        panel.addView(text("›", 28f, Color.WHITE, true))
        content.addView(panel, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(8); bottomMargin = dp(10)
        })
    }
 
    private fun chatPanel() {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.rgb(43, 29, 32), Color.rgb(34, 35, 39))
            ).apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = dp(22).toFloat()
                setStroke(dp(1), Color.rgb(142, 54, 45))
            }
            setOnClickListener { showCommunityChat() }
            isClickable = true
        }
        val chatBadge = TextView(this).apply {
            text = "•••"
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(232, 218, 218))
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = dp(14).toFloat()
                setColor(Color.rgb(54, 42, 45))
                setStroke(dp(1), Color.rgb(126, 55, 49))
            }
        }
        panel.addView(chatBadge, LinearLayout.LayoutParams(dp(44), dp(44)))
        val words = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }
        words.addView(text("Онлайн-чат", 18f, Color.WHITE, true))
        words.addView(text("Общайтесь, задавайте вопросы,", 12f, muted))
        words.addView(text("получайте помощь", 12f, muted))
        val mod = TextView(this).apply {
            text = "🛡 Автомодератор"
            textSize = 11f
            setTextColor(Color.rgb(201, 125, 113))
            setPadding(0, dp(4), 0, 0)
        }
        words.addView(mod)
        panel.addView(words, LinearLayout.LayoutParams(0, -2, 1f))
        panel.addView(text("›", 24f, Color.rgb(171, 77, 65), true))
        content.addView(panel, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(8); bottomMargin = dp(8)
        })
    }

    private fun dangerPanel() {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = outlinedPanel()
            setOnClickListener { showPage("Я в опасности", "Можно будет настроить отправку SMS и геолокации после подключения соответствующих разрешений.") }
            isClickable = true
        }
        panel.addView(text("🛡️", 27f, accent, true))
        val words = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, 0, 0) }
        words.addView(text("Я в опасности", 18f, Color.WHITE, true))
        words.addView(text("Отправить SMS и геолокацию", 13f, muted))
        panel.addView(words)
        content.addView(panel, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
    }

    private fun bottomNav() {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(4))
        }
        val items = listOf(
            Triple("⌂", "Главная", { showHome() }),
            Triple("⌖", "Карта", { showPage("Карта", "Карта пока не подключена.") }),
            Triple("⚠", "Опасности", { showPage("Опасности", "Нет подтверждённых актуальных данных. Это не означает отсутствие опасности.") }),
            Triple("⋯", "Ещё", { showPage("Ещё", "Дополнительные сервисы.") })
        )
        items.forEachIndexed { index, item ->
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(2), dp(5), dp(2), dp(5))
                addView(text(item.first, 23f, if (index == 0) accent else muted, true))
                addView(text(item.second, 10f, if (index == 0) accent else muted))
                setOnClickListener { item.third() }
                isClickable = true
            }
            row.addView(col, LinearLayout.LayoutParams(0, -2, 1f))
        }
        content.addView(row, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
    }

    private fun chooseCity() {
        val cities = arrayOf(
            "Авдеевка",
            "Амвросиевка",
            "Артёмовск (Бахмут)",
            "Белицкое",
            "Белозёрское",
            "Волноваха",
            "Горловка",
            "Горняк",
            "Дебальцево",
            "Дзержинск (Торецк)",
            "Доброполье",
            "Докучаевск",
            "Донецк",
            "Дружковка",
            "Енакиево",
            "Ждановка",
            "Железное",
            "Зугрэс",
            "Иловайск",
            "Кировское (Крестовка)",
            "Комсомольское (Кальмиусское)",
            "Константиновка",
            "Краматорск",
            "Красногоровка",
            "Красноармейск (Покровск)",
            "Курахово",
            "Лиман",
            "Макеевка",
            "Мариуполь",
            "Марьинка",
            "Мирноград",
            "Моспино",
            "Николаевка",
            "Новоазовск",
            "Новогродовка",
            "Родинское",
            "Светлодарск",
            "Святогорск",
            "Северск",
            "Селидово",
            "Славянск",
            "Снежное",
            "Соледар",
            "Торез (Чистяково)",
            "Углегорск",
            "Угледар",
            "Украинск",
            "Харцызск",
            "Часов Яр",
            "Шахтёрск",
            "Ясиноватая"
        )
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 8, 24, 8)
        }
        val search = EditText(this).apply {
            hint = "Поиск города"
            isSingleLine = true
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        val results = ListView(this)
        val visible = cities.toMutableList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, visible)
        results.adapter = adapter
        layout.addView(search)
        layout.addView(results, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, (resources.displayMetrics.density * 420).toInt()))
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Выберите город")
            .setView(layout)
            .setNegativeButton("Отмена", null)
            .create()
        results.setOnItemClickListener { _, _, position, _ ->
            city = visible[position]
            getPreferences(MODE_PRIVATE).edit().putString("city", city).apply()
            dialog.dismiss()
            showHome()
        }
        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim().orEmpty()
                visible.clear()
                visible.addAll(cities.filter { it.contains(query, ignoreCase = true) })
                adapter.notifyDataSetChanged()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        dialog.show()
    }

    private fun showHome() {
        currentPage = "home"
        shell("")
        content.removeAllViews()
        header()
        promoBanner()
        val cityRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = outlinedPanel()
            setPadding(dp(14), dp(2), dp(10), dp(2))
        }
        cityRow.addView(text("📍  $city", 16f, Color.WHITE, true), LinearLayout.LayoutParams(0, -2, 1f))
        cityRow.addView(button("Изменить ▾") { chooseCity() })
        cityRow.setOnClickListener { chooseCity() }
        content.addView(cityRow, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
        sosPanel()
        content.addView(text("Городские разделы", 21f, Color.WHITE, true))
        featureStrip()
        chatPanel()
        content.addView(text("Полезное рядом  →", 21f, Color.WHITE, true).apply {
            setPadding(0, dp(12), 0, dp(2))
        })
        val nearbyScroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val nearbyRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf(
            Triple("💊", "Аптеки", "Справочник аптек"),
            Triple("🚕", "Такси", "Контакты такси"),
            Triple("📍", "Полезные адреса", "Городские службы")
        ).forEach { item ->
            nearbyRow.addView(iconTile(item.first, item.second, item.third) {
                showPage(item.second, "Сведения появятся после подключения справочника.")
            }, LinearLayout.LayoutParams(dp(150), dp(112)).apply { rightMargin = dp(8) })
        }
        nearbyScroll.addView(nearbyRow)
        content.addView(nearbyScroll)
        bottomNav()
    }

    private fun showPage(title: String, description: String) {
        currentPage = title
        shell(title)
        content.addView(text("Город: $city", 16f, muted))
        content.addView(text(description, 16f, muted))
        content.addView(button("← На главную") { showHome() })
    }

    private fun showCommunityChat() {
        currentPage = "chat"
        shell("Онлайн-чат")
        if (city == "Выберите город") {
            content.addView(text("Для входа в городской чат сначала выберите город.", 16f, muted))
            content.addView(button("Выбрать город") { chooseCity() })
        } else {
            content.addView(text("Общий чат: $city", 21f, Color.WHITE, true))
            content.addView(text("Здесь жители выбранного города смогут общаться друг с другом.", 16f, muted))
            card("Правила и модерация", "Автоматический фильтр спама, жалобы на сообщения и проверка модератором. Нарушения могут приводить к ограничению доступа.")
            content.addView(text("Отправка сообщений станет доступна после подключения сервера и авторизации. Сейчас сообщения не отправляются.", 15f, muted))
        }
        content.addView(button("← На главную") { showHome() })
    }

    private fun showSos() {
        currentPage = "sos"
        shell("Экстренная помощь")
        content.addView(text("Выберите действие. Ничего не отправляется автоматически.", 16f, muted))
        content.addView(button("Позвонить 112") {
            android.app.AlertDialog.Builder(this)
                .setTitle("Открыть набор номера 112?")
                .setMessage("Вызов не будет начат автоматически.")
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Открыть") { _, _ ->
                    startActivity(android.content.Intent(android.content.Intent.ACTION_DIAL,
                        android.net.Uri.parse("tel:112")))
                }.show()
        })
        content.addView(button("Сообщить близким") {
            android.app.AlertDialog.Builder(this)
                .setTitle("Сообщение близким")
                .setMessage("Откроется приложение сообщений. Выберите получателя и подтвердите отправку самостоятельно. Геолокация не передаётся без разрешения.")
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Продолжить") { _, _ ->
                    startActivity(android.content.Intent(android.content.Intent.ACTION_SENDTO,
                        android.net.Uri.parse("smsto:")).putExtra("sms_body", "Мне нужна помощь. Пожалуйста, свяжитесь со мной."))
                }.show()
        })
        content.addView(button("← На главную") { showHome() })
    }
}
