package com.example.moigorod

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 56, 32, 32)
            setBackgroundColor(Color.rgb(245, 245, 247))
        }
        fun heading(text: String, size: Float, color: Int) = TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            setPadding(0, 12, 0, 12)
            gravity = Gravity.START
        }
        root.addView(heading("Мой город", 32f, Color.rgb(252, 63, 29)))
        root.addView(heading("Радио • Оповещения • Объявления", 18f, Color.DKGRAY))
        root.addView(heading("Первая чистая сборка. Функции добавим после проверки APK.", 16f, Color.BLACK))
        setContentView(root)
    }
}
