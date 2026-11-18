package com.group14.notes

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.group14.notes.auth.SessionManager
import com.group14.notes.util.applySystemBarPadding
import com.group14.notes.util.openLogin

class MainActivity : AppCompatActivity() {

    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // No valid session -> back to login
        session = SessionManager(this)
        if (!session.isLoggedIn()) {
            openLogin()
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.main).applySystemBarPadding()

        val nav = findViewById<BottomNavigationView>(R.id.bottomNav)
        nav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_notes -> show(NotesFragment())
                R.id.nav_account -> show(AccountFragment())
                else -> return@setOnItemSelectedListener false
            }
            true
        }
        if (savedInstanceState == null) nav.selectedItemId = R.id.nav_notes

        // Round add button: explicit intent to the editor in "new note" mode
        findViewById<FloatingActionButton>(R.id.fabAddNote).setOnClickListener {
            startActivity(NoteEditorActivity.newIntent(this))
        }
    }

    // Catches a persistent session expiring while the app sat in the background
    override fun onResume() {
        super.onResume()
        if (::session.isInitialized && !session.isLoggedIn()) {
            openLogin()
            finish()
        }
    }

    private fun show(fragment: Fragment) {
        supportFragmentManager.commit { replace(R.id.fragmentContainer, fragment) }
    }
}
