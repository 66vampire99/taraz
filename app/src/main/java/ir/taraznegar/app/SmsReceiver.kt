package ir.taraznegar.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            val fullBody = StringBuilder()
            for (msg in messages) {
                fullBody.append(msg.displayMessageBody ?: "")
            }
            val bodyText = fullBody.toString()

            // Check if SMS looks like an Iranian Bank Transaction
            val bankKeywords = listOf("بانک", "برداشت", "واریز", "مانده", "ریال", "خرید", "حساب", "کارت")
            val isBankSms = bankKeywords.count { bodyText.contains(it) } >= 2

            if (isBankSms) {
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("incoming_sms", bodyText)
                }
                context.startActivity(launchIntent)
            }
        }
    }
}
