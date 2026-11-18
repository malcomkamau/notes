package com.group14.notes

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.group14.notes.auth.SessionManager
import com.group14.notes.auth.Validators
import com.group14.notes.data.DatabaseHelper
import com.group14.notes.util.applySystemBarPadding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class LoginActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var db: DatabaseHelper

    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var cbRemember: CheckBox
    private lateinit var btnLogin: Button
    private lateinit var progress: View

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Already signed in? Skip the login screen.
        session = SessionManager(this)
        if (session.isLoggedIn()) {
            navigateToMain()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        findViewById<View>(R.id.root).applySystemBarPadding()

        db = DatabaseHelper.getInstance(this)

        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        cbRemember = findViewById(R.id.cbRemember)
        btnLogin = findViewById(R.id.btnLogin)
        progress = findViewById(R.id.progress)

        btnLogin.setOnClickListener { attemptLogin() }
        findViewById<TextView>(R.id.tvGoSignUp).setOnClickListener {
            // Explicit intent: login -> sign up
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    private fun attemptLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val remember = cbRemember.isChecked

        tilEmail.error = null
        tilPassword.error = null

        // Validate inputs before touching the database
        if (!Validators.isNotEmpty(email)) {
            tilEmail.error = getString(R.string.error_empty_email)
            etEmail.requestFocus()
            return
        }
        if (!Validators.isValidEmail(email)) {
            tilEmail.error = getString(R.string.error_invalid_email)
            etEmail.requestFocus()
            return
        }
        if (!Validators.isNotEmpty(password)) {
            tilPassword.error = getString(R.string.error_empty_password)
            etPassword.requestFocus()
            return
        }

        setLoading(true)

        executor.execute {
            val user = try {
                db.authenticate(email, password)
            } catch (e: Exception) {
                null
            }

            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                setLoading(false)

                if (user != null) {
                    session.createSession(user.id, user.name, user.email, remember)
                    Toast.makeText(this, getString(R.string.msg_welcome, user.name), Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
                    etPassword.text?.clear()
                    tilPassword.error = getString(R.string.msg_invalid_credentials)
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !loading
    }

    private fun navigateToMain() {
        // Explicit intent: login -> main, clearing the back stack
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
