package com.dexpro.launcher

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var settingsStore: SettingsDataStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        settingsStore = SettingsDataStore(this)

        scope.launch { loadSettings() }
        setupListeners()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun loadSettings() {
        // Window management
        val windowLimit = settingsStore.getWindowLimit()
        val slider = findViewById<SeekBar>(R.id.sliderWindowLimit)
        slider.progress = windowLimit - 1 // SeekBar is 0-11, limits are 1-12
        findViewById<TextView>(R.id.tvWindowLimitValue).text = windowLimit.toString()

        val animationSpeed = settingsStore.getAnimationSpeed()
        when (animationSpeed) {
            "fast" -> findViewById<RadioGroup>(R.id.rgAnimationSpeed).check(R.id.rbAnimFast)
            "slow" -> findViewById<RadioGroup>(R.id.rgAnimationSpeed).check(R.id.rbAnimSlow)
            else -> findViewById<RadioGroup>(R.id.rgAnimationSpeed).check(R.id.rbAnimNormal)
        }

        val aeroSnap = settingsStore.getAeroSnap()
        findViewById<SwitchCompat>(R.id.swAeroSnap).isChecked = aeroSnap

        // Desktop
        val columns = settingsStore.getDesktopColumns()
        when (columns) {
            4 -> findViewById<RadioGroup>(R.id.rgDesktopColumns).check(R.id.rbCol4)
            5 -> findViewById<RadioGroup>(R.id.rgDesktopColumns).check(R.id.rbCol5)
            6 -> findViewById<RadioGroup>(R.id.rgDesktopColumns).check(R.id.rbCol6)
        }

        val rows = settingsStore.getDesktopRows()
        when (rows) {
            3 -> findViewById<RadioGroup>(R.id.rgDesktopRows).check(R.id.rbRow3)
            4 -> findViewById<RadioGroup>(R.id.rgDesktopRows).check(R.id.rbRow4)
            5 -> findViewById<RadioGroup>(R.id.rgDesktopRows).check(R.id.rbRow5)
        }

        val iconSize = settingsStore.getIconSize()
        when (iconSize) {
            "small" -> findViewById<RadioGroup>(R.id.rgIconSize).check(R.id.rbIconSmall)
            "large" -> findViewById<RadioGroup>(R.id.rgIconSize).check(R.id.rbIconLarge)
            else -> findViewById<RadioGroup>(R.id.rgIconSize).check(R.id.rbIconMedium)
        }

        // Taskbar
        val dockPos = settingsStore.getDockPosition()
        when (dockPos) {
            "left" -> findViewById<RadioGroup>(R.id.rgDockPosition).check(R.id.rbDockLeft)
            "right" -> findViewById<RadioGroup>(R.id.rgDockPosition).check(R.id.rbDockRight)
            else -> findViewById<RadioGroup>(R.id.rgDockPosition).check(R.id.rbDockBottom)
        }

        findViewById<SwitchCompat>(R.id.swTaskbarAutoHide).isChecked =
            settingsStore.getTaskbarAutoHide()
        findViewById<SwitchCompat>(R.id.swShowSystemTray).isChecked =
            settingsStore.getShowSystemTray()

        // Behavior
        val launchMode = settingsStore.getDefaultLaunchMode()
        when (launchMode) {
            "desktop" -> findViewById<RadioGroup>(R.id.rgLaunchMode).check(R.id.rbLaunchDesktop)
            else -> findViewById<RadioGroup>(R.id.rgLaunchMode).check(R.id.rbLaunchPhone)
        }

        val backAction = settingsStore.getBackAction()
        when (backAction) {
            "exit" -> findViewById<RadioGroup>(R.id.rgBackAction).check(R.id.rbBackExit)
            else -> findViewById<RadioGroup>(R.id.rgBackAction).check(R.id.rbBackMinimize)
        }

        // Dark mode
        findViewById<SwitchCompat>(R.id.swDarkMode).isChecked = settingsStore.getDarkMode()
    }

    private fun setupListeners() {
        // Window
        findViewById<SeekBar>(R.id.sliderWindowLimit).setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    val value = progress + 1
                    findViewById<TextView>(R.id.tvWindowLimitValue).text = value.toString()
                    if (fromUser) scope.launch { settingsStore.setWindowLimit(value) }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            }
        )

        findViewById<RadioGroup>(R.id.rgAnimationSpeed).setOnCheckedChangeListener { _, id ->
            val value = when (id) {
                R.id.rbAnimFast -> "fast"
                R.id.rbAnimSlow -> "slow"
                else -> "normal"
            }
            scope.launch { settingsStore.setAnimationSpeed(value) }
        }

        findViewById<SwitchCompat>(R.id.swAeroSnap).setOnCheckedChangeListener { _, isChecked ->
            scope.launch { settingsStore.setAeroSnap(isChecked) }
        }

        // Desktop
        findViewById<RadioGroup>(R.id.rgDesktopColumns).setOnCheckedChangeListener { _, id ->
            val value = when (id) {
                R.id.rbCol5 -> 5
                R.id.rbCol6 -> 6
                else -> 4
            }
            scope.launch { settingsStore.setDesktopColumns(value) }
        }

        findViewById<RadioGroup>(R.id.rgDesktopRows).setOnCheckedChangeListener { _, id ->
            val value = when (id) {
                R.id.rbRow3 -> 3
                R.id.rbRow5 -> 5
                else -> 4
            }
            scope.launch { settingsStore.setDesktopRows(value) }
        }

        findViewById<RadioGroup>(R.id.rgIconSize).setOnCheckedChangeListener { _, id ->
            val value = when (id) {
                R.id.rbIconSmall -> "small"
                R.id.rbIconLarge -> "large"
                else -> "medium"
            }
            scope.launch { settingsStore.setIconSize(value) }
        }

        findViewById<Button>(R.id.btnWallpaper).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            startActivityForResult(intent, 200)
        }

        // Taskbar
        findViewById<RadioGroup>(R.id.rgDockPosition).setOnCheckedChangeListener { _, id ->
            val value = when (id) {
                R.id.rbDockLeft -> "left"
                R.id.rbDockRight -> "right"
                else -> "bottom"
            }
            scope.launch { settingsStore.setDockPosition(value) }
        }

        findViewById<SwitchCompat>(R.id.swTaskbarAutoHide).setOnCheckedChangeListener { _, isChecked ->
            scope.launch { settingsStore.setTaskbarAutoHide(isChecked) }
        }

        findViewById<SwitchCompat>(R.id.swShowSystemTray).setOnCheckedChangeListener { _, isChecked ->
            scope.launch { settingsStore.setShowSystemTray(isChecked) }
        }

        // Behavior
        findViewById<RadioGroup>(R.id.rgLaunchMode).setOnCheckedChangeListener { _, id ->
            val value = if (id == R.id.rbLaunchDesktop) "desktop" else "phone"
            scope.launch { settingsStore.setDefaultLaunchMode(value) }
        }

        findViewById<RadioGroup>(R.id.rgBackAction).setOnCheckedChangeListener { _, id ->
            val value = if (id == R.id.rbBackExit) "exit" else "minimize"
            scope.launch { settingsStore.setBackAction(value) }
        }

        findViewById<SwitchCompat>(R.id.swDarkMode).setOnCheckedChangeListener { _, isChecked ->
            scope.launch { settingsStore.setDarkMode(isChecked) }
            val mode = if (isChecked) {
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
            } else {
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
            }
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(mode)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 200 && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                scope.launch { settingsStore.setWallpaperUri(uri.toString()) }
            }
        }
    }
}
