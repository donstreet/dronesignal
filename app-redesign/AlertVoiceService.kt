package com.example.dronesignal

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.util.Locale

/**
 * Голосовое оповещение о тревоге. Радиопотоки и список FM-станций удалены.
 * Имя класса отдельно от App.kt позволяет сохранить логику тревог и отбоя.
 */
class AlertVoiceService : Service() {
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var alertText = ""
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var vibrator: Vibrator

    private val voiceTicker = object : Runnable {
        override fun run() {
            if (!AlertState.isActive(this@AlertVoiceService)) {
                stopClean()
                return
            }
            speakAlert()
            vibrateDanger()
            handler.postDelayed(this, 30_000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("ru", "RU")
                tts?.setSpeechRate(0.95f)
                ttsReady = true
                if (AlertState.isActive(this)) speakAlert()
            }
        }
        startForeground(1, buildNotif("Голосовое предупреждение включено"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                alertText = intent.getStringExtra(EXTRA_TEXT).orEmpty()
                vibrateDanger()
                speakAlert()
                handler.removeCallbacks(voiceTicker)
                handler.postDelayed(voiceTicker, 30_000L)
                updateNotif("Голосовое предупреждение об опасности БПЛА")
            }
            ACTION_UPDATE -> {
                alertText = intent.getStringExtra(EXTRA_TEXT).orEmpty()
                speakAlert()
            }
            ACTION_STOP -> {
                stopClean()
                return START_NOT_STICKY
            }
        }
        return START_STICKY
    }

    private fun speakAlert() {
        if (!ttsReady || !AlertState.isActive(this)) return
        val text = alertText.ifBlank { "Внимание! Опасность беспилотников." }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "voice-${System.nanoTime()}")
    }

    private fun vibrateDanger() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400, 200, 800), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 400, 200, 400, 200, 800), -1)
        }
    }

    private fun stopClean() {
        handler.removeCallbacksAndMessages(null)
        tts?.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotif(text: String): Notification {
        val channelId = "drone_alert"
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
            NotificationChannel(channelId, "Опасность БПЛА", NotificationManager.IMPORTANCE_HIGH)
        )
        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, AlertVoiceService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("⚠ Опасность БПЛА")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .addAction(0, "Отключить голос", stopIntent)
            .build()
    }

    private fun updateNotif(text: String) {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(1, buildNotif(text))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "START"
        const val ACTION_UPDATE = "UPDATE"
        const val ACTION_STOP = "STOP"
        const val EXTRA_TEXT = "text"

        fun start(ctx: Context, text: String = "") {
            ContextCompat.startForegroundService(
                ctx,
                Intent(ctx, AlertVoiceService::class.java).setAction(ACTION_START).putExtra(EXTRA_TEXT, text)
            )
        }

        fun update(ctx: Context, text: String) {
            runCatching {
                ctx.startService(
                    Intent(ctx, AlertVoiceService::class.java).setAction(ACTION_UPDATE).putExtra(EXTRA_TEXT, text)
                )
            }
        }

        fun stop(ctx: Context) {
            runCatching {
                ctx.startService(Intent(ctx, AlertVoiceService::class.java).setAction(ACTION_STOP))
            }
        }
    }
}
