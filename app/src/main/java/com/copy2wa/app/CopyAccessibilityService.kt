package com.copy2wa.app

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat

class CopyAccessibilityService : AccessibilityService() {

    private var lastClipboard: String? = null
    private var lastPromptedNumber: String? = null
    private var lastPromptedAt: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        ensureChannel(this)
        // Don't fire on whatever was in the clipboard before the service started
        lastClipboard = try {
            getSystemService(ClipboardManager::class.java)?.primaryClip
                ?.getItemAt(0)?.coerceToText(this)?.toString()
        } catch (_: Exception) { null }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val cm = getSystemService(ClipboardManager::class.java) ?: return
        val clip = cm.primaryClip ?: return
        if (clip.itemCount <= 0) return
        val text = clip.getItemAt(0)?.coerceToText(this)?.toString() ?: return
        if (text.isBlank() || text == lastClipboard) return   // react only to NEW clipboard content
        lastClipboard = text
        onNumberCopied(text.trim())
    }

    override fun onInterrupt() {}

    private fun onNumberCopied(raw: String) {
        val cc = getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE)
            .getString(MainActivity.KEY_CC, MainActivity.DEFAULT_CC) ?: MainActivity.DEFAULT_CC
        val number = PhoneNormalizer.normalize(raw, cc) ?: return

        // Ignore same number repeated within cooldown (as requested)
        val now = System.currentTimeMillis()
        if (number == lastPromptedNumber && now - lastPromptedAt < REPEAT_COOLDOWN_MS) return
        lastPromptedNumber = number
        lastPromptedAt = now

        showPrompt(number)
    }

    private fun showPrompt(number: String) {
        val openPi = PendingIntent.getActivity(
            this, number.hashCode(),
            WhatsAppLauncher.chatIntent(this, number),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val dismissPi = PendingIntent.getBroadcast(
            this, number.hashCode(),
            Intent(this, DismissReceiver::class.java).putExtra(EXTRA_NUMBER, number),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_send)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text, pretty(number)))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(openPi)
            .addAction(0, getString(R.string.notif_open), openPi)
            .addAction(0, getString(R.string.notif_dismiss), dismissPi)
            .build()
        getSystemService(NotificationManager::class.java)?.notify(number.hashCode(), notification)
    }

    private fun pretty(number: String): String = "+$number"

    companion object {
        const val CHANNEL_ID = "copied_numbers"
        const val EXTRA_NUMBER = "number"
        const val REPEAT_COOLDOWN_MS = 60_000L

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < 26) return
            val nm = context.getSystemService(NotificationManager::class.java) ?: return
            val channel = NotificationChannel(
                CHANNEL_ID, "Copied phone numbers", NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Pop-up prompts for phone numbers you copy"
            nm.createNotificationChannel(channel)
        }
    }
}

class DismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val number = intent.getStringExtra(CopyAccessibilityService.EXTRA_NUMBER) ?: return
        context.getSystemService(NotificationManager::class.java)?.cancel(number.hashCode())
    }
}
