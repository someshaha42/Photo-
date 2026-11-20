package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "pixelforge_settings")

data class AppSettings(
    val isDarkTheme: Boolean = true,
    val keepLocalMediaCopy: Boolean = true,
    val unlimitedLayers: Boolean = true
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val KEEP_LOCAL_MEDIA_COPY = booleanPreferencesKey("keep_local_media_copy")
        val UNLIMITED_LAYERS = booleanPreferencesKey("unlimited_layers")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            isDarkTheme = prefs[Keys.DARK_THEME] ?: true,
            keepLocalMediaCopy = prefs[Keys.KEEP_LOCAL_MEDIA_COPY] ?: true,
            unlimitedLayers = prefs[Keys.UNLIMITED_LAYERS] ?: true
        )
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.DARK_THEME] = enabled }
    }

    suspend fun toggleTheme() {
        context.settingsDataStore.edit { prefs ->
            val current = prefs[Keys.DARK_THEME] ?: true
            prefs[Keys.DARK_THEME] = !current
        }
    }

    suspend fun setKeepLocalMediaCopy(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.KEEP_LOCAL_MEDIA_COPY] = enabled }
    }

    suspend fun setUnlimitedLayers(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.UNLIMITED_LAYERS] = enabled }
    }
}
