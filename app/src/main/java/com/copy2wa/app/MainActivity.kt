package com.copy2wa.app

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var ccInput: EditText
    private lateinit var testInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        ccInput = findViewById(R.id.ccInput)
        testInput = findViewById(R.id.testInput)

        ccInput.setText(
            getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_CC, DEFAULT_CC)
        )

        findViewById<Button>(R.id.btnAccessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            val cc = ccInput.text.toString().filter { it.isDigit() }.ifEmpty { DEFAULT_CC }
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_CC, cc).apply()
            Toast.makeText(this, getString(R.string.saved_toast, cc), Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnTest).setOnClickListener {
            val cc = ccInput.text.toString().filter { it.isDigit() }.ifEmpty { DEFAULT_CC }
            val number = PhoneNormalizer.normalize(testInput.text.toString(), cc)
            if (number == null) {
                Toast.makeText(this, R.string.test_invalid_toast, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.test_ok_toast, "+$number"), Toast.LENGTH_SHORT).show()
                WhatsAppLauncher.open(this, number)
            }
        }

        requestNotificationPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        val on = isAccessibilityServiceEnabled()
        statusText.text = getString(if (on) R.string.status_on else R.string.status_off)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any { it.contains(packageName) }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001
            )
        }
    }

    companion object {
        const val PREFS = "settings"
        const val KEY_CC = "country_code"
        const val DEFAULT_CC = "62"
    }
}
