package com.group14.notes

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.group14.notes.auth.SessionManager
import com.group14.notes.util.AppSettings
import com.group14.notes.util.NotificationHelper

/**
 * Declared in the manifest with an intent filter for BOOT_COMPLETED (needs the normal permission
 * RECEIVE_BOOT_COMPLETED). After a restart, if the user has a remembered session and has not backed
 * up in over a week, it posts a reminder that opens Settings.
 *
 * Note: CONNECTIVITY_CHANGE can no longer be declared in the manifest (Android 7+ blocks implicit
 * broadcasts for manifest receivers), which is why connectivity is watched at runtime instead.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val session = SessionManager(context)
        if (!session.isLoggedIn()) return // temporary sessions do not survive a restart

        val last = AppSettings(context).lastBackup(session.userId)
        val overdue = last == 0L || System.currentTimeMillis() - last > REMINDER_AFTER_MS
        if (!overdue) return

        NotificationHelper.post(
            context = context,
            id = NOTIFICATION_ID,
            title = context.getString(R.string.notif_reminder_title),
            text = context.getString(R.string.notif_reminder_text),
            target = SettingsActivity::class.java
        )
    }

    companion object {
        private const val REMINDER_AFTER_MS = 7L * 24 * 60 * 60 * 1000
        private const val NOTIFICATION_ID = 1002
    }
}
