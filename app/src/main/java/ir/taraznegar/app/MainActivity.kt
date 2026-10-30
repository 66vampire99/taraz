package ir.taraznegar.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var pendingSmsToDeliver: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createNotificationChannels()
        requestNecessaryPermissions()

        webView = WebView(this)
        setContentView(webView)

        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = true

        webView.webChromeClient = WebChromeClient()
        webView.addJavascriptInterface(AndroidBridge(this), "AndroidBridge")

        pendingSmsToDeliver = intent?.getStringExtra("incoming_sms")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                pendingSmsToDeliver?.let { sms ->
                    deliverSmsToWebView(sms)
                    pendingSmsToDeliver = null
                }
            }
        }

        webView.loadUrl("file:///android_asset/index.html")
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val sms = intent?.getStringExtra("incoming_sms")
        if (!sms.isNullOrEmpty()) {
            deliverSmsToWebView(sms)
        }
    }

    private fun deliverSmsToWebView(sms: String) {
        val escaped = JSONObject.quote(sms)
        webView.post {
            webView.evaluateJavascript("if(window.onRealBankSmsReceived) window.onRealBankSmsReceived($escaped);", null)
        }
    }

    private fun requestNecessaryPermissions() {
        val perms = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val needed = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), 101)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val instChannel = NotificationChannel(
                "LOCKED_INSTALLMENTS",
                "یادآور اقساط ماهیانه (قفل‌شونده)",
                NotificationManager.IMPORTANCE_HIGH
            )
            nm.createNotificationChannel(instChannel)
        }
    }

    inner class AndroidBridge(private val context: Context) {

        @JavascriptInterface
        fun syncLockedNotification(id: Int, title: String, subtitle: String, isUrgent1Day: Boolean) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context, id, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, "LOCKED_INSTALLMENTS")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(if (isUrgent1Day) "🔒 $title (غیرقابل حذف تا پرداخت)" else "🔔 $title")
                .setContentText(subtitle)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(pendingIntent)
                .setOngoing(isUrgent1Day)     // TRUE = Locked in Android Notification Drawer! Cannot be swiped away!
                .setAutoCancel(!isUrgent1Day)

            nm.notify(id, builder.build())
        }

        @JavascriptInterface
        fun cancelInstallmentNotification(id: Int) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(id)
        }
    }
}
