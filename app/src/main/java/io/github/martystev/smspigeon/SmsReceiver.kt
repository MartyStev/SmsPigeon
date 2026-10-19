package io.github.martystev.smspigeon

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val settings = SettingsManager(context)
        if (!settings.isForwardingEnabled()) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) return

        val sender = messages[0].originatingAddress ?: ""
        val text = buildString {
            for (msg in messages) {
                append(msg.messageBody)
            }
        }

        val filterManager = FilterManager(context)
        if (!filterManager.isAllowed(sender)) return

        val slotIndex = extractSlotIndex(intent)
        val simName = settings.getSimNameForSlot(slotIndex)

        val serviceIntent = Intent(context, ForwardService::class.java).apply {
            action = ForwardService.ACTION_SEND_SMS
            putExtra(ForwardService.EXTRA_SIM_NAME, simName)
            putExtra(ForwardService.EXTRA_SENDER, sender)
            putExtra(ForwardService.EXTRA_TEXT, text)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    private fun extractSlotIndex(intent: Intent): Int {
        val extras = intent.extras ?: return -1
        val keys = listOf(
            "slot",
            "slot_id",
            "simSlot",
            "sim_slot",
            "phone",
            "android.telephony.extra.SLOT_INDEX"
        )
        for (k in keys) {
            if (extras.containsKey(k)) {
                try {
                    val v = extras.getInt(k)
                    if (v >= 0) return v
                } catch (_: Exception) {
                    // Ignore non-int extra
                }
            }
        }
        return -1
    }
}
