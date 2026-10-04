package com.custos.testnotif

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : Activity() {

    private lateinit var textPermissionStatus: TextView
    private lateinit var textCounter: TextView
    private lateinit var textLastPost: TextView
    private lateinit var inputTitle: EditText
    private lateinit var inputText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textPermissionStatus = findViewById(R.id.text_permission_status)
        textCounter = findViewById(R.id.text_counter)
        textLastPost = findViewById(R.id.text_last_post)
        inputTitle = findViewById(R.id.input_title)
        inputText = findViewById(R.id.input_text)

        val btnPostNow: Button = findViewById(R.id.btn_post_now)
        val btnPostDelayed5s: Button = findViewById(R.id.btn_post_delayed_5s)
        val btnClear: Button = findViewById(R.id.btn_clear)

        btnPostNow.setOnClickListener {
            val title = inputTitle.text.toString().ifBlank { "Custos Test Notification" }
            val text = inputText.text.toString().ifBlank { "Manual test trigger" }
            val ok = NotificationHelper.postNotification(this, title = title, text = text)
            if (ok) {
                Toast.makeText(this, "Notification posted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to post (check permission)", Toast.LENGTH_SHORT).show()
            }
            updateUi()
        }

        btnPostDelayed5s.setOnClickListener {
            val title = inputTitle.text.toString().ifBlank { "Custos Delayed Notification" }
            val text = inputText.text.toString().ifBlank { "Fired after 5 seconds" }
            NotificationHelper.postDelayedNotification(
                context = this,
                delaySeconds = 5,
                title = title,
                text = text
            )
            Toast.makeText(this, "Notification will post in 5s", Toast.LENGTH_SHORT).show()
            updateUi()
        }

        btnClear.setOnClickListener {
            NotificationHelper.clearAll(this)
            Toast.makeText(this, "Cleared notifications", Toast.LENGTH_SHORT).show()
            updateUi()
        }

        checkAndRequestNotificationPermission()
        updateUi()
    }

    override fun onResume() {
        super.onResume()
        updateUi()
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_CODE_POST_NOTIF
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        updateUi()
    }

    private fun updateUi() {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        textPermissionStatus.text = if (hasPermission) {
            "Permission: GRANTED ✅"
        } else {
            "Permission: DENIED ❌ (Tap to request)"
        }
        textPermissionStatus.setOnClickListener {
            if (!hasPermission) {
                checkAndRequestNotificationPermission()
            }
        }

        textCounter.text = "Notifications posted: ${NotificationHelper.notificationCounter.get()}"
        textLastPost.text = "Last post: ${NotificationHelper.lastPostedInfo}"
    }

    companion object {
        private const val REQUEST_CODE_POST_NOTIF = 101
    }
}
