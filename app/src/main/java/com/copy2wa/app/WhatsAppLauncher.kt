package com.copy2wa.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

object WhatsAppLauncher {
    private const val WHATSAPP = "com.whatsapp"

    /** Intent that opens an EMPTY chat with the number via wa.me */
    fun chatIntent(context: Context, normalizedNumber: String): Intent {
        val uri = Uri.parse("https://wa.me/$normalizedNumber")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        // Force regular WhatsApp (falls back to default handler if not installed)
        try {
            context.packageManager.getPackageInfo(WHATSAPP, 0)
            intent.setPackage(WHATSAPP)
        } catch (_: Exception) { }
        return intent
    }

    fun open(context: Context, normalizedNumber: String) {
        try {
            context.startActivity(chatIntent(context, normalizedNumber))
        } catch (e: ActivityNotFoundException) {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$normalizedNumber"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (_: Exception) { }
        }
    }
}
