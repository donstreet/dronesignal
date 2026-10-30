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
    private lateinit var root: LinearLayout
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
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }
        val scroll = ScrollView(this).apply {
            setBackgroundColor(bg)
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
        }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(8))
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        // Keep the whole layout, especially bottom navigation, above Android's system bars.
        root.setOnApplyWindowInsetsListener { view, insets ->
            var topInset = 0
            var bottomInset = 0
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                topInset = insets.getInsets(android.view.WindowInsets.Type.statusBars()).top
                bottomInset = insets.getInsets(android.view.WindowInsets.Type.navigationBars()).bottom
            } else {
                @Suppress("DEPRECATION")
                run {
                    topInset = insets.systemWindowInsetTop
                    bottomInset = insets.systemWindowInsetBottom
                }
            }
            // Keep the whole app below the status bar and above the navigation bar.
            view.setPadding(view.paddingLeft, topInset, view.paddingRight, bottomInset)
            insets
        }
        root.requestApplyInsets()
        if (title.isNotBlank()) content.addView(text(title, 29f, Color.WHITE, true))
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


    /** Reference-style illustrated badges instead of platform emoji glyphs. */
    private fun iconGraphic(key: String, sizeDp: Int): View = object : View(this) {
        private val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: android.graphics.Canvas) {
            super.onDraw(canvas)
            val scale = resources.displayMetrics.density
            val w = width.toFloat()
            val h = height.toFloat()
            val cx = w / 2f
            val cy = h / 2f
            val r = minOf(w, h) * 0.43f
            val bgColor = when (key) {
                "⚡" -> Color.rgb(166, 112, 28)
                "💧" -> Color.rgb(23, 111, 165)
                "⛽" -> Color.rgb(41, 125, 48)
                "▤" -> Color.rgb(112, 58, 165)
                "⚕" -> Color.rgb(20, 157, 105)
                "🚕" -> Color.rgb(205, 139, 20)
                "⌖" -> Color.rgb(26, 116, 211)
                "chat" -> Color.rgb(244, 42, 76)
                "city", "pin" -> Color.rgb(240, 24, 47)
                else -> surface
            }
            p.style = android.graphics.Paint.Style.FILL
            p.color = bgColor
            if (key == "city" || key == "pin") {
                val pinPath = android.graphics.Path().apply {
                    moveTo(cx, h * 0.96f)
                    cubicTo(w * 0.40f, h * 0.70f, w * 0.08f, h * 0.53f, w * 0.08f, h * 0.34f)
                    cubicTo(w * 0.08f, h * 0.10f, w * 0.30f, h * 0.02f, cx, h * 0.02f)
                    cubicTo(w * 0.70f, h * 0.02f, w * 0.92f, h * 0.10f, w * 0.92f, h * 0.34f)
                    cubicTo(w * 0.92f, h * 0.53f, w * 0.60f, h * 0.70f, cx, h * 0.96f)
                    close()
                }
                canvas.drawPath(pinPath, p)
                p.color = Color.WHITE
                if (key == "city") {
                    canvas.drawRect(w*.25f,h*.34f,w*.38f,h*.60f,p)
                    canvas.drawRect(w*.42f,h*.25f,w*.57f,h*.60f,p)
                    canvas.drawRect(w*.61f,h*.38f,w*.74f,h*.60f,p)
                    p.color = bgColor
                    canvas.drawRect(w*.29f,h*.40f,w*.33f,h*.45f,p)
                    canvas.drawRect(w*.46f,h*.32f,w*.50f,h*.37f,p)
                    canvas.drawRect(w*.65f,h*.43f,w*.69f,h*.48f,p)
                } else {
                    canvas.drawCircle(cx, h * 0.34f, minOf(w, h) * 0.14f, p)
                }
                return
            }
            canvas.drawCircle(cx, cy, r, p)
            p.color = Color.WHITE
            p.style = android.graphics.Paint.Style.STROKE
            p.strokeWidth = maxOf(1.6f * scale, sizeDp * scale * 0.055f)
            p.strokeCap = android.graphics.Paint.Cap.ROUND
            p.strokeJoin = android.graphics.Paint.Join.ROUND
            when (key) {
                "⚡" -> {
                    p.style = android.graphics.Paint.Style.FILL
                    val path = android.graphics.Path().apply {
                        moveTo(cx+w*.02f,cy-r*.65f); lineTo(cx-r*.38f,cy+w*.02f)
                        lineTo(cx-r*.02f,cy+w*.02f); lineTo(cx-r*.15f,cy+r*.60f)
                        lineTo(cx+r*.40f,cy-r*.12f); lineTo(cx+r*.06f,cy-r*.12f); close()
                    }
                    canvas.drawPath(path,p)
                }
                "💧" -> {
                    val path = android.graphics.Path().apply {
                        moveTo(cx,cy-r*.72f); cubicTo(cx-r*.18f,cy-r*.35f,cx-r*.62f,cy+r*.05f,cx-r*.50f,cy+r*.35f)
                        cubicTo(cx-r*.35f,cy+r*.78f,cx+r*.35f,cy+r*.78f,cx+r*.50f,cy+r*.35f)
                        cubicTo(cx+r*.62f,cy+r*.05f,cx+r*.18f,cy-r*.35f,cx,cy-r*.72f); close()
                    }
                    p.style = android.graphics.Paint.Style.FILL; canvas.drawPath(path,p)
                }
                "⛽" -> {
                    canvas.drawRoundRect(cx-r*.42f,cy-r*.42f,cx+r*.20f,cy+r*.48f,r*.08f,r*.08f,p)
                    canvas.drawRect(cx-r*.30f,cy-r*.30f,cx+r*.08f,cy-r*.02f,p)
                    canvas.drawLine(cx+r*.20f,cy-r*.28f,cx+r*.43f,cy-r*.10f,p)
                    canvas.drawLine(cx+r*.43f,cy-r*.10f,cx+r*.43f,cy+r*.22f,p)
                }
                "▤" -> {
                    for (i in 0..2) canvas.drawLine(cx-r*.48f,cy-r*.35f+i*r*.35f,cx+r*.48f,cy-r*.35f+i*r*.35f,p)
                    canvas.drawRect(cx-r*.48f,cy-r*.48f,cx+r*.48f,cy+r*.48f,p)
                }
                "⚕" -> {
                    p.strokeWidth = r*.30f
                    canvas.drawLine(cx-r*.42f,cy,cx+r*.42f,cy,p)
                    canvas.drawLine(cx,cy-r*.42f,cx,cy+r*.42f,p)
                }
                "🚕" -> {
                    canvas.drawRoundRect(cx-r*.52f,cy-r*.05f,cx+r*.52f,cy+r*.35f,r*.12f,r*.12f,p)
                    canvas.drawLine(cx-r*.30f,cy-r*.05f,cx-r*.12f,cy-r*.32f,p)
                    canvas.drawLine(cx-r*.12f,cy-r*.32f,cx+r*.24f,cy-r*.32f,p)
                    canvas.drawCircle(cx-r*.28f,cy+r*.38f,r*.10f,p); canvas.drawCircle(cx+r*.30f,cy+r*.38f,r*.10f,p)
                }
                "⌖" -> {
                    canvas.drawCircle(cx,cy,r*.43f,p)
                    canvas.drawCircle(cx,cy,r*.12f,p)
                    canvas.drawLine(cx-r*.72f,cy,cx+r*.72f,cy,p)
                    canvas.drawLine(cx,cy-r*.72f,cx,cy+r*.72f,p)
                }
                else -> {
                    p.style = android.graphics.Paint.Style.FILL
                    p.textAlign = android.graphics.Paint.Align.CENTER
                    p.textSize = r*1.15f
                    canvas.drawText(if (key == "chat") "•••" else key, cx, cy+r*.36f, p)
                }
            }
        }
    }.apply { contentDescription = when(key) {
        "⚡" -> "Свет"; "💧" -> "Вода"; "⛽" -> "Топливо"; "▤" -> "Новости"
        "⚕" -> "Аптеки"; "🚕" -> "Такси"; "⌖" -> "Полезные адреса"; "city" -> "Мой город"; "chat" -> "Онлайн-чат"
        "pin" -> "Выбор города"
        else -> key
    } }

    private fun iconTile(icon: String, label: String, description: String, compact: Boolean = false, action: () -> Unit): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(if (compact) 8 else 10), dp(if (compact) 5 else 8), dp(if (compact) 6 else 8), dp(if (compact) 5 else 8))
            background = outlinedPanel()
            fun cleanLine(value: String, size: Float, color: Int, bold: Boolean = false) =
                TextView(this@MainActivity).apply {
                    text = value
                    textSize = size
                    setTextColor(color)
                    if (bold) setTypeface(null, Typeface.BOLD)
                    setPadding(0, 0, 0, 0)
                    includeFontPadding = false
                    gravity = Gravity.CENTER_VERTICAL
                }
            val iconSize = if (compact) 28 else 32
            addView(iconGraphic(icon, iconSize), LinearLayout.LayoutParams(dp(iconSize + 2), dp(iconSize + 2)).apply { bottomMargin = dp(2) })
            addView(cleanLine(label, if (compact) 12.5f else 13f, Color.WHITE, true).apply {
                maxLines = 2
                ellipsize = null
                gravity = Gravity.CENTER_VERTICAL
            }, LinearLayout.LayoutParams(-1, -2).apply { minimumHeight = dp(if (compact) 27 else 30) })
            if (description.isNotBlank()) {
                addView(cleanLine(description, 11f, muted), LinearLayout.LayoutParams(-1, dp(20)))
            }
            setOnClickListener { action() }
            isClickable = true
        }

    private fun featureStrip() {
        // Все четыре городских раздела компактно в один ряд.
        val items = listOf(
            Triple("⚡", "Свет", "Отключения"),
            Triple("💧", "Вода", "Водоснабжение"),
            Triple("⛽", "Топливо", "Наличие"),
            Triple("▤", "Новости", "Городская лента")
        )
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        items.forEach { item ->
            val tile = iconTile(item.first, item.second, "") {
                if (item.second == "Свет") { showElectricity() } else if (item.second == "Вода") { showWater() } else if (item.second == "Топливо") { showFuel() } else if (item.second == "Новости") { showNews() } else showPage(item.second, when (item.second) {
                    "Свет" -> "Данные об отключениях пока не загружены."
                    "Вода" -> "Данные о водоснабжении пока не загружены."
                    "Топливо" -> "Данные о наличии топлива пока не загружены."
                    else -> "Новости пока не загружены."
                })
            }.apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(8), dp(7), dp(5), dp(7))
            }
            row.addView(tile, LinearLayout.LayoutParams(0, dp(72), 1f).apply {
                rightMargin = dp(5)
            })
        }
        content.addView(row, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })
    }

    private fun header() {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val cityBadge = TextView(this).apply {
            text = "🏙"
            textSize = 27f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = dp(12).toFloat()
                setColor(Color.rgb(35, 36, 40))
                setStroke(dp(2), accent)
            }
        }
        brand.addView(iconGraphic("city", 44), LinearLayout.LayoutParams(dp(52), dp(52)).apply {
            rightMargin = dp(10)
        })
        val brandWords = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        brandWords.addView(TextView(this).apply {
            text = "МОЙ ГОРОД"
            textSize = 21f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 0)
        })
        brandWords.addView(TextView(this).apply {
            text = "Безопасность. Информация. Помощь."
            textSize = 10f
            setTextColor(muted)
            setPadding(0, 0, 0, 0)
        })
        brand.addView(brandWords, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        val bell = object : View(this) {
            private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = dp(2).toFloat()
                strokeCap = android.graphics.Paint.Cap.ROUND
                strokeJoin = android.graphics.Paint.Join.ROUND
            }
            override fun onDraw(canvas: android.graphics.Canvas) {
                super.onDraw(canvas)
                val cx = width / 2f
                val cy = height / 2f
                val scale = dp(1).toFloat()
                val path = android.graphics.Path().apply {
                    moveTo(cx - 10 * scale, cy + 5 * scale)
                    quadTo(cx - 7 * scale, cy + 1 * scale, cx - 7 * scale, cy - 5 * scale)
                    quadTo(cx - 7 * scale, cy - 13 * scale, cx, cy - 13 * scale)
                    quadTo(cx + 7 * scale, cy - 13 * scale, cx + 7 * scale, cy - 5 * scale)
                    quadTo(cx + 7 * scale, cy + 1 * scale, cx + 10 * scale, cy + 5 * scale)
                    close()
                }
                canvas.drawPath(path, paint)
                canvas.drawLine(cx - 11 * scale, cy + 6 * scale, cx + 11 * scale, cy + 6 * scale, paint)
                canvas.drawArc(cx - 3 * scale, cy + 7 * scale, cx + 3 * scale, cy + 12 * scale, 0f, 180f, false, paint)
                // Red unread indicator, matching the approved reference.
                paint.style = android.graphics.Paint.Style.FILL
                paint.color = accent
                canvas.drawCircle(cx + dp(13).toFloat(), cy - dp(12).toFloat(), dp(5).toFloat(), paint)
            }
        }.apply {
            contentDescription = "Уведомления"
            setOnClickListener { showPage("Уведомления", "Здесь будут отображаться уведомления приложения.") }
        }
        row.addView(bell, LinearLayout.LayoutParams(dp(48), dp(48)).apply {
            rightMargin = dp(4)
        })
        val settings = object : View(this) {
            private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = dp(2).toFloat()
                strokeCap = android.graphics.Paint.Cap.ROUND
                strokeJoin = android.graphics.Paint.Join.ROUND
            }
            override fun onDraw(canvas: android.graphics.Canvas) {
                super.onDraw(canvas)
                val cx = width / 2f
                val cy = height / 2f
                val outer = dp(12).toFloat()
                val inner = dp(8).toFloat()
                val teeth = android.graphics.Path()
                for (i in 0 until 24) {
                    val a = Math.toRadians(i * 15.0)
                    val radius = when (i % 3) { 0 -> outer; 1 -> outer; else -> inner }
                    val x = cx + kotlin.math.cos(a).toFloat() * radius
                    val y = cy + kotlin.math.sin(a).toFloat() * radius
                    if (i == 0) teeth.moveTo(x, y) else teeth.lineTo(x, y)
                }
                teeth.close()
                canvas.drawPath(teeth, paint)
                canvas.drawCircle(cx, cy, dp(5).toFloat(), paint)
            }
        }.apply {
            contentDescription = "Настройки"
            isClickable = true
            isFocusable = true
            setOnClickListener { showSettings() }
        }
        row.addView(settings, LinearLayout.LayoutParams(dp(48), dp(48)))
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
        words.addView(text("Частные объявления, услуги и реклама жителей", 12f, muted))
        words.addView(button("➤ Частные объявления и реклама") {
            showAnnouncements()
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
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(232, 218, 218))
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = dp(12).toFloat()
                setColor(Color.rgb(54, 42, 45))
                setStroke(dp(1), Color.rgb(126, 55, 49))
            }
        }
        panel.addView(chatBadge, LinearLayout.LayoutParams(dp(44), dp(44)))
        val words = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }
        words.addView(text("Онлайн-чат", 18f, Color.WHITE, true))
        words.addView(text("Вопросы, общение и помощь", 12f, muted))
        words.addView(TextView(this).apply {
            text = "🛡 Автомодератор"
            textSize = 10f
            setTextColor(Color.rgb(201, 125, 113))
            setPadding(0, 0, 0, 0)
        })
        panel.addView(words, LinearLayout.LayoutParams(0, -2, 1f))
        panel.addView(text("›", 24f, Color.rgb(171, 77, 65), true))
        content.addView(panel, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(8)
            bottomMargin = dp(8)
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
            setPadding(0, 0, 0, dp(6))
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
                addView(TextView(this@MainActivity).apply {
                    text = item.first
                    textSize = 23f
                    setTextColor(if (index == 0) accent else muted)
                    setTypeface(null, Typeface.BOLD)
                    gravity = Gravity.CENTER
                    setPadding(0, 0, 0, 0)
                    includeFontPadding = false
                }, LinearLayout.LayoutParams(-1, dp(30)))
                addView(TextView(this@MainActivity).apply {
                    text = item.second
                    textSize = 10f
                    setTextColor(if (index == 0) accent else muted)
                    gravity = Gravity.CENTER
                    setPadding(0, 0, 0, 0)
                    includeFontPadding = false
                }, LinearLayout.LayoutParams(-1, dp(18)))
                setOnClickListener { item.third() }
                isClickable = true
            }
            row.addView(col, LinearLayout.LayoutParams(0, -2, 1f))
        }
        root.addView(row, LinearLayout.LayoutParams(-1, dp(58)).apply {
            topMargin = dp(2)
            bottomMargin = dp(2)
        })
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
            when (currentPage) {
                "electricity" -> showElectricity()
                "water" -> showWater()
                "fuel" -> showFuel()
                "news" -> showNews()
                else -> showHome()
            }
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
        val cityIcon = TextView(this).apply {
            text = ""
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 0)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = dp(9).toFloat()
                setColor(Color.rgb(35, 36, 40))
                setStroke(dp(1), accent)
            }
        }
        cityRow.addView(iconGraphic("pin", 32), LinearLayout.LayoutParams(dp(38), dp(38)).apply {
            rightMargin = dp(10)
        })
        cityRow.addView(text(city, 16f, Color.WHITE, true), LinearLayout.LayoutParams(0, -2, 1f))
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
        val nearbyScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val nearbyRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf(
            Triple("⚕", "Аптеки", "Справочник аптек"),
            Triple("🚕", "Такси", "Контакты такси"),
            Triple("⌖", "Полезные адреса", "Городские службы"),
            Triple("chat", "Каналы", "Администрации")
        ).forEach { item ->
            nearbyRow.addView(iconTile(item.first, item.second, "", compact = true) {
                if (item.second == "Каналы") showOfficialChannels()
                else showPage(item.second, "Сведения появятся после подключения справочника.")
            }, LinearLayout.LayoutParams(dp(108), dp(72)).apply { rightMargin = dp(5) })
        }
        nearbyScroll.addView(nearbyRow)
        content.addView(nearbyScroll)

        // Small page indicators for the horizontally scrollable useful-services row.
        val nearbyDots = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(7), 0, dp(2))
        }
        val dotViews = mutableListOf<View>()
        fun refreshNearbyDots() {
            val viewport = nearbyScroll.width
            val contentWidth = nearbyRow.width
            if (viewport <= 0 || contentWidth <= 0) return
            val pageCount = maxOf(1, kotlin.math.ceil(contentWidth.toDouble() / viewport.toDouble()).toInt())
            if (dotViews.size != pageCount) {
                nearbyDots.removeAllViews()
                dotViews.clear()
                repeat(pageCount) {
                    val dot = View(this).apply {
                        background = android.graphics.drawable.GradientDrawable().apply {
                            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                            cornerRadius = dp(4).toFloat()
                            setColor(muted)
                        }
                    }
                    dotViews.add(dot)
                    nearbyDots.addView(dot, LinearLayout.LayoutParams(dp(6), dp(6)).apply {
                        leftMargin = dp(3)
                        rightMargin = dp(3)
                    })
                }
            }
            val maxScroll = maxOf(1, nearbyRow.width - nearbyScroll.width)
            val active = ((nearbyScroll.scrollX.toFloat() / maxScroll) * (pageCount - 1))
                .toInt().coerceIn(0, pageCount - 1)
            dotViews.forEachIndexed { index, dot ->
                val params = dot.layoutParams as LinearLayout.LayoutParams
                params.width = dp(if (index == active) 16 else 6)
                params.height = dp(6)
                dot.layoutParams = params
                (dot.background as android.graphics.drawable.GradientDrawable).setColor(
                    if (index == active) accent else muted
                )
            }
            nearbyDots.visibility = if (pageCount > 1) View.VISIBLE else View.GONE
        }
        nearbyScroll.setOnScrollChangeListener { _, _, _, _, _ -> refreshNearbyDots() }
        nearbyScroll.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> refreshNearbyDots() }
        nearbyRow.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> refreshNearbyDots() }
        content.addView(nearbyDots, LinearLayout.LayoutParams(-1, -2))
        bottomNav()
    }

    private fun showPage(title: String, description: String) {
        currentPage = title
        shell(title)
        content.addView(text("Город: $city", 16f, muted))
        content.addView(text(description, 16f, muted))
        content.addView(button("← На главную") { showHome() })
    }

    private fun showSettings() {
        currentPage = "settings"
        shell("⚙ Настройки")
        content.addView(text("Источник данных", 18f, Color.WHITE, true))
        content.addView(text("HTTPS-адрес API сервера. По умолчанию подключён проверенный сервер объявлений.", 14f, muted))
        val current = getPreferences(MODE_PRIVATE).getString("utility_api_base", "https://dnrsignal.duckdns.org").orEmpty()
        val field = EditText(this).apply {
            setText(current)
            hint = "https://dnrsignal.duckdns.org"
            isSingleLine = true
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            setTextColor(Color.WHITE)
            setHintTextColor(muted)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = outlinedPanel()
        }
        content.addView(field, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(10) })
        content.addView(button("Сохранить адрес") {
            val value = field.text.toString().trim().trimEnd('/')
            val uri = try { android.net.Uri.parse(value) } catch (_: Exception) { null }
            if (!value.startsWith("https://") || uri?.host.isNullOrBlank()) {
                android.widget.Toast.makeText(this, "Нужен корректный HTTPS-адрес сервера.", android.widget.Toast.LENGTH_LONG).show()
            } else {
                getPreferences(MODE_PRIVATE).edit().putString("utility_api_base", value).apply()
                android.widget.Toast.makeText(this, "Адрес сохранён.", android.widget.Toast.LENGTH_SHORT).show()
                showSettings()
            }
        })
        content.addView(button("Очистить адрес") {
            getPreferences(MODE_PRIVATE).edit().remove("utility_api_base").apply()
            showSettings()
        })
        content.addView(button("← На главную") { showHome() })
    }

    private fun showElectricity() {
        currentPage = "electricity"
        shell("⚡ Свет")
        content.addView(text("Город: $city", 17f, Color.WHITE, true))
        run {
            card("Актуальность сведений", "Проверяем опубликованные сообщения. Нет данных — не значит, что отключений нет.")
            val status = text("Загрузка сведений…", 15f, muted)
            content.addView(status)
            val entries = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            content.addView(entries)
            fun load() {
                status.text = "Загрузка сведений…"
                entries.removeAllViews()
                val selectedCity = city
                Thread {
                    val outcome = try {
                        // Set the verified HTTPS production API base URL before enabling live data.
                        val base = getPreferences(MODE_PRIVATE).getString("utility_api_base", "").orEmpty()
                        require(base.startsWith("https://")) { "Источник данных пока не подключён." }
                        val url = java.net.URL(base.trimEnd('/') + "/api/outages?kind=electricity&city=" +
                            java.net.URLEncoder.encode(selectedCity, "UTF-8"))
                        val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                            connectTimeout = 8000
                            readTimeout = 8000
                            setRequestProperty("Accept", "application/json")
                        }
                        try {
                            require(conn.responseCode == 200) { "Сервер временно недоступен." }
                            val raw = conn.inputStream.bufferedReader().use { it.readText() }
                            val json = org.json.JSONObject(raw)
                            val rows = json.getJSONArray("outages")
                            val accepted = mutableListOf<org.json.JSONObject>()
                            for (i in 0 until rows.length()) {
                                val item = rows.getJSONObject(i)
                                if (item.optString("city") == selectedCity &&
                                    item.optString("kind") == "electricity" &&
                                    item.optString("verification") == "confirmed" &&
                                    item.optString("source").isNotBlank() &&
                                    item.optString("published_at").isNotBlank()) accepted.add(item)
                            }
                            accepted
                        } finally { conn.disconnect() }
                    } catch (e: Exception) { e }
                    runOnUiThread {
                        if (currentPage != "electricity" || city != selectedCity) return@runOnUiThread
                        when (outcome) {
                            is Exception -> status.text = outcome.message ?: "Не удалось получить сведения."
                            is List<*> -> {
                                status.text = if (outcome.isEmpty()) "Подтверждённых сообщений пока нет. Это не означает отсутствие отключений." else "Подтверждённые сообщения:"
                                outcome.filterIsInstance<org.json.JSONObject>().forEach { item ->
                                    val state = when (item.optString("status")) {
                                        "planned" -> "Плановые работы"
                                        "active" -> "Аварийное отключение"
                                        "restored" -> "Электроснабжение восстановлено (по данным источника)"
                                        else -> "Статус уточняется"
                                    }
                                    val addressArray = item.optJSONArray("addresses")
                                    val addresses = if (addressArray == null) "Адреса уточняются" else
                                        (0 until addressArray.length()).joinToString(", ") { addressArray.optString(it) }.ifBlank { "Адреса уточняются" }
                                    val restore = item.optString("expected_restore_at").ifBlank { "не указано" }
                                    val panel = LinearLayout(this).apply {
                                        orientation = LinearLayout.VERTICAL
                                        setPadding(dp(16), dp(12), dp(16), dp(12))
                                        background = outlinedPanel()
                                        addView(text(state, 18f, Color.WHITE, true))
                                        addView(text(addresses, 15f, muted))
                                        addView(text("Ожидаемое восстановление: $restore", 14f, muted))
                                        addView(text("Источник: ${item.optString("source")} · ${item.optString("published_at")}", 12f, muted))
                                    }
                                    entries.addView(panel, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
                                }
                            }
                        }
                    }
                }.start()
            }
            content.addView(button("Обновить сведения") { load() })
            load()
        }
        content.addView(button("← На главную") { showHome() })
    }

    private fun showWater() {
        currentPage = "water"
        shell("💧 Вода")
        content.addView(text("Город: $city", 17f, Color.WHITE, true))
        run {
            card("Актуальность сведений", "Проверяем опубликованные сообщения. Нет данных — не значит, что отключений нет.")
            val status = text("Загрузка сведений…", 15f, muted)
            content.addView(status)
            val entries = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            content.addView(entries)
            fun load() {
                status.text = "Загрузка сведений…"
                entries.removeAllViews()
                val selectedCity = city
                Thread {
                    val outcome = try {
                        // Set the verified HTTPS production API base URL before enabling live data.
                        val base = getPreferences(MODE_PRIVATE).getString("utility_api_base", "").orEmpty()
                        require(base.startsWith("https://")) { "Источник данных пока не подключён." }
                        val url = java.net.URL(base.trimEnd('/') + "/api/outages?kind=water&city=" +
                            java.net.URLEncoder.encode(selectedCity, "UTF-8"))
                        val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                            connectTimeout = 8000
                            readTimeout = 8000
                            setRequestProperty("Accept", "application/json")
                        }
                        try {
                            require(conn.responseCode == 200) { "Сервер временно недоступен." }
                            val raw = conn.inputStream.bufferedReader().use { it.readText() }
                            val json = org.json.JSONObject(raw)
                            val rows = json.getJSONArray("outages")
                            val accepted = mutableListOf<org.json.JSONObject>()
                            for (i in 0 until rows.length()) {
                                val item = rows.getJSONObject(i)
                                if (item.optString("city") == selectedCity &&
                                    item.optString("kind") == "water" &&
                                    item.optString("verification") == "confirmed" &&
                                    item.optString("source").isNotBlank() &&
                                    item.optString("published_at").isNotBlank()) accepted.add(item)
                            }
                            accepted
                        } finally { conn.disconnect() }
                    } catch (e: Exception) { e }
                    runOnUiThread {
                        if (currentPage != "water" || city != selectedCity) return@runOnUiThread
                        when (outcome) {
                            is Exception -> status.text = outcome.message ?: "Не удалось получить сведения."
                            is List<*> -> {
                                status.text = if (outcome.isEmpty()) "Подтверждённых сообщений пока нет. Это не означает отсутствие отключений." else "Подтверждённые сообщения:"
                                outcome.filterIsInstance<org.json.JSONObject>().forEach { item ->
                                    val state = when (item.optString("status")) {
                                        "planned" -> "Плановые работы"
                                        "active" -> "Аварийное отключение"
                                        "restored" -> "Водоснабжение восстановлено (по данным источника)"
                                        else -> "Статус уточняется"
                                    }
                                    val addressArray = item.optJSONArray("addresses")
                                    val addresses = if (addressArray == null) "Адреса уточняются" else
                                        (0 until addressArray.length()).joinToString(", ") { addressArray.optString(it) }.ifBlank { "Адреса уточняются" }
                                    val restore = item.optString("expected_restore_at").ifBlank { "не указано" }
                                    val panel = LinearLayout(this).apply {
                                        orientation = LinearLayout.VERTICAL
                                        setPadding(dp(16), dp(12), dp(16), dp(12))
                                        background = outlinedPanel()
                                        addView(text(state, 18f, Color.WHITE, true))
                                        addView(text(addresses, 15f, muted))
                                        addView(text("Ожидаемое восстановление: $restore", 14f, muted))
                                        addView(text("Источник: ${item.optString("source")} · ${item.optString("published_at")}", 12f, muted))
                                    }
                                    entries.addView(panel, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
                                }
                            }
                        }
                    }
                }.start()
            }
            content.addView(button("Обновить сведения") { load() })
            load()
        }
        content.addView(button("← На главную") { showHome() })
    }

    private fun showFuel() {
        currentPage = "fuel"
        shell("⛽ Топливо")
        content.addView(text("Общая сводка: Донецк + Макеевка", 17f, Color.WHITE, true))
        if (city != "Донецк" && city != "Макеевка") {
            card("Сводка по топливу", "Доступная сводка охватывает Донецк и Макеевку. Выбранный город: $city. При необходимости смените город на главном экране.")
        } else {
            card("Статус данных", "Онлайн-источник пока не подключён. Наличие топлива на конкретных АЗС не подтверждено.")
            listOf("АИ-92", "АИ-95", "Дизель", "Газ").forEach { fuel ->
                card(fuel, "Нет актуальных подтверждённых сведений")
            }
            card("Ограничения и поставки", "Подтверждённых свежих сообщений пока нет. Это не означает, что ограничений нет.")
            content.addView(text("После подключения источников покажем АЗС, адрес, наличие, цену (если опубликована), время проверки и ссылку на сообщение.", 14f, muted))
            content.addView(button("Обновить сведения") { showFuel() })
        }
        content.addView(button("← На главную") { showHome() })
    }

    private fun friendlyNewsSource(sourceId: String): String = when (sourceId) {
        "donetsk_admin" -> "Администрация Донецка"
        "donetsk_head" -> "Глава Донецка"
        "makeevka_news" -> "Новости Макеевки"
        "debaltsevo_admin" -> "Администрация Дебальцево"
        "debaltsevo_head" -> "Глава Дебальцево"
        "dokuchaevsk_admin" -> "Администрация Докучаевска"
        "dokuchaevsk_head" -> "Глава Докучаевска"
        "gorlovka_admin" -> "Администрация Горловки"
        "enakievo_admin" -> "Администрация Енакиево"
        "enakievo_head" -> "Глава Енакиево"
        "ilovaisk_admin" -> "Администрация Иловайска"
        "ilovaisk_head" -> "Руководитель Иловайска"
        "mariupol_admin" -> "Администрация Мариуполя"
        "mariupol_head" -> "Глава Мариуполя"
        "snezhnoe_admin" -> "Администрация Снежного"
        "snezhnoe_head" -> "Глава Снежного"
        "torez_admin" -> "Администрация Тореза"
        "khartsyzsk_admin" -> "Администрация Харцызска"
        "amvrosievka_admin" -> "Администрация Амвросиевки"
        "amvrosievka_head" -> "Глава Амвросиевки"
        "volnovakha_admin" -> "Администрация Волновахи"
        "volnovakha_head" -> "Глава Волновахи"
        "shakhtarsk_admin" -> "Администрация Шахтёрска"
        "shakhtarsk_head" -> "Глава Шахтёрска"
        "novoazovsk_admin" -> "Администрация Новоазовска"
        "novoazovsk_head" -> "Руководитель Новоазовского округа"
        "telmanovo_admin" -> "Администрация Тельманово"
        "starobeshevo_admin" -> "Администрация Старобешево"
        "volodarsk_admin" -> "Администрация Володарского"
        "mangush_admin" -> "Администрация Мангуша"
        "zhdanovka_admin" -> "Администрация Ждановки"
        "kirovskoe_admin" -> "Администрация Кировского"
        "yasinovataya_admin" -> "Администрация Ясиноватой"
        "krasny_liman_admin" -> "Администрация Красного Лимана"
        "fuel_dnr" -> "Источник по топливу"
        "benz_dnr" -> "Источник по топливу"
        "voda_dnr" -> "Источник по водоснабжению"
        else -> "Официальный источник"
    }

    private fun showNews() {
        currentPage = "news"
        shell("▤ Новости")
        content.addView(text("Город: $city", 17f, Color.WHITE, true))
        run {
            card("Проверенные публикации", "Показываем только сообщения, прошедшие проверку. Пустая лента не означает, что в городе ничего не происходит.")
            val status = text("Загрузка новостей…", 15f, muted)
            content.addView(status)
            val entries = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            content.addView(entries)
            fun load() {
                status.text = "Загрузка новостей…"
                entries.removeAllViews()
                val selectedCity = city
                Thread {
                    val outcome = try {
                        val base = getPreferences(MODE_PRIVATE).getString("utility_api_base", "").orEmpty()
                        require(base.startsWith("https://")) { "Источник новостей пока не подключён." }
                        val url = java.net.URL(base.trimEnd('/') + "/api/news?city=" +
                            java.net.URLEncoder.encode(selectedCity, "UTF-8") + "&limit=20")
                        val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                            connectTimeout = 8000
                            readTimeout = 8000
                            setRequestProperty("Accept", "application/json")
                        }
                        try {
                            require(conn.responseCode == 200) { "Сервер новостей временно недоступен." }
                            val json = org.json.JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                            val rows = json.getJSONArray("items")
                            val accepted = mutableListOf<org.json.JSONObject>()
                            for (i in 0 until rows.length()) {
                                val item = rows.getJSONObject(i)
                                val sourceUrl = item.optString("source_url")
                                val parsedSource = android.net.Uri.parse(sourceUrl)
                                val sourceHost = parsedSource.host?.lowercase()
                                val trustedSource = parsedSource.scheme == "https" &&
                                    parsedSource.userInfo == null &&
                                    (sourceHost == "t.me" || sourceHost == "max.ru") &&
                                    parsedSource.path.orEmpty().length > 1
                                if (item.optString("city") == selectedCity &&
                                    item.optString("verification") == "confirmed" &&
                                    trustedSource &&
                                    item.optString("title").isNotBlank() &&
                                    item.optString("published_at").isNotBlank()) accepted.add(item)
                            }
                            accepted
                        } finally { conn.disconnect() }
                    } catch (e: Exception) { e }
                    runOnUiThread {
                        if (currentPage != "news" || city != selectedCity) return@runOnUiThread
                        when (outcome) {
                            is Exception -> status.text = outcome.message ?: "Не удалось получить новости."
                            is List<*> -> {
                                val news = outcome.filterIsInstance<org.json.JSONObject>()
                                status.text = if (news.isEmpty()) "Подтверждённых публикаций пока нет." else "Публикации: ${news.size}"
                                news.forEach { item ->
                                    val panel = LinearLayout(this).apply {
                                        orientation = LinearLayout.VERTICAL
                                        setPadding(dp(16), dp(12), dp(16), dp(12))
                                        background = outlinedPanel()
                                        addView(text(item.optString("title"), 17f, Color.WHITE, true))
                                        addView(text(item.optString("summary"), 15f, muted))
                                        addView(text(friendlyNewsSource(item.optString("source")) + " · " + item.optString("published_at"), 12f, muted))
                                        addView(button("Открыть оригинал") {
                                            val sourceUri = android.net.Uri.parse(item.optString("source_url"))
                                            if (sourceUri.scheme == "https" &&
                                                sourceUri.userInfo == null &&
                                                (sourceUri.host?.lowercase() == "t.me" || sourceUri.host?.lowercase() == "max.ru") &&
                                                sourceUri.path.orEmpty().length > 1) {
                                                startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, sourceUri))
                                            } else {
                                                android.widget.Toast.makeText(this@MainActivity, "Ссылка на источник не прошла проверку.", android.widget.Toast.LENGTH_LONG).show()
                                            }
                                        })
                                    }
                                    val params = LinearLayout.LayoutParams(-1, -2)
                                    params.bottomMargin = dp(8)
                                    entries.addView(panel, params)
                                }
                            }
                        }
                    }
                }.start()
            }
            load()
            content.addView(button("Обновить ленту") { load() })
        }
        content.addView(button("← На главную") { showHome() })
    }

    private fun showAnnouncements() {
        currentPage = "announcements"
        shell("Частные объявления и реклама")
        content.addView(text("Объявления жителей, услуги мастеров и реклама местного бизнеса", 16f, muted))
        val status = text("Загрузка объявлений…", 15f, muted)
        content.addView(status)
        val entries = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(entries)

        fun load() {
            status.text = "Загрузка объявлений…"
            entries.removeAllViews()
            Thread {
                val outcome = try {
                    val base = getPreferences(MODE_PRIVATE)
                        .getString("utility_api_base", "https://dnrsignal.duckdns.org")
                        .orEmpty().trim().trimEnd('/')
                    require(base.startsWith("https://")) { "Нужен HTTPS-адрес API." }
                    val parsedBase = android.net.Uri.parse(base)
                    require(!parsedBase.host.isNullOrBlank() && parsedBase.userInfo == null) {
                        "Некорректный адрес API."
                    }
                    val conn = (java.net.URL(base + "/api/announcements").openConnection()
                        as java.net.HttpURLConnection).apply {
                        connectTimeout = 8000
                        readTimeout = 8000
                        setRequestProperty("Accept", "application/json")
                    }
                    try {
                        require(conn.responseCode == 200) { "Сервер объявлений временно недоступен." }
                        val json = org.json.JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                        val rows = json.getJSONArray("announcements")
                        val items = mutableListOf<org.json.JSONObject>()
                        for (i in 0 until rows.length()) {
                            val item = rows.getJSONObject(i)
                            if (item.optString("category") != "news" &&
                                item.optString("title").isNotBlank() &&
                                item.optString("body").isNotBlank()) items.add(item)
                        }
                        items
                    } finally { conn.disconnect() }
                } catch (e: Exception) { e }
                runOnUiThread {
                    if (currentPage != "announcements") return@runOnUiThread
                    when (outcome) {
                        is Exception -> status.text = outcome.message ?: "Не удалось загрузить объявления."
                        is List<*> -> {
                            val items = outcome.filterIsInstance<org.json.JSONObject>()
                            status.text = if (items.isEmpty()) {
                                "Пока нет опубликованных объявлений."
                            } else {
                                "Опубликованные объявления: ${items.size}"
                            }
                            items.forEach { item ->
                                val category = when (item.optString("category")) {
                                    "general" -> "Общее"
                                    "electricity" -> "Электроснабжение"
                                    "water" -> "Водоснабжение"
                                    "news" -> "Новости"
                                    "emergency" -> "Важное"
                                    else -> "Объявление"
                                }
                                val panel = LinearLayout(this).apply {
                                    orientation = LinearLayout.VERTICAL
                                    setPadding(dp(16), dp(12), dp(16), dp(12))
                                    background = outlinedPanel()
                                    addView(text(category.uppercase(), 12f, accent, true))
                                    addView(text(item.optString("title"), 19f, Color.WHITE, true))
                                    addView(text(item.optString("body"), 15f, muted))
                                    val published = item.optString("published_at")
                                    if (published.isNotBlank()) addView(text("Опубликовано: $published", 12f, muted))
                                    val actionUrl = item.optString("action_url")
                                    val actionUri = android.net.Uri.parse(actionUrl)
                                    if (actionUri.scheme == "https" && !actionUri.host.isNullOrBlank() &&
                                        actionUri.userInfo == null) {
                                        addView(button("Подробнее") {
                                            startActivity(android.content.Intent(
                                                android.content.Intent.ACTION_VIEW, actionUri))
                                        })
                                    }
                                    val phone = item.optString("action_phone").trim()
                                    if (phone.isNotBlank() && phone.matches(Regex("^[+0-9() -]{5,24}$"))) {
                                        addView(button("Позвонить: $phone") {
                                            startActivity(android.content.Intent(
                                                android.content.Intent.ACTION_DIAL,
                                                android.net.Uri.parse("tel:" + phone.filter { it.isDigit() || it == '+' })))
                                        })
                                    }
                                }
                                entries.addView(panel, LinearLayout.LayoutParams(-1, -2).apply {
                                    bottomMargin = dp(8)
                                })
                            }
                        }
                    }
                }
            }.start()
        }
        load()
        content.addView(button("Обновить объявления") { load() })
        content.addView(button("← На главную") { showHome() })
    }

    private fun showOfficialChannels() {
        currentPage = "channels"
        shell("Официальные каналы")
        content.addView(text("Город: $city", 17f, Color.WHITE, true))
        content.addView(text("Ссылки ведут на опубликованные каналы администраций и руководителей. Открытие происходит в Telegram или MAX. Наличие ссылки не означает автоматическую загрузку публикаций в приложение.", 14f, muted))

        data class OfficialLink(val organization: String, val telegram: String? = null, val max: String? = null)
        val links = when (city) {
            "Донецк" -> listOf(
                OfficialLink("Администрация города", "https://t.me/gorodDonetsk"),
                OfficialLink("Глава города", "https://t.me/kulemzin_donetsk", "https://max.ru/kulemzin_donetsk")
            )
            "Макеевка" -> listOf(OfficialLink("Администрация города", "https://t.me/Novosti_Makeevka", "https://max.ru/makeevka_ru"))
            "Дебальцево" -> listOf(
                OfficialLink("Администрация города", "https://t.me/admDebaltsevo"),
                OfficialLink("Глава города", "https://t.me/glavadebaltsevo")
            )
            "Докучаевск" -> listOf(
                OfficialLink("Администрация города", "https://t.me/Dokuchaevsk_admin", "https://max.ru/id9306004432_gos"),
                OfficialLink("Глава города", "https://t.me/dokuchaevsk", "https://max.ru/id9306004432_gos2")
            )
            "Горловка" -> listOf(OfficialLink("Администрация города", "https://t.me/Gorlowka_now", "https://max.ru/gorlowka_now"))
            "Енакиево" -> listOf(
                OfficialLink("Администрация города", "https://t.me/Enakievo_official", "https://max.ru/enakievo_official"),
                OfficialLink("Глава города", "https://t.me/IgorMartynov_enk")
            )
            "Иловайск" -> listOf(
                OfficialLink("Администрация городского округа", "https://t.me/ilovaisk_onlain", "https://max.ru/id9304022980_gos"),
                OfficialLink("Руководитель городского округа", "https://t.me/dz_ilov_gorokrug")
            )
            "Мариуполь" -> listOf(
                OfficialLink("Администрация города", "https://t.me/nmrpl"),
                OfficialLink("Глава города", "https://t.me/morgun_ov")
            )
            "Снежное" -> listOf(
                OfficialLink("Администрация города", "https://t.me/Snijne", "https://max.ru/id9304022972_gos"),
                OfficialLink("Глава города", "https://t.me/Ermakov_ESV")
            )
            "Торез (Чистяково)" -> listOf(OfficialLink("Администрация города", "https://t.me/torez_admin", "https://max.ru/id9304022958_gos"))
            "Харцызск" -> listOf(OfficialLink("Администрация города", "https://t.me/khartsyzsk_press_office"))
            "Амвросиевка" -> listOf(
                OfficialLink("Администрация округа", "https://t.me/amvr_admin", "https://max.ru/id9304022919_gos"),
                OfficialLink("Глава администрации", "https://t.me/Lyzov_amvrosievka", "https://max.ru/join/HIkfVJh2KB7pFxakfgjwwaGirL1L-t9kZISWlNE96XE")
            )
            "Волноваха" -> listOf(
                OfficialLink("Администрация округа", "https://t.me/volnovahadnr", "https://max.ru/id9306004440_gos"),
                OfficialLink("Глава администрации", "https://t.me/kazinchenko", "https://max.ru/id9306004440_gos2")
            )
            "Шахтёрск" -> listOf(
                OfficialLink("Администрация округа", "https://t.me/AGShahtersk", "https://max.ru/mo_shahtersk"),
                OfficialLink("Глава администрации", "https://t.me/shatov_av_agsh")
            )
            "Новоазовск" -> listOf(
                OfficialLink("Администрация округа", "https://t.me/novoazovsk_adm", "https://max.ru/id9305011204_gos"),
                OfficialLink("Канал руководителя округа", "https://t.me/novoazovskiy_region")
            )
            "Тельманово" -> listOf(OfficialLink("Администрация округа", "https://t.me/adm_telmanovskogo_rajona", "https://max.ru/id9305011162_gos"))
            "Старобешево" -> listOf(OfficialLink("Администрация округа", "https://t.me/starobeshevo1923"))
            "Володарское" -> listOf(OfficialLink("Администрация округа", "https://t.me/volodinfo"))
            "Мангуш" -> listOf(OfficialLink("Администрация округа", "https://t.me/Mangush_mo"))
            "Ждановка" -> listOf(OfficialLink("Администрация города", "https://t.me/adm_zhdanovka"))
            "Кировское (Крестовка)" -> listOf(OfficialLink("Администрация города", "https://t.me/gorod_kirovskoe"))
            "Ясиноватая" -> listOf(OfficialLink("Администрация округа", "https://t.me/gromko_yas"))
            "Красный Лиман" -> listOf(OfficialLink("Администрация округа", "https://t.me/kr_liman_admin"))
            else -> emptyList()
        }

        if (links.isEmpty()) {
            card("Проверенные ссылки пока не добавлены", "Для выбранного населённого пункта мы ещё не подтвердили официальные каналы. Не подставляем случайные ссылки.")
        } else {
            links.forEach { link ->
                val panel = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(14), dp(8), dp(14), dp(10))
                    background = outlinedPanel()
                }
                panel.addView(text(link.organization, 16f, Color.WHITE, true))
                val actions = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                link.telegram?.let { url ->
                    actions.addView(button("Открыть Telegram") {
                        startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                    }, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(6) })
                }
                link.max?.let { url ->
                    actions.addView(button("Открыть MAX") {
                        startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                    }, LinearLayout.LayoutParams(0, -2, 1f))
                }
                panel.addView(actions, LinearLayout.LayoutParams(-1, -2))
                if (link.max == null) panel.addView(text("Подтверждённая ссылка MAX пока не добавлена.", 12f, muted))
                content.addView(panel, LinearLayout.LayoutParams(-1, -2).apply {
                    topMargin = dp(6)
                    bottomMargin = dp(4)
                })
            }
        }
        content.addView(button("← На главную") { showHome() })
    }

    private fun showCommunityChat() {
        currentPage = "chat"
        if (city == "Выберите город") { chooseCity(); return }
        shell("Онлайн-чат")
        run {
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
        content.addView(text("Экстренный вызов 112. Приложение не звонит автоматически.", 16f, muted))

        content.addView(button("Позвонить 112") {
            android.app.AlertDialog.Builder(this)
                .setTitle("Открыть набор номера 112?")
                .setMessage("Откроется приложение телефона с номером 112. Чтобы начать вызов, нажмите кнопку звонка в приложении телефона.")
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Открыть набор") { _, _ ->
                    val dialIntent = android.content.Intent(
                        android.content.Intent.ACTION_DIAL,
                        android.net.Uri.parse("tel:112")
                    )
                    try {
                        if (dialIntent.resolveActivity(packageManager) != null) {
                            startActivity(dialIntent)
                        } else {
                            android.widget.Toast.makeText(
                                this, "На устройстве не найдено приложение для звонков.",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    } catch (_: android.content.ActivityNotFoundException) {
                        android.widget.Toast.makeText(
                            this, "Не удалось открыть набор номера. Попробуйте открыть приложение телефона вручную.",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    } catch (_: SecurityException) {
                        android.widget.Toast.makeText(
                            this, "Система не разрешила открыть набор номера.",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }.show()
        })

        content.addView(text("Приложение не подтверждает, что экстренная служба получила вызов.", 13f, muted))
        content.addView(button("← На главную") { showHome() })
    }
}