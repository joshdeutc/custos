package com.custos.testnotif

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class NotificationTriggerReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "Custos.NotifSimReceiver"

        const val ACTION_POST = "com.custos.testnotif.POST"
        const val ACTION_CLEAR = "com.custos.testnotif.CLEAR"
        const val ACTION_PING = "com.custos.testnotif.PING"

        const val EXTRA_TITLE = "title"
        const val EXTRA_TEXT = "text"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_BODY = "body"
        const val EXTRA_ID = "id"
        const val EXTRA_HIGH_PRIORITY = "high_priority"
        const val EXTRA_DELAY_SECONDS = "delay_seconds"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.i(TAG, "Received broadcast action: $action")

        when (action) {
            ACTION_POST -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Custos Test Alert"
                val text = intent.getStringExtra(EXTRA_TEXT)
                    ?: intent.getStringExtra(EXTRA_MESSAGE)
                    ?: intent.getStringExtra(EXTRA_BODY)
                    ?: "Notification sent via ADB broadcast"
                val id = intent.getIntExtra(EXTRA_ID, 1001)
                val highPriority = intent.getBooleanExtra(EXTRA_HIGH_PRIORITY, true)
                val delaySeconds = intent.getIntExtra(EXTRA_DELAY_SECONDS, 0)

                if (delaySeconds > 0) {
                    NotificationHelper.postDelayedNotification(
                        context = context,
                        delaySeconds = delaySeconds,
                        id = id,
                        title = title,
                        text = text,
                        highPriority = highPriority
                    )
                } else {
                    NotificationHelper.postNotification(
                        context = context,
                        id = id,
                        title = title,
                        text = text,
                        highPriority = highPriority
                    )
                }
            }

            ACTION_CLEAR -> {
                val id = intent.getIntExtra(EXTRA_ID, -1)
                if (id != -1) {
                    NotificationHelper.clearNotification(context, id)
                } else {
                    NotificationHelper.clearAll(context)
                }
            }

            ACTION_PING -> {
                Log.i(TAG, "PONG: NotificationTriggerReceiver is alive and ready.")
            }

            else -> {
                Log.w(TAG, "Unhandled action: $action")
            }
        }
    }
}
