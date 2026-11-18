package com.group14.notes

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.group14.notes.auth.SessionManager
import com.group14.notes.util.AppSettings
import com.group14.notes.util.NotificationHelper
import com.group14.notes.util.applySystemBarPadding
import com.group14.notes.util.openLogin

class SettingsActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var settings: AppSettings
    private lateinit var tvLastBackup: TextView

    // Runtime permission request. Whatever the answer, the backup still runs (the notification is optional).
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { startBackup() }

    // Runtime-registered receiver: hears the service's "backup finished" broadcast while this screen is visible
    private val backupReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = onBackupFinished(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        session = SessionManager(this)
        if (!session.isLoggedIn()) {
            openLogin()
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        findViewById<View>(R.id.root).applySystemBarPadding()

        settings = AppSettings(this)
        tvLastBackup = findViewById(R.id.tvLastBackup)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnBackup).setOnClickListener { startBackupFlow() }

        setupThemeChoice()
    }

    // ─── Theme: light / dark / system default ───────────────

    private fun setupThemeChoice() {
        val group = findViewById<RadioGroup>(R.id.rgTheme)

        // Show the saved choice first, then listen for changes (so this does not trigger the listener)
        group.check(
            when (settings.themeMode) {
                AppCompatDelegate.MODE_NIGHT_NO -> R.id.rbLight
                AppCompatDelegate.MODE_NIGHT_YES -> R.id.rbDark
                else -> R.id.rbSystem
            }
        )

        group.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.rbLight -> AppCompatDelegate.MODE_NIGHT_NO
                R.id.rbDark -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            settings.themeMode = mode                     // save to SharedPreferences
            AppCompatDelegate.setDefaultNightMode(mode)   // apply now (the activity recreates itself)
        }
    }

    // ─── Backup ─────────────────────────────────────────────

    private fun startBackupFlow() {
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !NotificationHelper.canPost(this)

        if (needsPermission && !settings.notificationPromptShown) {
            // Explain first (rationale), then ask
            settings.notificationPromptShown = true
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.permission_rationale_title)
                .setMessage(R.string.permission_rationale_message)
                .setNegativeButton(R.string.action_not_now) { _, _ -> startBackup() }
                .setPositiveButton(R.string.action_allow) { _, _ ->
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                .setOnCancelListener { startBackup() }
                .show()
        } else {
            startBackup()
        }
    }

    private fun startBackup() {
        // Application context to start the service: it must not depend on this Activity staying alive
        applicationContext.startService(NoteBackupService.newIntent(applicationContext, session.userId))
        Snackbar.make(findViewById(R.id.root), R.string.backup_started, Snackbar.LENGTH_SHORT).show()
    }

    private fun onBackupFinished(intent: Intent) {
        val success = intent.getBooleanExtra(NoteBackupService.EXTRA_SUCCESS, false)
        val count = intent.getIntExtra(NoteBackupService.EXTRA_COUNT, 0)
        val message = if (success) {
            resources.getQuantityString(R.plurals.backup_notes_saved, count, count)
        } else {
            getString(R.string.backup_failed)
        }
        Snackbar.make(findViewById(R.id.root), message, Snackbar.LENGTH_LONG).show()
        updateLastBackup()
    }

    private fun updateLastBackup() {
        val time = settings.lastBackup(session.userId)
        val text = if (time == 0L) {
            getString(R.string.backup_never)
        } else {
            DateUtils.formatDateTime(
                this, time,
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH
            )
        }
        tvLastBackup.text = getString(R.string.backup_last, text)
    }

    override fun onStart() {
        super.onStart()
        if (!::settings.isInitialized) return
        ContextCompat.registerReceiver(
            this, backupReceiver,
            IntentFilter(NoteBackupService.ACTION_BACKUP_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        updateLastBackup()
    }

    override fun onStop() {
        super.onStop()
        if (!::settings.isInitialized) return
        unregisterReceiver(backupReceiver)
    }
}
