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

    private fun card(title: String, description: String, action: (() -> Unit)? = null) {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(surface)
                cornerRadius = dp(16).toFloat()
            }
            isClickable = action != null
            if (action != null) setOnClickListener { action() }
        }
        panel.addView(text(title, 19f, Color.WHITE, true))
        panel.addView(text(description, 14f, muted))
        content.addView(panel, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(10)
            bottomMargin = dp(6)
        })
    }

    private fun navigationTile(label: String, description: String, action: () -> Unit): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(10), dp(12))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(surface)
                cornerRadius = dp(14).toFloat()
            }
            addView(text(label, 18f, Color.WHITE, true))
            addView(text(description, 13f, muted))
            setOnClickListener { action() }
            isClickable = true
        }

    private fun navigationRow(
        first: Triple<String, String, () -> Unit>,
        second: Triple<String, String, () -> Unit>
    ) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(navigationTile(first.first, first.second, first.third),
            LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(5) })
        row.addView(navigationTile(second.first, second.second, second.third),
            LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(5) })
        content.addView(row, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
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
            singleLine = true
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
        shell("МОЙ ГОРОД")
        content.addView(button("📍 $city  ▾") { chooseCity() })
        card("Объявления", "Публикаций пока нет. Обновления появятся после подключения сервера.")
        card("Статус опасности неизвестен", "Нет подтверждённых актуальных данных. Это не означает отсутствие опасности.") {
            showPage("Опасности", "Нет подтверждённых актуальных данных.")
        }
        content.addView(text("Разделы", 21f, Color.WHITE, true))
        navigationRow(
            Triple("⚠ Опасности", "Оповещения", { showPage("Опасности", "Нет подтверждённых актуальных данных.") }),
            Triple("⚡ Свет", "Отключения", { showPage("Свет", "Информация об отключениях пока не загружена.") })
        )
        navigationRow(
            Triple("💧 Вода", "Водоснабжение", { showPage("Вода", "Информация об отключениях пока не загружена.") }),
            Triple("⛽ Топливо", "Наличие", { showPage("Топливо", "Данные о наличии топлива пока не загружены.") })
        )
        navigationRow(
            Triple("▤ Новости", "Городская лента", { showPage("Новости", "Новости пока не загружены.") }),
            Triple("☏ Чат", "Общение", { showPage("Чат", "Вход по подтверждённому номеру телефона будет доступен после подключения сервера.") })
        )
        content.addView(button("SOS · Экстренная помощь") { showSos() })
    }

    private fun showPage(title: String, description: String) {
        currentPage = title
        shell(title)
        content.addView(text("Город: $city", 16f, muted))
        content.addView(text(description, 16f, muted))
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
        content.addView(button("← На главную") { showHome() })
    }
}
