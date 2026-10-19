package io.github.martystev.smspigeon

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder

class ForwardService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startInForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isInForeground) {
            startInForeground()
        }

        if (intent?.action == ACTION_SEND_SMS) {
            val simName = intent.getStringExtra(EXTRA_SIM_NAME) ?: "SIM ?"
            val sender = intent.getStringExtra(EXTRA_SENDER) ?: ""
            val text = intent.getStringExtra(EXTRA_TEXT) ?: ""

            Thread {
                val settings = SettingsManager(this)
                val message = buildMessage(simName, sender, text)
                TelegramSender.sendMessage(settings.getToken(), settings.getChatId(), message)
            }.start()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        isInForeground = false
        super.onDestroy()
    }

    private fun startInForeground() {
        val channelId = CHANNEL_ID
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "SmsPigeon",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            Notification.Builder(this)
        }

        val notification: Notification = builder
            .setContentTitle(getString(R.string.notification_title))
            .setSmallIcon(R.drawable.ic_sms)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        isInForeground = true
    }

    private fun buildMessage(simName: String, sender: String, text: String): String {
        return getString(R.string.forward_message, simName, sender, text)
    }

    companion object {
        const val ACTION_SEND_SMS = "io.github.martystev.smspigeon.ACTION_SEND_SMS"
        const val EXTRA_SIM_NAME = "extra_sim_name"
        const val EXTRA_SENDER = "extra_sender"
        const val EXTRA_TEXT = "extra_text"

        private const val CHANNEL_ID = "sms_forwarder_channel"
        private const val NOTIFICATION_ID = 1001
        private var isInForeground: Boolean = false
    }
}
