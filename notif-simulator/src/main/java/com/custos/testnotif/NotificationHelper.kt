package com.custos.testnotif

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.concurrent.atomic.AtomicInteger

object NotificationHelper {
    private const val TAG = "Custos.NotifSim"
    const val CHANNEL_ID = "custos_test_channel"
    private const val CHANNEL_NAME = "Custos Test Notifications"

    val notificationCounter = AtomicInteger(0)
    var lastPostedInfo: String = "None"
        private set

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (nm != null && nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Channel for simulating notifications to test Custos muting"
                    enableVibration(true)
                }
                nm.createNotificationChannel(channel)
                Log.i(TAG, "Notification channel created: $CHANNEL_ID")
            }
        }
    }

    fun postNotification(
        context: Context,
        id: Int = 1001,
        title: String = "Custos Test Notification",
        text: String = "Test notification message",
        highPriority: Boolean = true
    ): Boolean {
        ensureChannel(context)

        val priority = if (highPriority) {
            NotificationCompat.PRIORITY_HIGH
        } else {
            NotificationCompat.PRIORITY_DEFAULT
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(priority)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        return try {
            val nm = NotificationManagerCompat.from(context)
            nm.notify(id, builder.build())
            val count = notificationCounter.incrementAndGet()
            lastPostedInfo = "ID: $id | '$title' (#$count)"
            Log.i(TAG, "✅ Notification posted successfully [id=$id, title='$title', count=$count]")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Missing POST_NOTIFICATIONS permission: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to post notification: ${e.message}", e)
            false
        }
    }

    fun postDelayedNotification(
        context: Context,
        delaySeconds: Int,
        id: Int = 1001,
        title: String = "Custos Test Notification",
        text: String = "Test notification message",
        highPriority: Boolean = true
    ) {
        Log.i(TAG, "Scheduling notification in ${delaySeconds}s [id=$id, title='$title']")
        Handler(Looper.getMainLooper()).postDelayed({
            postNotification(context, id, title, text, highPriority)
        }, delaySeconds * 1000L)
    }

    fun clearNotification(context: Context, id: Int) {
        val nm = NotificationManagerCompat.from(context)
        nm.cancel(id)
        Log.i(TAG, "Notification cancelled [id=$id]")
    }

    fun clearAll(context: Context) {
        val nm = NotificationManagerCompat.from(context)
        nm.cancelAll()
        Log.i(TAG, "All notifications cleared")
    }
}
