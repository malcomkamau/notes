package com.group14.notes.auth

import java.util.regex.Pattern

/**
 * Reusable, framework-free input validation (no Android classes, so it is unit-testable with plain JUnit).
 */
object Validators {

    const val MIN_PASSWORD_LENGTH = 8
    const val MIN_NAME_LENGTH = 2

    private val EMAIL_PATTERN: Pattern =
        Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun isNotEmpty(value: String?): Boolean = !value.isNullOrBlank()

    fun isValidEmail(email: String?): Boolean =
        isNotEmpty(email) && EMAIL_PATTERN.matcher(email!!.trim()).matches()

    fun isValidName(name: String?): Boolean =
        isNotEmpty(name) && name!!.trim().length >= MIN_NAME_LENGTH

    /** At least [MIN_PASSWORD_LENGTH] characters, containing a letter and a digit. */
    fun isValidPassword(password: String?): Boolean =
        password != null &&
            password.length >= MIN_PASSWORD_LENGTH &&
            password.any { it.isLetter() } &&
            password.any { it.isDigit() }

    fun passwordsMatch(password: String?, confirm: String?): Boolean =
        password != null && password == confirm
}
