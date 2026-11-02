package com.example.foxtrack.data.local

import android.content.Context
import com.example.foxtrack.BuildConfig
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemePreference(val stored: String) {
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromStored(value: String?): ThemePreference? = entries.firstOrNull { it.stored == value }
    }
}

enum class AppLanguage(val tag: String) {
    RU("ru"),
    EN("en");

    companion object {
        fun fromTag(value: String?): AppLanguage? = entries.firstOrNull { it.tag == value }
    }
}

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private val themeState = MutableStateFlow(ThemePreference.fromStored(prefs.getString(KEY_THEME, null)))
    private val languageState = MutableStateFlow(
        AppLanguage.fromTag(prefs.getString(KEY_LANGUAGE, null))
            ?: AppLanguage.fromTag(Locale.getDefault().language)
            ?: AppLanguage.RU,
    )
    private val apiKeyState = MutableStateFlow(prefs.getString(KEY_API_KEY, "").orEmpty())

    val theme: StateFlow<ThemePreference?> = themeState.asStateFlow()
    val language: StateFlow<AppLanguage> = languageState.asStateFlow()
    val apiKey: StateFlow<String> = apiKeyState.asStateFlow()

    fun setTheme(value: ThemePreference) {
        prefs.edit().putString(KEY_THEME, value.stored).apply()
        themeState.value = value
    }

    fun setLanguage(value: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, value.tag).apply()
        languageState.value = value
    }

    fun setApiKey(value: String) {
        val trimmed = value.trim()
        prefs.edit().putString(KEY_API_KEY, trimmed).apply()
        apiKeyState.value = trimmed
    }

    fun effectiveApiKey(): String = apiKeyState.value.ifEmpty { BuildConfig.TGDB_API_KEY }.trim()

    fun saveSession(userId: Long, days: Int = SESSION_DAYS) {
        val expires = System.currentTimeMillis() + days * DAY_MS
        prefs.edit().putLong(KEY_SESSION_USER, userId).putLong(KEY_SESSION_EXPIRES, expires).apply()
    }

    fun sessionUserId(): Long? {
        val id = prefs.getLong(KEY_SESSION_USER, NO_SESSION)
        val expires = prefs.getLong(KEY_SESSION_EXPIRES, 0L)
        return if (id != NO_SESSION && expires > System.currentTimeMillis()) id else null
    }

    fun clearSession() {
        prefs.edit().remove(KEY_SESSION_USER).remove(KEY_SESSION_EXPIRES).apply()
    }

    companion object {
        private const val FILE = "foxtrack_settings"
        private const val KEY_THEME = "theme"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_API_KEY = "tgdb_api_key"
        private const val KEY_SESSION_USER = "session_user_id"
        private const val KEY_SESSION_EXPIRES = "session_expires_at"
        private const val NO_SESSION = -1L
        private const val SESSION_DAYS = 30
        private const val DAY_MS = 24L * 60L * 60L * 1000L

        fun readLanguage(context: Context): AppLanguage {
            val stored = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_LANGUAGE, null)
            return AppLanguage.fromTag(stored)
                ?: AppLanguage.fromTag(Locale.getDefault().language)
                ?: AppLanguage.RU
        }
    }
}
