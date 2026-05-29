package com.dexpro.launcher

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dexpro_settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        // Window
        val KEY_WINDOW_LIMIT = intPreferencesKey("window_limit")
        val KEY_ANIMATION_SPEED = stringPreferencesKey("animation_speed")
        val KEY_AERO_SNAP = booleanPreferencesKey("aero_snap")

        // Desktop
        val KEY_DESKTOP_COLUMNS = intPreferencesKey("desktop_columns")
        val KEY_DESKTOP_ROWS = intPreferencesKey("desktop_rows")
        val KEY_WALLPAPER_URI = stringPreferencesKey("wallpaper_uri")
        val KEY_ICON_SIZE = stringPreferencesKey("icon_size")

        // Taskbar
        val KEY_DOCK_POSITION = stringPreferencesKey("dock_position")
        val KEY_TASKBAR_AUTO_HIDE = booleanPreferencesKey("taskbar_auto_hide")
        val KEY_SHOW_SYSTEM_TRAY = booleanPreferencesKey("show_system_tray")

        // Behavior
        val KEY_DEFAULT_LAUNCH_MODE = stringPreferencesKey("default_launch_mode")
        val KEY_BACK_ACTION = stringPreferencesKey("back_action")
        val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")

        // Defaults
        const val DEFAULT_WINDOW_LIMIT = 8
        const val DEFAULT_ANIMATION_SPEED = "normal"
        const val DEFAULT_AERO_SNAP = true
        const val DEFAULT_DESKTOP_COLUMNS = 4
        const val DEFAULT_DESKTOP_ROWS = 4
        const val DEFAULT_ICON_SIZE = "medium"
        const val DEFAULT_DOCK_POSITION = "bottom"
        const val DEFAULT_LAUNCH_MODE = "phone"
        const val DEFAULT_BACK_ACTION = "minimize"
    }

    suspend fun getWindowLimit(): Int =
        context.dataStore.data.map { it[KEY_WINDOW_LIMIT] ?: DEFAULT_WINDOW_LIMIT }.first()

    suspend fun setWindowLimit(value: Int) {
        context.dataStore.edit { it[KEY_WINDOW_LIMIT] = value }
    }

    suspend fun getAnimationSpeed(): String =
        context.dataStore.data.map { it[KEY_ANIMATION_SPEED] ?: DEFAULT_ANIMATION_SPEED }.first()

    suspend fun setAnimationSpeed(value: String) {
        context.dataStore.edit { it[KEY_ANIMATION_SPEED] = value }
    }

    suspend fun getAeroSnap(): Boolean =
        context.dataStore.data.map { it[KEY_AERO_SNAP] ?: DEFAULT_AERO_SNAP }.first()

    suspend fun setAeroSnap(value: Boolean) {
        context.dataStore.edit { it[KEY_AERO_SNAP] = value }
    }

    suspend fun getDesktopColumns(): Int =
        context.dataStore.data.map { it[KEY_DESKTOP_COLUMNS] ?: DEFAULT_DESKTOP_COLUMNS }.first()

    suspend fun setDesktopColumns(value: Int) {
        context.dataStore.edit { it[KEY_DESKTOP_COLUMNS] = value }
    }

    suspend fun getDesktopRows(): Int =
        context.dataStore.data.map { it[KEY_DESKTOP_ROWS] ?: DEFAULT_DESKTOP_ROWS }.first()

    suspend fun setDesktopRows(value: Int) {
        context.dataStore.edit { it[KEY_DESKTOP_ROWS] = value }
    }

    suspend fun getWallpaperUri(): String =
        context.dataStore.data.map { it[KEY_WALLPAPER_URI] ?: "" }.first()

    suspend fun setWallpaperUri(value: String) {
        context.dataStore.edit { it[KEY_WALLPAPER_URI] = value }
    }

    suspend fun getIconSize(): String =
        context.dataStore.data.map { it[KEY_ICON_SIZE] ?: DEFAULT_ICON_SIZE }.first()

    suspend fun setIconSize(value: String) {
        context.dataStore.edit { it[KEY_ICON_SIZE] = value }
    }

    suspend fun getDockPosition(): String =
        context.dataStore.data.map { it[KEY_DOCK_POSITION] ?: DEFAULT_DOCK_POSITION }.first()

    suspend fun setDockPosition(value: String) {
        context.dataStore.edit { it[KEY_DOCK_POSITION] = value }
    }

    suspend fun getTaskbarAutoHide(): Boolean =
        context.dataStore.data.map { it[KEY_TASKBAR_AUTO_HIDE] ?: false }.first()

    suspend fun setTaskbarAutoHide(value: Boolean) {
        context.dataStore.edit { it[KEY_TASKBAR_AUTO_HIDE] = value }
    }

    suspend fun getShowSystemTray(): Boolean =
        context.dataStore.data.map { it[KEY_SHOW_SYSTEM_TRAY] ?: true }.first()

    suspend fun setShowSystemTray(value: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_SYSTEM_TRAY] = value }
    }

    suspend fun getDefaultLaunchMode(): String =
        context.dataStore.data.map { it[KEY_DEFAULT_LAUNCH_MODE] ?: DEFAULT_LAUNCH_MODE }.first()

    suspend fun setDefaultLaunchMode(value: String) {
        context.dataStore.edit { it[KEY_DEFAULT_LAUNCH_MODE] = value }
    }

    suspend fun getBackAction(): String =
        context.dataStore.data.map { it[KEY_BACK_ACTION] ?: DEFAULT_BACK_ACTION }.first()

    suspend fun setBackAction(value: String) {
        context.dataStore.edit { it[KEY_BACK_ACTION] = value }
    }

    suspend fun getDarkMode(): Boolean =
        context.dataStore.data.map { it[KEY_DARK_MODE] ?: true }.first()

    suspend fun setDarkMode(value: Boolean) {
        context.dataStore.edit { it[KEY_DARK_MODE] = value }
    }
}
