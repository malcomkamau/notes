package com.group14.notes.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/**
 * App-wide settings kept in SharedPreferences: the chosen theme, backup times, and whether the
 * notification prompt has been shown. Uses the Application context so it is safe to create from
 * a Service, a BroadcastReceiver or an Activity.
 */
class AppSettings(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** One of AppCompatDelegate.MODE_NIGHT_NO / MODE_NIGHT_YES / MODE_NIGHT_FOLLOW_SYSTEM. */
    var themeMode: Int
        get() = prefs.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        set(value) = prefs.edit().putInt(KEY_THEME_MODE, value).apply()

    /** Applies the saved theme. Called once when the app process starts. */
    fun applyTheme() = AppCompatDelegate.setDefaultNightMode(themeMode)

    fun lastBackup(userId: Long): Long = prefs.getLong("$KEY_LAST_BACKUP$userId", 0L)

    fun setLastBackup(userId: Long, time: Long) =
        prefs.edit().putLong("$KEY_LAST_BACKUP$userId", time).apply()

    /** True once we have explained and asked for the notification permission. */
    var notificationPromptShown: Boolean
        get() = prefs.getBoolean(KEY_PROMPT_SHOWN, false)
        set(value) = prefs.edit().putBoolean(KEY_PROMPT_SHOWN, value).apply()

    companion object {
        private const val PREFS_NAME = "app_settings"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_LAST_BACKUP = "last_backup_"
        private const val KEY_PROMPT_SHOWN = "notification_prompt_shown"
    }
}
