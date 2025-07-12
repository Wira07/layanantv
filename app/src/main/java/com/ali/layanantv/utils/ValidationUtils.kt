package com.ali.layanantv.utils

object ValidationUtils {
    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    fun isValidName(name: String): Boolean {
        return name.trim().length >= 2
    }

    fun isAdminEmail(email: String): Boolean {
        return email.endsWith(Constants.ADMIN_EMAIL_DOMAIN)
    }
}