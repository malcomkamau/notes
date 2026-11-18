package com.group14.notes.auth

import android.content.Context

/**
 * Two kinds of session:
 *  - Persistent ("Remember me" checked): saved in SharedPreferences, survives closing the app,
 *    expires after [SESSION_DURATION_MS].
 *  - Temporary ("Remember me" unchecked): held in memory only, so it ends when the app process ends.
 *
 * Uses the Application context so it never holds a reference to an Activity.
 */
class SessionManager(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun createSession(userId: Long, name: String, email: String, remember: Boolean) {
        clear()
        if (remember) {
            prefs.edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putLong(KEY_USER_ID, userId)
                .putString(KEY_NAME, name)
                .putString(KEY_EMAIL, email)
                .putLong(KEY_LOGIN_TIME, System.currentTimeMillis())
                .apply()
        } else {
            memLoggedIn = true
            memUserId = userId
            memName = name
            memEmail = email
        }
    }

    fun isLoggedIn(): Boolean {
        if (memLoggedIn) return true
        if (!prefs.getBoolean(KEY_LOGGED_IN, false)) return false

        val age = System.currentTimeMillis() - prefs.getLong(KEY_LOGIN_TIME, 0L)
        if (age < 0 || age > SESSION_DURATION_MS) {
            clear()
            return false
        }
        return true
    }

    val userId: Long
        get() = if (memLoggedIn) memUserId else prefs.getLong(KEY_USER_ID, -1L)

    val userName: String
        get() = if (memLoggedIn) memName else prefs.getString(KEY_NAME, "") ?: ""

    val userEmail: String
        get() = if (memLoggedIn) memEmail else prefs.getString(KEY_EMAIL, "") ?: ""

    fun clear() {
        memLoggedIn = false
        memUserId = -1L
        memName = ""
        memEmail = ""
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "session_prefs"
        private const val KEY_LOGGED_IN = "logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_NAME = "user_name"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_LOGIN_TIME = "login_time"

        // Persistent session lifetime: 1 day
        private const val SESSION_DURATION_MS = 24L * 60 * 60 * 1000

        @Volatile private var memLoggedIn = false
        @Volatile private var memUserId = -1L
        @Volatile private var memName = ""
        @Volatile private var memEmail = ""
    }
}
