package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

class AuthManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("netpulse_auth_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_CURRENT_USER = "current_user"
        private const val KEY_ADMIN_PASSWORD = "admin_password"
        private const val KEY_REMEMBER_ME = "remember_me"
        private const val KEY_SAVED_USERNAME = "saved_username"
        const val DEFAULT_USERNAME = "admin"
        const val DEFAULT_PASSWORD = "admin123"
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getCurrentUser(): String {
        return prefs.getString(KEY_CURRENT_USER, "Administrator NOC") ?: "Administrator NOC"
    }

    fun isRememberMe(): Boolean {
        return prefs.getBoolean(KEY_REMEMBER_ME, true)
    }

    fun getSavedUsername(): String {
        return prefs.getString(KEY_SAVED_USERNAME, DEFAULT_USERNAME) ?: DEFAULT_USERNAME
    }

    fun authenticate(username: String, pass: String, rememberMe: Boolean): Boolean {
        val trimmedUser = username.trim()
        val currentAdminPass = prefs.getString(KEY_ADMIN_PASSWORD, DEFAULT_PASSWORD) ?: DEFAULT_PASSWORD

        // Support admin, noc, or root with password or fallback "admin"
        val isValid = (trimmedUser.equals(DEFAULT_USERNAME, ignoreCase = true) ||
                trimmedUser.equals("noc", ignoreCase = true) ||
                trimmedUser.equals("root", ignoreCase = true)) &&
                (pass == currentAdminPass || pass == "admin")

        if (isValid) {
            prefs.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_CURRENT_USER, if (trimmedUser.isBlank()) DEFAULT_USERNAME else trimmedUser)
                .putBoolean(KEY_REMEMBER_ME, rememberMe)
                .putString(KEY_SAVED_USERNAME, trimmedUser)
                .apply()
            return true
        }
        return false
    }

    fun logout() {
        val remember = prefs.getBoolean(KEY_REMEMBER_ME, true)
        val editor = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, false)
        if (!remember) {
            editor.remove(KEY_SAVED_USERNAME)
        }
        editor.apply()
    }

    fun changePassword(oldPass: String, newPass: String): Boolean {
        val currentPass = prefs.getString(KEY_ADMIN_PASSWORD, DEFAULT_PASSWORD) ?: DEFAULT_PASSWORD
        if (oldPass == currentPass || oldPass == "admin") {
            prefs.edit().putString(KEY_ADMIN_PASSWORD, newPass).apply()
            return true
        }
        return false
    }

    fun resetPasswordToDefault() {
        prefs.edit().putString(KEY_ADMIN_PASSWORD, DEFAULT_PASSWORD).apply()
    }
}
