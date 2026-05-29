package com.dexpro.launcher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dexpro.launcher.databinding.ActivitySettingsBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore(name = "dexpro_settings")

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    companion object {
        val KEY_WINDOW_LIMIT = intPreferencesKey("window_limit")
        val KEY_DOCK_POSITION = stringPreferencesKey("dock_position")
        val KEY_DARK_MODE = intPreferencesKey("dark_mode")
        val KEY_WALLPAPER_URI = stringPreferencesKey("wallpaper_uri")
        const val REQUEST_WALLPAPER = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setResult(RESULT_OK)

        loadCurrentSettings()
        setupListeners()
    }

    private fun loadCurrentSettings() {
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = dataStore.data.first()
            val windowLimit = prefs[KEY_WINDOW_LIMIT] ?: 5
            val dockPosition = prefs[KEY_DOCK_POSITION] ?: "bottom"
            val darkMode = prefs[KEY_DARK_MODE] ?: 1

            runOnUiThread {
                binding.sliderWindowLimit.progress = windowLimit
                binding.tvWindowLimitValue.text = windowLimit.toString()
                when (dockPosition) {
                    "left" -> binding.rbDockLeft.isChecked = true
                    "right" -> binding.rbDockRight.isChecked = true
                    else -> binding.rbDockBottom.isChecked = true
                }
                binding.swDarkMode.isChecked = darkMode == 1
            }
        }
    }

    private fun setupListeners() {
        binding.sliderWindowLimit.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                binding.tvWindowLimitValue.text = progress.toString()
                if (fromUser) saveSettings()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.rgDockPosition.setOnCheckedChangeListener { _, _ -> saveSettings() }

        binding.swDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            saveSettings()
        }

        binding.btnWallpaper.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            startActivityForResult(intent, REQUEST_WALLPAPER)
        }

        binding.btnAdvancedSettings.setOnClickListener {
            ExtendedSettingsActivity.start(this)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_WALLPAPER && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                CoroutineScope(Dispatchers.IO).launch {
                    dataStore.edit { prefs ->
                        prefs[KEY_WALLPAPER_URI] = uri.toString()
                    }
                }
                Toast.makeText(this, "Wallpaper set", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveSettings() {
        val windowLimit = binding.sliderWindowLimit.progress
        val dockPosition = when {
            binding.rbDockLeft.isChecked -> "left"
            binding.rbDockRight.isChecked -> "right"
            else -> "bottom"
        }
        val darkMode = if (binding.swDarkMode.isChecked) 1 else 0

        runBlocking {
            dataStore.edit { prefs ->
                prefs[KEY_WINDOW_LIMIT] = windowLimit
                prefs[KEY_DOCK_POSITION] = dockPosition
                prefs[KEY_DARK_MODE] = darkMode
            }
        }
    }
}