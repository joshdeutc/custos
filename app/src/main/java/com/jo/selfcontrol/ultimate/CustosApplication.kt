package com.jo.selfcontrol.ultimate

import android.app.Application
import android.content.Context
import android.util.Log

class CustosApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        appStartTime = System.currentTimeMillis()
        setupCrashHandler()
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e(TAG, "💥 UNCAUGHT CRASH in thread [${thread.name}]", throwable)
                val prefs = getSharedPreferences(PREFS_CRASH, Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean(KEY_APP_CRASHED, true)
                    .putLong(KEY_CRASH_TIMESTAMP, System.currentTimeMillis())
                    .putString(KEY_CRASH_MESSAGE, throwable.message ?: throwable.javaClass.simpleName)
                    .putString(KEY_CRASH_THREAD, thread.name)
                    .apply()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist crash info", e)
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    companion object {
        private const val TAG = "Custos.App"
        const val PREFS_CRASH = "custos_crash_info"
        const val KEY_APP_CRASHED = "key_app_crashed"
        const val KEY_CRASH_TIMESTAMP = "key_crash_timestamp"
        const val KEY_CRASH_MESSAGE = "key_crash_message"
        const val KEY_CRASH_THREAD = "key_crash_thread"

        @Volatile
        var instance: CustosApplication? = null
            private set

        @Volatile
        var appStartTime: Long = 0L
            private set

        fun wasLastRunCrashed(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_CRASH, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_APP_CRASHED, false)
        }

        fun clearCrashFlag(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_CRASH, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_APP_CRASHED, false).apply()
        }
    }
}
