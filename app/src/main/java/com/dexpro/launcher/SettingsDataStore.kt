package com.dexpro.launcher

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "dexpro_settings")

class SettingsDataStore private constructor(private val context: Context) {
    val dataStore: DataStore<Preferences> get() = context.settingsDataStore

    companion object {
        @Volatile
        private var INSTANCE: SettingsDataStore? = null

        fun getInstance(context: Context): SettingsDataStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsDataStore(context.applicationContext).also { INSTANCE = it }
            }
        }

        val KEY_FREEFORM_LIMIT = intPreferencesKey("freeform_limit")
        val KEY_DOCK_POSITION = intPreferencesKey("dock_position")
        val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
        val KEY_WALLPAPER_URI = stringPreferencesKey("wallpaper_uri")
    }
}