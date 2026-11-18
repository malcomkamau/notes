package com.group14.notes

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.group14.notes.data.DatabaseHelper
import com.group14.notes.security.CryptoManager
import com.group14.notes.util.AppSettings
import com.group14.notes.util.NotificationHelper
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * STARTED service (not bound): the Settings screen starts it with startService(), it does its work
 * and calls stopSelf(). Lifecycle: onCreate() -> onStartCommand() -> [work] -> stopSelf() -> onDestroy().
 * onBind() returns null because no client binds to it.
 *
 * A Service runs on the main thread, so the file work is handed to a background executor.
 *
 * The backup is the user's notes as JSON, encrypted with the same Keystore AES-GCM key and written to
 * the app's private internal storage (filesDir/backups). Private storage is not readable by other
 * apps, and the encryption means the file is not plain text even if the device is compromised.
 */
class NoteBackupService : Service() {

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val userId = intent?.getLongExtra(EXTRA_USER_ID, NO_USER) ?: NO_USER
        if (userId == NO_USER) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        executor.execute {
            val count = try {
                writeBackup(userId)
            } catch (e: Exception) {
                -1
            }
            announce(count)
            stopSelf(startId)
        }
        return START_NOT_STICKY // do not restart if the system kills it; the user can tap again
    }

    /** Returns the number of notes saved. */
    private fun writeBackup(userId: Long): Int {
        val notes = DatabaseHelper.getInstance(this).getNotes(userId)

        val array = JSONArray()
        notes.forEach {
            array.put(
                JSONObject()
                    .put("title", it.title)
                    .put("content", it.content)
                    .put("createdAt", it.createdAt)
                    .put("updatedAt", it.updatedAt)
            )
        }
        val now = System.currentTimeMillis()
        val payload = JSONObject()
            .put("version", 1)
            .put("exportedAt", now)
            .put("notes", array)
            .toString()

        val dir = File(filesDir, BACKUP_DIR).apply { mkdirs() }
        val target = File(dir, "notes_user_$userId.bak")
        val temp = File(dir, "notes_user_$userId.tmp")
        temp.writeText(CryptoManager.encrypt(payload))
        Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)

        AppSettings(this).setLastBackup(userId, now)
        return notes.size
    }

    /** Tells the UI (broadcast) and the user (notification, if permitted) how the backup went. */
    private fun announce(count: Int) {
        val success = count >= 0

        // Explicit-package broadcast: only this app can receive it
        sendBroadcast(
            Intent(ACTION_BACKUP_COMPLETE)
                .setPackage(packageName)
                .putExtra(EXTRA_SUCCESS, success)
                .putExtra(EXTRA_COUNT, count.coerceAtLeast(0))
        )

        NotificationHelper.post(
            context = this,
            id = NOTIFICATION_ID,
            title = getString(if (success) R.string.notif_backup_title else R.string.notif_backup_failed_title),
            text = if (success) resources.getQuantityString(R.plurals.backup_notes_saved, count, count)
                   else getString(R.string.notif_backup_failed_text),
            target = MainActivity::class.java
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }

    companion object {
        const val ACTION_BACKUP_COMPLETE = "com.group14.notes.action.BACKUP_COMPLETE"
        const val EXTRA_SUCCESS = "success"
        const val EXTRA_COUNT = "count"
        private const val EXTRA_USER_ID = "user_id"
        private const val NO_USER = -1L
        private const val BACKUP_DIR = "backups"
        private const val NOTIFICATION_ID = 1001

        fun newIntent(context: android.content.Context, userId: Long): Intent =
            Intent(context, NoteBackupService::class.java).putExtra(EXTRA_USER_ID, userId)
    }
}
