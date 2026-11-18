package com.group14.notes

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.group14.notes.auth.SessionManager
import com.group14.notes.auth.Validators
import com.group14.notes.data.DatabaseHelper
import com.group14.notes.util.applySystemBarPadding
import com.group14.notes.util.openLogin
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Create or edit one note. Launched with an explicit intent from the list or the add button. */
class NoteEditorActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var db: DatabaseHelper

    private lateinit var tilTitle: TextInputLayout
    private lateinit var etTitle: TextInputEditText
    private lateinit var etContent: TextInputEditText
    private lateinit var btnSave: Button
    private lateinit var btnDelete: ImageButton

    private var noteId: Long = NEW_NOTE
    private val isEditing get() = noteId != NEW_NOTE

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        session = SessionManager(this)
        if (!session.isLoggedIn()) {
            openLogin()
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_note_editor)
        findViewById<View>(R.id.root).applySystemBarPadding()

        db = DatabaseHelper.getInstance(this)
        noteId = intent.getLongExtra(EXTRA_NOTE_ID, NEW_NOTE)

        tilTitle = findViewById(R.id.tilTitle)
        etTitle = findViewById(R.id.etTitle)
        etContent = findViewById(R.id.etContent)
        btnSave = findViewById(R.id.btnSave)
        btnDelete = findViewById(R.id.btnDelete)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnShare).setOnClickListener { shareNote() }
        btnDelete.setOnClickListener { confirmDelete() }
        btnSave.setOnClickListener { saveNote() }

        if (isEditing) {
            findViewById<TextView>(R.id.tvEditorTitle).setText(R.string.editor_edit_title)
            btnDelete.visibility = View.VISIBLE
            if (savedInstanceState == null) loadNote()
        }
    }

    private fun loadNote() {
        val userId = session.userId
        executor.execute {
            val note = try {
                db.getNote(userId, noteId)
            } catch (e: Exception) {
                null
            }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                if (note == null) {
                    Toast.makeText(this, R.string.msg_load_note_failed, Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    etTitle.setText(note.title)
                    etContent.setText(note.content)
                }
            }
        }
    }

    private fun saveNote() {
        val title = etTitle.text.toString().trim()
        val content = etContent.text.toString()

        tilTitle.error = null
        if (!Validators.isNotEmpty(title)) {
            tilTitle.error = getString(R.string.error_empty_title)
            etTitle.requestFocus()
            return
        }

        btnSave.isEnabled = false
        val userId = session.userId

        executor.execute {
            val ok = try {
                if (isEditing) {
                    db.updateNote(userId, noteId, title, content) > 0
                } else {
                    db.insertNote(userId, title, content) != -1L
                }
            } catch (e: Exception) {
                false
            }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                if (ok) {
                    Toast.makeText(this, R.string.msg_note_saved, Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    btnSave.isEnabled = true
                    Toast.makeText(this, R.string.msg_save_note_failed, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /** Implicit intent: any app that accepts plain text can receive the note. */
    private fun shareNote() {
        val title = etTitle.text.toString().trim()
        val content = etContent.text.toString().trim()

        if (!Validators.isNotEmpty(title) && !Validators.isNotEmpty(content)) {
            Toast.makeText(this, R.string.msg_nothing_to_share, Toast.LENGTH_SHORT).show()
            return
        }

        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, if (content.isEmpty()) title else "$title\n\n$content")
        }
        startActivity(Intent.createChooser(send, getString(R.string.share_chooser_title)))
    }

    private fun confirmDelete() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_delete_title)
            .setMessage(R.string.dialog_delete_message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                val userId = session.userId
                executor.execute {
                    try { db.deleteNote(userId, noteId) } catch (e: Exception) { /* ignored */ }
                    runOnUiThread { if (!isFinishing && !isDestroyed) finish() }
                }
            }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }

    companion object {
        private const val EXTRA_NOTE_ID = "note_id"
        const val NEW_NOTE = -1L

        /** Build the explicit intent that opens the editor (new note by default). */
        fun newIntent(context: Context, noteId: Long = NEW_NOTE): Intent =
            Intent(context, NoteEditorActivity::class.java).putExtra(EXTRA_NOTE_ID, noteId)
    }
}
