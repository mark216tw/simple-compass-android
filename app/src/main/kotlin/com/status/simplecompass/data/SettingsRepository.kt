package com.status.simplecompass.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "compass_settings")

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class ThemePalette {
    CLASSIC,
    OCEAN,
    FOREST,
    SUNSET,
    HIGH_CONTRAST,
    DYNAMIC,
}

enum class NorthMode {
    MAGNETIC,
    TRUE,
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val themePalette: ThemePalette = ThemePalette.CLASSIC,
    val keepScreenOn: Boolean = false,
    val northMode: NorthMode = NorthMode.MAGNETIC,
    val northHapticsEnabled: Boolean = true,
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val legacyTheme = stringPreferencesKey("theme")
        val themeMode = stringPreferencesKey("theme_mode")
        val themePalette = stringPreferencesKey("theme_palette")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
        val northMode = stringPreferencesKey("north_mode")
        val northHapticsEnabled = booleanPreferencesKey("north_haptics_enabled")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw error
        }
        .map { preferences ->
            AppSettings(
                themeMode = (preferences[Keys.themeMode] ?: preferences[Keys.legacyTheme])
                    ?.let { stored -> ThemeMode.entries.find { it.name == stored } }
                    ?: ThemeMode.SYSTEM,
                themePalette = preferences[Keys.themePalette]
                    ?.let { stored -> ThemePalette.entries.find { it.name == stored } }
                    ?: ThemePalette.CLASSIC,
                keepScreenOn = preferences[Keys.keepScreenOn] ?: false,
                northMode = preferences[Keys.northMode]
                    ?.let { stored -> NorthMode.entries.find { it.name == stored } }
                    ?: NorthMode.MAGNETIC,
                northHapticsEnabled = preferences[Keys.northHapticsEnabled] ?: true,
            )
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.themeMode] = mode.name }
    }

    suspend fun setThemePalette(palette: ThemePalette) {
        context.settingsDataStore.edit { it[Keys.themePalette] = palette.name }
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.keepScreenOn] = enabled }
    }

    suspend fun setNorthMode(mode: NorthMode) {
        context.settingsDataStore.edit { it[Keys.northMode] = mode.name }
    }

    suspend fun setNorthHapticsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.northHapticsEnabled] = enabled }
    }
}
