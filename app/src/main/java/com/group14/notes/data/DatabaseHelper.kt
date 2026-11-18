package com.group14.notes.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.group14.notes.auth.PasswordHasher
import com.group14.notes.security.CryptoManager

data class User(val id: Long, val name: String, val email: String)

/**
 * SQLite helper. Every query uses ? placeholders (parameterized) - user input is never
 * concatenated into SQL.
 *
 * Schema (version 1):
 *   users(id, name, email UNIQUE, password_hash, salt)
 *   notes(id, user_id -> users.id, title, content, created_at, updated_at)
 *     title/content hold AES-GCM ciphertext; encryption/decryption happens in the note methods
 *     below, so callers only ever deal with plain text and nothing is stored unencrypted.
 *   Every note query is scoped by user_id, so one user can never read or change another's notes.
 */
class DatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_USERS (
                $COL_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_USER_NAME TEXT NOT NULL,
                $COL_USER_EMAIL TEXT NOT NULL UNIQUE COLLATE NOCASE,
                $COL_USER_HASH TEXT NOT NULL,
                $COL_USER_SALT TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE $TABLE_NOTES (
                $COL_NOTE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NOTE_USER_ID INTEGER NOT NULL
                    REFERENCES $TABLE_USERS($COL_USER_ID) ON DELETE CASCADE,
                $COL_NOTE_TITLE TEXT NOT NULL,
                $COL_NOTE_CONTENT TEXT NOT NULL,
                $COL_NOTE_CREATED INTEGER NOT NULL,
                $COL_NOTE_UPDATED INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_notes_user ON $TABLE_NOTES($COL_NOTE_USER_ID)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Version 1: nothing to migrate yet.
    }

    // ─── Users / authentication ─────────────────────────────

    fun emailExists(email: String): Boolean =
        readableDatabase.query(
            TABLE_USERS, arrayOf(COL_USER_ID),
            "$COL_USER_EMAIL = ?", arrayOf(normalize(email)),
            null, null, null, "1"
        ).use { it.moveToFirst() }

    /** Returns the new user's id, or -1 if the insert failed (e.g. duplicate email). */
    fun insertUser(name: String, email: String, password: String): Long {
        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hash(password, salt)

        return writableDatabase.insert(TABLE_USERS, null, ContentValues().apply {
            put(COL_USER_NAME, name.trim())
            put(COL_USER_EMAIL, normalize(email))
            put(COL_USER_HASH, PasswordHasher.encode(hash))
            put(COL_USER_SALT, PasswordHasher.encode(salt))
        })
    }

    /** Returns the user if the credentials match, otherwise null. */
    fun authenticate(email: String, password: String): User? {
        readableDatabase.query(
            TABLE_USERS,
            arrayOf(COL_USER_ID, COL_USER_NAME, COL_USER_EMAIL, COL_USER_HASH, COL_USER_SALT),
            "$COL_USER_EMAIL = ?", arrayOf(normalize(email)),
            null, null, null
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null

            val matches = PasswordHasher.verify(
                password,
                storedHash = cursor.getString(3),
                storedSalt = cursor.getString(4)
            )
            return if (matches) User(cursor.getLong(0), cursor.getString(1), cursor.getString(2)) else null
        }
    }

    // ─── Notes (CRUD, encrypted at rest) ─────────────────────

    /** Create. Returns the new note id, or -1 on failure. */
    fun insertNote(
        userId: Long,
        title: String,
        content: String,
        createdAt: Long = System.currentTimeMillis(),
        updatedAt: Long = createdAt
    ): Long =
        writableDatabase.insert(TABLE_NOTES, null, ContentValues().apply {
            put(COL_NOTE_USER_ID, userId)
            put(COL_NOTE_TITLE, CryptoManager.encrypt(title))
            put(COL_NOTE_CONTENT, CryptoManager.encrypt(content))
            put(COL_NOTE_CREATED, createdAt)
            put(COL_NOTE_UPDATED, updatedAt)
        })

    /** Read all of a user's notes, newest first. */
    fun getNotes(userId: Long): List<Note> {
        val notes = mutableListOf<Note>()
        readableDatabase.query(
            TABLE_NOTES, NOTE_COLUMNS,
            "$COL_NOTE_USER_ID = ?", arrayOf(userId.toString()),
            null, null, "$COL_NOTE_UPDATED DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) notes.add(cursor.toNote())
        }
        return notes
    }

    /** Read one note (only if it belongs to the user). */
    fun getNote(userId: Long, noteId: Long): Note? =
        readableDatabase.query(
            TABLE_NOTES, NOTE_COLUMNS,
            "$COL_NOTE_ID = ? AND $COL_NOTE_USER_ID = ?",
            arrayOf(noteId.toString(), userId.toString()),
            null, null, null
        ).use { cursor -> if (cursor.moveToFirst()) cursor.toNote() else null }

    /** Update. Returns the number of rows changed (0 if not found / not the user's). */
    fun updateNote(userId: Long, noteId: Long, title: String, content: String): Int =
        writableDatabase.update(
            TABLE_NOTES,
            ContentValues().apply {
                put(COL_NOTE_TITLE, CryptoManager.encrypt(title))
                put(COL_NOTE_CONTENT, CryptoManager.encrypt(content))
                put(COL_NOTE_UPDATED, System.currentTimeMillis())
            },
            "$COL_NOTE_ID = ? AND $COL_NOTE_USER_ID = ?",
            arrayOf(noteId.toString(), userId.toString())
        )

    /** Delete. Returns the number of rows removed. */
    fun deleteNote(userId: Long, noteId: Long): Int =
        writableDatabase.delete(
            TABLE_NOTES,
            "$COL_NOTE_ID = ? AND $COL_NOTE_USER_ID = ?",
            arrayOf(noteId.toString(), userId.toString())
        )

    private fun Cursor.toNote(): Note {
        val (title, content) = try {
            CryptoManager.decrypt(getString(1)) to CryptoManager.decrypt(getString(2))
        } catch (e: Exception) {
            // Key lost or data tampered with: show a placeholder instead of crashing
            "Unreadable note" to ""
        }
        return Note(getLong(0), title, content, getLong(3), getLong(4))
    }

    private fun normalize(email: String) = email.trim().lowercase()

    companion object {
        private const val DATABASE_NAME = "notes.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_USERS = "users"
        const val COL_USER_ID = "id"
        const val COL_USER_NAME = "name"
        const val COL_USER_EMAIL = "email"
        const val COL_USER_HASH = "password_hash"
        const val COL_USER_SALT = "salt"

        const val TABLE_NOTES = "notes"
        const val COL_NOTE_ID = "id"
        const val COL_NOTE_USER_ID = "user_id"
        const val COL_NOTE_TITLE = "title"
        const val COL_NOTE_CONTENT = "content"
        const val COL_NOTE_CREATED = "created_at"
        const val COL_NOTE_UPDATED = "updated_at"

        private val NOTE_COLUMNS = arrayOf(
            COL_NOTE_ID, COL_NOTE_TITLE, COL_NOTE_CONTENT, COL_NOTE_CREATED, COL_NOTE_UPDATED
        )

        @Volatile private var instance: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper =
            instance ?: synchronized(this) {
                instance ?: DatabaseHelper(context).also { instance = it }
            }
    }
}
