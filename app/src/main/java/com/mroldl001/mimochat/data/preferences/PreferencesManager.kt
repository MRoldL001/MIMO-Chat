package com.mroldl001.mimochat.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.mroldl001.mimochat.ui.theme.ThemeColor
import com.mroldl001.mimochat.ui.theme.ThemeMode
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "mimochat_prefs"
        private const val KEY_THEME_COLOR = "theme_color"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_API_BASE_URL = "api_base_url"
        private const val KEY_CUSTOM_SYSTEM_PROMPT = "custom_system_prompt"
        private const val KEY_SELECTED_MODEL_ID = "selected_model_id"
        private const val KEY_NOTIFICATION_PERMISSION_REQUESTED = "notification_permission_requested"
        const val DEFAULT_API_BASE_URL = "https://api.xiaomimimo.com"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getThemeColor(): ThemeColor {
        val name = prefs.getString(KEY_THEME_COLOR, ThemeColor.WHITE.name)
        return try {
            ThemeColor.valueOf(name ?: ThemeColor.WHITE.name)
        } catch (e: IllegalArgumentException) {
            ThemeColor.WHITE
        }
    }

    fun saveThemeColor(color: ThemeColor) {
        prefs.edit().putString(KEY_THEME_COLOR, color.name).apply()
    }

    fun getThemeMode(): ThemeMode {
        val name = prefs.getString(KEY_THEME_MODE, ThemeMode.FOLLOW_SYSTEM.name)
        return try {
            ThemeMode.valueOf(name ?: ThemeMode.FOLLOW_SYSTEM.name)
        } catch (e: IllegalArgumentException) {
            ThemeMode.FOLLOW_SYSTEM
        }
    }

    fun saveThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun getApiKey(): String {
        return prefs.getString(KEY_API_KEY, "") ?: ""
    }

    fun saveApiKey(key: String) {
        prefs.edit().putString(KEY_API_KEY, key).apply()
    }

    fun getApiBaseUrl(): String {
        return prefs.getString(KEY_API_BASE_URL, DEFAULT_API_BASE_URL) ?: DEFAULT_API_BASE_URL
    }

    fun saveApiBaseUrl(url: String) {
        prefs.edit().putString(KEY_API_BASE_URL, url).apply()
    }

    fun getCustomSystemPrompt(): String {
        return prefs.getString(KEY_CUSTOM_SYSTEM_PROMPT, "") ?: ""
    }

    fun saveCustomSystemPrompt(prompt: String) {
        prefs.edit().putString(KEY_CUSTOM_SYSTEM_PROMPT, prompt).apply()
    }

    fun getSelectedModelId(): String {
        return prefs.getString(KEY_SELECTED_MODEL_ID, "") ?: ""
    }

    fun saveSelectedModelId(modelId: String) {
        prefs.edit().putString(KEY_SELECTED_MODEL_ID, modelId).apply()
    }

    fun hasRequestedNotificationPermission(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, false)
    }

    fun setNotificationPermissionRequested(requested: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, requested).apply()
    }
}
