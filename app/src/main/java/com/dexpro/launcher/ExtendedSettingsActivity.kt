package com.dexpro.launcher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dexpro.launcher.databinding.ActivityExtendedSettingsBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val Context.extendedDataStore by preferencesDataStore(name = "dexpro_extended")

class ExtendedSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExtendedSettingsBinding

    companion object {
        val KEY_ANIMATION_DURATION = intPreferencesKey("animation_duration")
        val KEY_SNAP_THRESHOLD = intPreferencesKey("snap_threshold")
        val KEY_SHOW_SHADOWS = booleanPreferencesKey("show_shadows")
        val KEY_SHOW_BORDERS = booleanPreferencesKey("show_borders")
        val KEY_GRID_COLUMNS = intPreferencesKey("grid_columns")
        val KEY_GRID_ROWS = intPreferencesKey("grid_rows")
        val KEY_AUTO_TILE = booleanPreferencesKey("auto_tile")
        val KEY_GESTURE_SENSITIVITY = intPreferencesKey("gesture_sensitivity")
        val KEY_KEYBOARD_SHORTCUTS = booleanPreferencesKey("keyboard_shortcuts")
        val KEY_MOUSE_ACCELERATION = booleanPreferencesKey("mouse_acceleration")
        val KEY_PLUGIN_AUTO_UPDATE = booleanPreferencesKey("plugin_auto_update")
        val KEY_DEBUG_OVERLAY = booleanPreferencesKey("debug_overlay")

        fun start(context: Context) {
            val intent = Intent(context, ExtendedSettingsActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExtendedSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadCurrentSettings()
        setupListeners()
    }

    private fun loadCurrentSettings() {
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = extendedDataStore.data.first()
            val animationDuration = prefs[KEY_ANIMATION_DURATION] ?: 250
            val snapThreshold = prefs[KEY_SNAP_THRESHOLD] ?: 50
            val showShadows = prefs[KEY_SHOW_SHADOWS] ?: true
            val showBorders = prefs[KEY_SHOW_BORDERS] ?: true
            val gridColumns = prefs[KEY_GRID_COLUMNS] ?: 4
            val gridRows = prefs[KEY_GRID_ROWS] ?: 3
            val autoTile = prefs[KEY_AUTO_TILE] ?: false
            val gestureSensitivity = prefs[KEY_GESTURE_SENSITIVITY] ?: 50
            val keyboardShortcuts = prefs[KEY_KEYBOARD_SHORTCUTS] ?: true
            val mouseAcceleration = prefs[KEY_MOUSE_ACCELERATION] ?: true
            val pluginAutoUpdate = prefs[KEY_PLUGIN_AUTO_UPDATE] ?: true
            val debugOverlay = prefs[KEY_DEBUG_OVERLAY] ?: false

            runOnUiThread {
                binding.sliderAnimationDuration.progress = animationDuration
                binding.tvAnimationDurationValue.text = "${animationDuration}ms"
                binding.sliderSnapThreshold.progress = snapThreshold
                binding.tvSnapThresholdValue.text = "${snapThreshold}px"
                binding.swShowShadows.isChecked = showShadows
                binding.swShowBorders.isChecked = showBorders
                binding.sliderGridColumns.progress = gridColumns
                binding.tvGridColumnsValue.text = gridColumns.toString()
                binding.sliderGridRows.progress = gridRows
                binding.tvGridRowsValue.text = gridRows.toString()
                binding.swAutoTile.isChecked = autoTile
                binding.sliderGestureSensitivity.progress = gestureSensitivity
                binding.tvGestureSensitivityValue.text = gestureSensitivity.toString()
                binding.swKeyboardShortcuts.isChecked = keyboardShortcuts
                binding.swMouseAcceleration.isChecked = mouseAcceleration
                binding.swPluginAutoUpdate.isChecked = pluginAutoUpdate
                binding.swDebugOverlay.isChecked = debugOverlay
            }
        }
    }

    private fun setupListeners() {
        binding.sliderAnimationDuration.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                binding.tvAnimationDurationValue.text = "${progress}ms"
                if (fromUser) saveSettings()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.sliderSnapThreshold.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                binding.tvSnapThresholdValue.text = "${progress}px"
                if (fromUser) saveSettings()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.sliderGridColumns.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                binding.tvGridColumnsValue.text = progress.toString()
                if (fromUser) saveSettings()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.sliderGridRows.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                binding.tvGridRowsValue.text = progress.toString()
                if (fromUser) saveSettings()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.sliderGestureSensitivity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                binding.tvGestureSensitivityValue.text = progress.toString()
                if (fromUser) saveSettings()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        listOf(
            binding.swShowShadows,
            binding.swShowBorders,
            binding.swAutoTile,
            binding.swKeyboardShortcuts,
            binding.swMouseAcceleration,
            binding.swPluginAutoUpdate,
            binding.swDebugOverlay
        ).forEach { switch ->
            switch.setOnCheckedChangeListener { _, _ -> saveSettings() }
        }

        binding.btnPluginManager.setOnClickListener {
            // TODO: Open plugin manager UI
            Toast.makeText(this, "Plugin Manager (coming soon)", Toast.LENGTH_SHORT).show()
        }

        binding.btnKeymapEditor.setOnClickListener {
            // TODO: Open keymap editor
            Toast.makeText(this, "Keymap Editor (coming soon)", Toast.LENGTH_SHORT).show()
        }

        binding.btnGestureEditor.setOnClickListener {
            // TODO: Open gesture editor
            Toast.makeText(this, "Gesture Editor (coming soon)", Toast.LENGTH_SHORT).show()
        }

        binding.btnResetDefaults.setOnClickListener {
            resetToDefaults()
        }
    }

    private fun saveSettings() {
        val animationDuration = binding.sliderAnimationDuration.progress
        val snapThreshold = binding.sliderSnapThreshold.progress
        val showShadows = binding.swShowShadows.isChecked
        val showBorders = binding.swShowBorders.isChecked
        val gridColumns = binding.sliderGridColumns.progress
        val gridRows = binding.sliderGridRows.progress
        val autoTile = binding.swAutoTile.isChecked
        val gestureSensitivity = binding.sliderGestureSensitivity.progress
        val keyboardShortcuts = binding.swKeyboardShortcuts.isChecked
        val mouseAcceleration = binding.swMouseAcceleration.isChecked
        val pluginAutoUpdate = binding.swPluginAutoUpdate.isChecked
        val debugOverlay = binding.swDebugOverlay.isChecked

        runBlocking {
            extendedDataStore.edit { prefs ->
                prefs[KEY_ANIMATION_DURATION] = animationDuration
                prefs[KEY_SNAP_THRESHOLD] = snapThreshold
                prefs[KEY_SHOW_SHADOWS] = showShadows
                prefs[KEY_SHOW_BORDERS] = showBorders
                prefs[KEY_GRID_COLUMNS] = gridColumns
                prefs[KEY_GRID_ROWS] = gridRows
                prefs[KEY_AUTO_TILE] = autoTile
                prefs[KEY_GESTURE_SENSITIVITY] = gestureSensitivity
                prefs[KEY_KEYBOARD_SHORTCUTS] = keyboardShortcuts
                prefs[KEY_MOUSE_ACCELERATION] = mouseAcceleration
                prefs[KEY_PLUGIN_AUTO_UPDATE] = pluginAutoUpdate
                prefs[KEY_DEBUG_OVERLAY] = debugOverlay
            }
        }

        // Plugin notification removed due to compilation issues

        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
    }

    private fun resetToDefaults() {
        runBlocking {
            extendedDataStore.edit { prefs ->
                prefs[KEY_ANIMATION_DURATION] = 250
                prefs[KEY_SNAP_THRESHOLD] = 50
                prefs[KEY_SHOW_SHADOWS] = true
                prefs[KEY_SHOW_BORDERS] = true
                prefs[KEY_GRID_COLUMNS] = 4
                prefs[KEY_GRID_ROWS] = 3
                prefs[KEY_AUTO_TILE] = false
                prefs[KEY_GESTURE_SENSITIVITY] = 50
                prefs[KEY_KEYBOARD_SHORTCUTS] = true
                prefs[KEY_MOUSE_ACCELERATION] = true
                prefs[KEY_PLUGIN_AUTO_UPDATE] = true
                prefs[KEY_DEBUG_OVERLAY] = false
            }
        }
        loadCurrentSettings()
        Toast.makeText(this, "Reset to defaults", Toast.LENGTH_SHORT).show()
    }
}