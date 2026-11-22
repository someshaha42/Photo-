package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.EmojiSourceMode
import com.example.model.ExportImageFormat
import com.example.model.ExportResolutionPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "pixelforge_settings")

data class AppSettings(
    val isDarkTheme: Boolean = true,
    val keepLocalMediaCopy: Boolean = true,
    val unlimitedLayers: Boolean = true,
    val emojiSourceMode: EmojiSourceMode = EmojiSourceMode.SYSTEM,
    val appEmojiFontId: String? = null,
    val exportFormat: ExportImageFormat = ExportImageFormat.PNG,
    val exportResolutionPreset: ExportResolutionPreset = ExportResolutionPreset.RES_2K,
    val exportCustomWidth: Int = 1080,
    val exportCustomHeight: Int = 1080,
    val previewWindowEnabled: Boolean = false
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val KEEP_LOCAL_MEDIA_COPY = booleanPreferencesKey("keep_local_media_copy")
        val UNLIMITED_LAYERS = booleanPreferencesKey("unlimited_layers")
        val EMOJI_SOURCE_MODE = stringPreferencesKey("emoji_source_mode")
        val APP_EMOJI_FONT_ID = stringPreferencesKey("app_emoji_font_id")
        val EXPORT_FORMAT = stringPreferencesKey("export_format")
        val EXPORT_RES_PRESET = stringPreferencesKey("export_resolution_preset")
        val EXPORT_CUSTOM_WIDTH = intPreferencesKey("export_custom_width")
        val EXPORT_CUSTOM_HEIGHT = intPreferencesKey("export_custom_height")
        val PREVIEW_WINDOW_ENABLED = booleanPreferencesKey("preview_window_enabled")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        val mode = runCatching {
            EmojiSourceMode.valueOf(prefs[Keys.EMOJI_SOURCE_MODE] ?: EmojiSourceMode.SYSTEM.name)
        }.getOrDefault(EmojiSourceMode.SYSTEM)
        val expFormat = runCatching {
            ExportImageFormat.valueOf(prefs[Keys.EXPORT_FORMAT] ?: ExportImageFormat.PNG.name)
        }.getOrDefault(ExportImageFormat.PNG)
        val expPreset = runCatching {
            ExportResolutionPreset.valueOf(prefs[Keys.EXPORT_RES_PRESET] ?: ExportResolutionPreset.RES_2K.name)
        }.getOrDefault(ExportResolutionPreset.RES_2K)

        AppSettings(
            isDarkTheme = prefs[Keys.DARK_THEME] ?: true,
            keepLocalMediaCopy = prefs[Keys.KEEP_LOCAL_MEDIA_COPY] ?: true,
            unlimitedLayers = prefs[Keys.UNLIMITED_LAYERS] ?: true,
            emojiSourceMode = mode,
            appEmojiFontId = prefs[Keys.APP_EMOJI_FONT_ID],
            exportFormat = expFormat,
            exportResolutionPreset = expPreset,
            exportCustomWidth = (prefs[Keys.EXPORT_CUSTOM_WIDTH] ?: 1080).coerceIn(64, 8192),
            exportCustomHeight = (prefs[Keys.EXPORT_CUSTOM_HEIGHT] ?: 1080).coerceIn(64, 8192),
            previewWindowEnabled = prefs[Keys.PREVIEW_WINDOW_ENABLED] ?: false
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

    suspend fun setPreviewWindowEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.PREVIEW_WINDOW_ENABLED] = enabled }
    }

    suspend fun setKeepLocalMediaCopy(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.KEEP_LOCAL_MEDIA_COPY] = enabled }
    }

    suspend fun setUnlimitedLayers(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.UNLIMITED_LAYERS] = enabled }
    }

    suspend fun setEmojiSource(mode: EmojiSourceMode, appEmojiFontId: String?) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.EMOJI_SOURCE_MODE] = mode.name
            if (appEmojiFontId != null) {
                prefs[Keys.APP_EMOJI_FONT_ID] = appEmojiFontId
            }
        }
    }

    suspend fun setExportPreferences(
        format: ExportImageFormat,
        preset: ExportResolutionPreset,
        customWidth: Int,
        customHeight: Int
    ) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.EXPORT_FORMAT] = format.name
            prefs[Keys.EXPORT_RES_PRESET] = preset.name
            prefs[Keys.EXPORT_CUSTOM_WIDTH] = customWidth.coerceIn(64, 8192)
            prefs[Keys.EXPORT_CUSTOM_HEIGHT] = customHeight.coerceIn(64, 8192)
        }
    }
}
