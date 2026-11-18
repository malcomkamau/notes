package com.group14.notes

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.group14.notes.auth.Validators
import com.group14.notes.data.DatabaseHelper
import com.group14.notes.util.applySystemBarPadding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SignUpActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper

    private lateinit var tilName: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var tilConfirm: TextInputLayout
    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirm: TextInputEditText
    private lateinit var btnSignUp: Button
    private lateinit var progress: View

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_signup)
        findViewById<View>(R.id.root).applySystemBarPadding()

        db = DatabaseHelper.getInstance(this)

        tilName = findViewById(R.id.tilName)
        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        tilConfirm = findViewById(R.id.tilConfirm)
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirm = findViewById(R.id.etConfirm)
        btnSignUp = findViewById(R.id.btnSignUp)
        progress = findViewById(R.id.progress)

        btnSignUp.setOnClickListener { attemptSignUp() }
        findViewById<TextView>(R.id.tvGoLogin).setOnClickListener { finish() }
    }

    private fun attemptSignUp() {
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val confirm = etConfirm.text.toString()

        listOf(tilName, tilEmail, tilPassword, tilConfirm).forEach { it.error = null }

        if (!Validators.isNotEmpty(name)) {
            tilName.error = getString(R.string.error_empty_name); etName.requestFocus(); return
        }
        if (!Validators.isValidName(name)) {
            tilName.error = getString(R.string.error_name_short); etName.requestFocus(); return
        }
        if (!Validators.isNotEmpty(email)) {
            tilEmail.error = getString(R.string.error_empty_email); etEmail.requestFocus(); return
        }
        if (!Validators.isValidEmail(email)) {
            tilEmail.error = getString(R.string.error_invalid_email); etEmail.requestFocus(); return
        }
        if (!Validators.isValidPassword(password)) {
            tilPassword.error = getString(R.string.error_weak_password); etPassword.requestFocus(); return
        }
        if (!Validators.passwordsMatch(password, confirm)) {
            tilConfirm.error = getString(R.string.error_password_mismatch); etConfirm.requestFocus(); return
        }

        setLoading(true)

        executor.execute {
            var emailTaken = false
            var created = false
            try {
                if (db.emailExists(email)) {
                    emailTaken = true
                } else {
                    created = db.insertUser(name, email, password) != -1L
                }
            } catch (e: Exception) {
                created = false
            }

            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                setLoading(false)

                when {
                    emailTaken -> {
                        tilEmail.error = getString(R.string.error_email_taken)
                        etEmail.requestFocus()
                    }
                    created -> {
                        Toast.makeText(this, R.string.msg_account_created, Toast.LENGTH_LONG).show()
                        finish() // back to the login screen
                    }
                    else -> Toast.makeText(this, R.string.msg_signup_failed, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        btnSignUp.isEnabled = !loading
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
