package com.dexpro.launcher

import android.Manifest
import android.app.ActivityOptions
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Rect
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.ContextMenu
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.WindowManager as AndroidWindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dexpro.launcher.cast.CastManager
import com.dexpro.launcher.service.DesktopService
import com.dexpro.launcher.service.TaskbarAccessibilityService
import com.dexpro.launcher.ui.DesktopShortcutAdapter
import com.dexpro.launcher.ui.StartMenuAdapter
import com.dexpro.launcher.ui.TaskbarAppAdapter
import com.dexpro.launcher.utils.DisplayHelper
import com.dexpro.launcher.utils.PermissionsHelper
import com.dexpro.launcher.utils.ShizukuHelper
import com.dexpro.launcher.widget.AppWidgetHostManager
import com.dexpro.launcher.widget.WidgetGridLayout
import com.dexpro.launcher.window.WindowManager as DeXWindowManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DesktopActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_POST_NOTIFICATIONS = 2001
        private const val REQUEST_PICK_WIDGET = 3001
    }

    private lateinit var windowManager: DeXWindowManager
    private lateinit var displayHelper: DisplayHelper
    private lateinit var shizukuHelper: ShizukuHelper

    // UI components
    private lateinit var btnStart: ImageButton
    private lateinit var btnExpandTray: ImageButton
    private lateinit var btnNotifications: ImageButton
    private lateinit var tvClock: TextView
    private lateinit var taskbarAppList: RecyclerView
    private lateinit var pinnedAppsList: RecyclerView
    private lateinit var allAppsList: RecyclerView
    private lateinit var desktopShortcuts: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var startMenuView: View
    private lateinit var systemTrayView: View
    private lateinit var workspaceContainer: FrameLayout
    private lateinit var btnPower: ImageButton
    private lateinit var btnUser: ImageButton
    private lateinit var btnCast: ImageButton

    // Widget host
    private lateinit var widgetHostManager: AppWidgetHostManager
    private lateinit var widgetGrid: WidgetGridLayout
    private var pendingWidgetId: Int = -1
    private var btnAddWidget: ImageButton? = null

    // System tray controls
    private lateinit var btnWifi: ImageButton
    private lateinit var btnBluetooth: ImageButton
    private lateinit var sliderBrightness: SeekBar
    private lateinit var sliderVolume: SeekBar
    private lateinit var tileWifi: View
    private lateinit var tileBluetooth: View

    private val clockFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private var isStartMenuOpen = false
    private var isSystemTrayOpen = false
    private var installedApps: List<AppInfo> = emptyList()
    private var filteredApps: List<AppInfo> = emptyList()
    private var desktopShortcutApps: MutableList<AppInfo> = mutableListOf()

    private lateinit var startMenuAdapter: StartMenuAdapter
    private lateinit var taskbarAdapter: TaskbarAppAdapter
    private lateinit var desktopShortcutAdapter: DesktopShortcutAdapter
    private lateinit var pinnedAppsAdapter: StartMenuAdapter

    // BroadcastReceiver for taskbar updates from accessibility service
    private val taskbarReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == TaskbarAccessibilityService.ACTION_UPDATE_TASKBAR) {
                val packageNames = intent.getStringArrayListExtra(
                    TaskbarAccessibilityService.EXTRA_RUNNING_APPS
                ) ?: return
                updateRunningApps(packageNames)
            }
        }
    }

    // Display change receiver
    private val displayReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_CONFIGURATION_CHANGED) {
                windowManager.recalculateWindowBounds()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_desktop)

        // Immersive desktop mode — hide status bar for real desktop experience
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        )

        displayHelper = DisplayHelper(this)
        shizukuHelper = ShizukuHelper(this)
        windowManager = DeXWindowManager(this, findViewById(R.id.workspaceContainer))
        widgetHostManager = AppWidgetHostManager.getInstance(this)
        widgetGrid = findViewById(R.id.widgetGrid)

        bindViews()
        setupTaskbar()
        setupStartMenu()
        setupSystemTray()
        setupDesktopShortcuts()
        setupWidgetGrid()
        setupCast()
        loadInstalledApps()
        loadPinnedApps()
        loadDesktopShortcuts()
        startDesktopServiceIfPermitted()
        startClockUpdate()

        // Register taskbar broadcast receiver
        val taskbarFilter = IntentFilter(TaskbarAccessibilityService.ACTION_UPDATE_TASKBAR)
        registerReceiver(taskbarReceiver, taskbarFilter, Context.RECEIVER_NOT_EXPORTED)

        // First-run permission check
        checkPermissionsOnFirstRun()
    }

    private fun checkPermissionsOnFirstRun() {
        val prefs = getSharedPreferences("dexpro_setup", MODE_PRIVATE)
        val hasShown = prefs.getBoolean("permission_check_shown", false)
        if (hasShown) return

        val missing = PermissionsHelper.getCriticalMissing(this)
        if (missing.isNotEmpty()) {
            val toast = Toast.makeText(
                this,
                "Desktop mode needs permissions. Tap user icon in Start menu.",
                Toast.LENGTH_LONG
            )
            toast.show()
        }
        prefs.edit().putBoolean("permission_check_shown", true).apply()
    }

    private fun bindViews() {
        btnStart = findViewById(R.id.btnStart)
        btnExpandTray = findViewById(R.id.btnExpandTray)
        btnNotifications = findViewById(R.id.btnNotifications)
        tvClock = findViewById(R.id.tvClock)
        taskbarAppList = findViewById(R.id.taskbarAppList)
        pinnedAppsList = findViewById(R.id.pinnedAppsList)
        allAppsList = findViewById(R.id.allAppsList)
        desktopShortcuts = findViewById(R.id.desktopShortcuts)
        startMenuView = findViewById(R.id.startMenu)
        systemTrayView = findViewById(R.id.systemTray)
        workspaceContainer = findViewById(R.id.workspaceContainer)
        btnWifi = findViewById(R.id.btnWifi)
        btnBluetooth = findViewById(R.id.btnBluetooth)

        // System tray sliders are inside the included layout
        sliderBrightness = systemTrayView.findViewById(R.id.sliderBrightness)
        sliderVolume = systemTrayView.findViewById(R.id.sliderVolume)
        tileWifi = systemTrayView.findViewById(R.id.tileWifi)
        tileBluetooth = systemTrayView.findViewById(R.id.tileBluetooth)

        // Search
        etSearch = startMenuView.findViewById(R.id.etSearch)

        // Power button in start menu — opens SettingsActivity
        btnPower = startMenuView.findViewById(R.id.btnPower)
        btnPower.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            closeStartMenu()
        }

        // User button in start menu — opens permission guide
        btnUser = startMenuView.findViewById(R.id.btnUser)
        btnUser.setOnClickListener {
            startActivity(Intent(this, PermissionGuideActivity::class.java))
            closeStartMenu()
        }

        // Cast button in taskbar
        btnCast = findViewById(R.id.btnCast)
    }

    // ===== TASKBAR SETUP =====

    private fun setupTaskbar() {
        btnStart.setOnClickListener { toggleStartMenu() }

        taskbarAppList.layoutManager = LinearLayoutManager(
            this, LinearLayoutManager.HORIZONTAL, false
        )
        taskbarAdapter = TaskbarAppAdapter(
            onAppClick = { appInfo -> windowManager.focusWindow(appInfo.packageName) },
            onAppClose = { appInfo -> windowManager.closeWindow(appInfo.packageName) }
        )
        taskbarAppList.adapter = taskbarAdapter

        btnExpandTray.setOnClickListener { toggleSystemTray() }
        btnNotifications.setOnClickListener {
            // Expand status bar notifications
            try {
                val statusBarManager = getSystemService(Context.STATUS_BAR_SERVICE)
                val method = statusBarManager.javaClass.getMethod("expandNotificationsPanel")
                method.invoke(statusBarManager)
            } catch (e: Exception) {
                // Fallback: just toggle system tray
                toggleSystemTray()
            }
        }

        // Quick toggle for WiFi/BT in taskbar
        btnWifi.setOnClickListener { toggleWifi() }
        btnBluetooth.setOnClickListener { toggleBluetooth() }
    }

    private fun updateRunningApps(packageNames: List<String>) {
        val pm = packageManager
        val appInfos = packageNames.mapNotNull { pkg ->
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                AppInfo(
                    packageName = pkg,
                    appName = pm.getApplicationLabel(appInfo).toString(),
                    icon = pm.getApplicationIcon(appInfo)
                )
            } catch (e: Exception) {
                null
            }
        }
        taskbarAdapter.submitList(appInfos)
    }

    // ===== SYSTEM TRAY SETUP =====

    private fun setupSystemTray() {
        // Workspace switcher
        val wsIds = listOf(R.id.ws1, R.id.ws2, R.id.ws3, R.id.ws4, R.id.btnAddWorkspace)
        wsIds.forEach { id ->
            systemTrayView.findViewById<View>(id)?.setOnClickListener { view ->
                if (view.id == R.id.btnAddWorkspace) {
                    windowManager.addWorkspace()
                } else {
                    val wsNum = when (view.id) {
                        R.id.ws1 -> 1; R.id.ws2 -> 2
                        R.id.ws3 -> 3; R.id.ws4 -> 4
                        else -> 1
                    }
                    windowManager.switchWorkspace(wsNum)
                    updateWorkspaceIndicators()
                }
            }
        }

        // Quick tiles in system tray popup
        tileWifi.setOnClickListener { toggleWifi() }
        tileBluetooth.setOnClickListener { toggleBluetooth() }

        // Brightness slider
        val currentBrightness = try {
            Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (e: Exception) { 128 }
        sliderBrightness.max = 255
        sliderBrightness.progress = currentBrightness
        sliderBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    // Need WRITE_SETTINGS permission on API 23+
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (Settings.System.canWrite(this@DesktopActivity)) {
                            Settings.System.putInt(
                                contentResolver,
                                Settings.System.SCREEN_BRIGHTNESS,
                                progress
                            )
                        } else {
                            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                            intent.data = android.net.Uri.parse("package:${packageName}")
                            startActivity(intent)
                        }
                    } else {
                        Settings.System.putInt(
                            contentResolver,
                            Settings.System.SCREEN_BRIGHTNESS,
                            progress
                        )
                    }
                    // Also apply to current window
                    val layoutParams = window?.attributes
                    if (layoutParams != null) {
                        layoutParams.screenBrightness = progress / 255f
                        window?.attributes = layoutParams
                    }
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Volume slider
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        sliderVolume.max = maxVolume
        sliderVolume.progress = currentVolume
        sliderVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun toggleWifi() {
        // Cannot directly toggle WiFi on modern Android without system permissions.
        // Open WiFi settings instead.
        try {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open WiFi settings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleBluetooth() {
        try {
            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open Bluetooth settings", Toast.LENGTH_SHORT).show()
        }
    }

    // ===== START MENU SETUP =====

    private fun setupStartMenu() {
        pinnedAppsList.layoutManager = LinearLayoutManager(
            this, LinearLayoutManager.HORIZONTAL, false
        )

        allAppsList.layoutManager = GridLayoutManager(this, 4)

        // Search filtering with TextWatcher
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim().lowercase()
                filteredApps = if (query.isEmpty()) {
                    installedApps
                } else {
                    installedApps.filter {
                        it.appName.lowercase().contains(query) ||
                        it.packageName.lowercase().contains(query)
                    }
                }
                startMenuAdapter.updateApps(filteredApps)
            }
        })
    }

    private fun loadInstalledApps() {
        val pm = packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        installedApps = pm.queryIntentActivities(mainIntent, 0).map { resolveInfo ->
            AppInfo(
                packageName = resolveInfo.activityInfo.packageName,
                appName = resolveInfo.loadLabel(pm).toString(),
                icon = resolveInfo.loadIcon(pm)
            )
        }.sortedBy { it.appName.lowercase() }
        filteredApps = installedApps

        startMenuAdapter = StartMenuAdapter(
            filteredApps,
            onAppClick = { appInfo -> launchAppInWindow(appInfo) },
            onAppLongClick = { appInfo -> addDesktopShortcut(appInfo) }
        )
        allAppsList.adapter = startMenuAdapter

        // Register context menu for start menu apps
        registerForContextMenu(allAppsList)
    }

    private fun loadPinnedApps() {
        val pinned = windowManager.getPinnedApps()
        val pinnedApps = installedApps.filter { pinned.contains(it.packageName) }
        pinnedAppsAdapter = StartMenuAdapter(
            pinnedApps,
            onAppClick = { appInfo -> launchAppInWindow(appInfo) },
            onAppLongClick = { appInfo -> unpinApp(appInfo) }
        )
        pinnedAppsList.adapter = pinnedAppsAdapter
    }

    private fun loadDesktopShortcuts() {
        val prefs = getSharedPreferences("dexpro_shortcuts", MODE_PRIVATE)
        val shortcuts = prefs.getStringSet("shortcut_packages", emptySet()) ?: emptySet()
        desktopShortcutApps = installedApps.filter {
            shortcuts.contains(it.packageName)
        }.toMutableList()

        desktopShortcuts.layoutManager = GridLayoutManager(this, 5)
        desktopShortcutAdapter = DesktopShortcutAdapter(
            desktopShortcutApps,
            onAppClick = { appInfo -> launchAppInWindow(appInfo) },
            onAppLongClick = { appInfo -> removeDesktopShortcut(appInfo) }
        )
        desktopShortcuts.adapter = desktopShortcutAdapter
        registerForContextMenu(desktopShortcuts)
    }

    // ===== DESKTOP SHORTCUTS =====

    private fun setupDesktopShortcuts() {
        // Context menu registration done in loadDesktopShortcuts
    }

    // ===== WIDGET GRID =====

    private fun setupWidgetGrid() {
        // Restore previously added widgets
        val restoredViews = widgetHostManager.restoreWidgets()
        for (view in restoredViews) {
            widgetGrid.addWidget(view)
        }

        // Build "Add Widget" button
        btnAddWidget = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_input_add)
            setBackgroundColor(getColor(R.color.bg_btn_secondary))
            setOnClickListener {
                try {
                    pendingWidgetId = widgetHostManager.openWidgetPicker(this@DesktopActivity)
                } catch (e: Exception) {
                    Toast.makeText(
                        this@DesktopActivity,
                        "Cannot open widget picker: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        val addBtnParams = widgetGrid.getButtonLayoutParams()
        widgetGrid.addView(btnAddWidget, addBtnParams)
    }

    // ===== CAST =====

    private fun setupCast() {
        btnCast.setOnClickListener {
            startActivity(Intent(this, CastActivity::class.java))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_PICK_WIDGET) {
            val success = widgetHostManager.handlePickerResult(
                this, AppWidgetHostManager.REQUEST_BIND_WIDGET, resultCode, data
            )
            if (success) {
                // Remove old button, add widget, re-add button
                refreshWidgetGrid()
                Toast.makeText(this, "Widget added", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun refreshWidgetGrid() {
        widgetGrid.removeAllViews()
        val restoredViews = widgetHostManager.restoreWidgets()
        for (view in restoredViews) {
            widgetGrid.addWidget(view)
        }
        btnAddWidget = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_input_add)
            setBackgroundColor(getColor(R.color.bg_btn_secondary))
            setOnClickListener {
                try {
                    pendingWidgetId = widgetHostManager.openWidgetPicker(this@DesktopActivity)
                } catch (e: Exception) {
                    Toast.makeText(
                        this@DesktopActivity,
                        "Cannot open widget picker: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        val addBtnParams = widgetGrid.getButtonLayoutParams()
        widgetGrid.addView(btnAddWidget, addBtnParams)
    }

    private fun addDesktopShortcut(appInfo: AppInfo) {
        if (desktopShortcutApps.none { it.packageName == appInfo.packageName }) {
            desktopShortcutApps.add(appInfo)
            desktopShortcutAdapter.notifyItemInserted(desktopShortcutApps.size - 1)
            saveDesktopShortcuts()
        }
    }

    private fun removeDesktopShortcut(appInfo: AppInfo) {
        desktopShortcutApps.removeAll { it.packageName == appInfo.packageName }
        desktopShortcutAdapter.notifyDataSetChanged()
        saveDesktopShortcuts()
    }

    private fun saveDesktopShortcuts() {
        val prefs = getSharedPreferences("dexpro_shortcuts", MODE_PRIVATE)
        prefs.edit().putStringSet(
            "shortcut_packages",
            desktopShortcutApps.map { it.packageName }.toSet()
        ).apply()
    }

    // ===== CONTEXT MENU =====

    override fun onCreateContextMenu(
        menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        when (v.id) {
            R.id.allAppsList -> {
                menu.add(0, 101, 0, "Create Desktop Shortcut")
            }
            R.id.desktopShortcuts -> {
                menu.add(0, 201, 0, "Open")
                menu.add(0, 202, 0, "Remove from Desktop")
                menu.add(0, 203, 0, "App Info")
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            201 -> {
                // Open app (handled by click already, context menu info parsing needed)
                true
            }
            202 -> {
                // Remove from desktop
                val menuInfo = item.menuInfo as? android.widget.AdapterView.AdapterContextMenuInfo
                if (menuInfo != null && menuInfo.position < desktopShortcutApps.size) {
                    removeDesktopShortcut(desktopShortcutApps[menuInfo.position])
                }
                true
            }
            203 -> {
                // App info
                val menuInfo = item.menuInfo as? android.widget.AdapterView.AdapterContextMenuInfo
                if (menuInfo != null && menuInfo.position < desktopShortcutApps.size) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = android.net.Uri.parse("package:${desktopShortcutApps[menuInfo.position].packageName}")
                    startActivity(intent)
                }
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    // ===== APP LAUNCH WITH THREE-TIER FALLBACK =====

    private fun launchAppInWindow(appInfo: AppInfo) {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
                ?: return

            launchIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            )

            val screenWidth = resources.displayMetrics.widthPixels
            val screenHeight = resources.displayMetrics.heightPixels
            val windowWidth = (screenWidth * 0.6).toInt()
            val windowHeight = (screenHeight * 0.6).toInt()
            val left = (screenWidth - windowWidth) / 2
            val top = 40
            val bounds = Rect(left, top, left + windowWidth, top + windowHeight)

            // Tier 1: Shizuku Freeform
            if (shizukuHelper.isShizukuAvailable()) {
                val success = shizukuHelper.launchInFreeform(appInfo.packageName, bounds)
                if (success) {
                    windowManager.addWindow(appInfo.packageName, bounds)
                    closeStartMenu()
                    return
                }
            }

            // Tier 2: setLaunchBounds via ActivityOptions (Android N+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val options = ActivityOptions.makeBasic().apply {
                    launchBounds = bounds
                }
                startActivity(launchIntent, options.toBundle())
                windowManager.addWindow(appInfo.packageName, bounds)
                closeStartMenu()
                return
            }

            // Tier 3: Fullscreen fallback
            startActivity(launchIntent)
            windowManager.addWindow(appInfo.packageName)
            closeStartMenu()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to launch ${appInfo.appName}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun unpinApp(appInfo: AppInfo) {
        val prefs = getSharedPreferences("dexpro_pins", MODE_PRIVATE)
        val pinned = prefs.getStringSet("pinned", emptySet())?.toMutableSet() ?: mutableSetOf()
        pinned.remove(appInfo.packageName)
        prefs.edit().putStringSet("pinned", pinned).apply()
        loadPinnedApps()
    }

    // ===== TOGGLES =====

    private fun toggleStartMenu() {
        isStartMenuOpen = !isStartMenuOpen
        startMenuView.visibility = if (isStartMenuOpen) View.VISIBLE else View.GONE
        if (isStartMenuOpen) {
            isSystemTrayOpen = false
            systemTrayView.visibility = View.GONE
        }
    }

    private fun closeStartMenu() {
        isStartMenuOpen = false
        startMenuView.visibility = View.GONE
    }

    private fun toggleSystemTray() {
        isSystemTrayOpen = !isSystemTrayOpen
        systemTrayView.visibility = if (isSystemTrayOpen) View.VISIBLE else View.GONE
        if (isSystemTrayOpen) {
            isStartMenuOpen = false
            startMenuView.visibility = View.GONE
            // Refresh volume slider
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            sliderVolume.progress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        }
    }

    private fun updateWorkspaceIndicators() {
        val wsMap = mapOf(1 to R.id.ws1, 2 to R.id.ws2, 3 to R.id.ws3, 4 to R.id.ws4)
        val currentWs = windowManager.getCurrentWorkspace()
        wsMap.forEach { (ws, id) ->
            val view = systemTrayView.findViewById<TextView>(id)
            view.setTextColor(
                if (ws == currentWs) getColor(R.color.onPrimary)
                else getColor(R.color.onSurface)
            )
            view.background = if (ws == currentWs)
                getDrawable(R.drawable.bg_workspace_active)
            else
                getDrawable(R.drawable.bg_workspace)
        }
    }

    // ===== CLOCK UPDATE =====

    private fun startClockUpdate() {
        lifecycleScope.launch {
            while (true) {
                val now = Date()
                tvClock.text = clockFormat.format(now)
                delay(1000)
            }
        }
    }

    private fun startDesktopServiceIfPermitted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_POST_NOTIFICATIONS
                )
                return
            }
        }
        startDesktopService()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_POST_NOTIFICATIONS) {
            // Start service regardless of grant result — it will fall back gracefully
            startDesktopService()
        }
    }

    private fun startDesktopService() {
        val serviceIntent = Intent(this, DesktopService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    // ===== KEYBOARD SHORTCUTS (all 10) =====

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent): Boolean {
        return when {
            // 1. Win key: toggle start menu
            keyCode == android.view.KeyEvent.KEYCODE_META_LEFT ||
            keyCode == android.view.KeyEvent.KEYCODE_META_RIGHT -> {
                toggleStartMenu()
                true
            }
            // 2. Alt+Tab: cycle windows
            event.isAltPressed && keyCode == android.view.KeyEvent.KEYCODE_TAB -> {
                windowManager.cycleWindows()
                true
            }
            // 3. Escape: close start menu or minimize current window
            keyCode == android.view.KeyEvent.KEYCODE_ESCAPE -> {
                if (isStartMenuOpen) closeStartMenu()
                else if (isSystemTrayOpen) toggleSystemTray()
                else windowManager.minimizeCurrentWindow()
                true
            }
            // 4. Win+D: minimize all windows (show desktop)
            event.isMetaPressed && keyCode == android.view.KeyEvent.KEYCODE_D -> {
                windowManager.minimizeAll()
                true
            }
            // 5. Win+E: open file explorer
            event.isMetaPressed && keyCode == android.view.KeyEvent.KEYCODE_E -> {
                try {
                    val intent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_APP_FILES)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                } catch (_: Exception) {}
                true
            }
            // 6. Win+Left: snap current window left
            event.isMetaPressed && keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                windowManager.focusedPackage?.let {
                    windowManager.snapWindow(it, com.dexpro.launcher.window.SnapEdge.LEFT)
                }
                true
            }
            // 7. Win+Right: snap current window right
            event.isMetaPressed && keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                windowManager.focusedPackage?.let {
                    windowManager.snapWindow(it, com.dexpro.launcher.window.SnapEdge.RIGHT)
                }
                true
            }
            // 8. Win+Up: maximize current window
            event.isMetaPressed && keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                windowManager.focusedPackage?.let {
                    windowManager.snapWindow(it, com.dexpro.launcher.window.SnapEdge.MAXIMIZE)
                }
                true
            }
            // 9. Win+Down: restore/minimize current window
            event.isMetaPressed && keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                windowManager.focusedPackage?.let {
                    val meta = windowManager.getWindowMeta(it)
                    if (meta?.isMaximized == true) {
                        windowManager.snapWindow(it, com.dexpro.launcher.window.SnapEdge.RESTORE)
                    } else {
                        windowManager.minimizeCurrentWindow()
                    }
                }
                true
            }
            // 10. Alt+F4: close current window
            event.isAltPressed && keyCode == android.view.KeyEvent.KEYCODE_F4 -> {
                windowManager.focusedPackage?.let { windowManager.closeWindow(it) }
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    // ===== LIFECYCLE =====

    override fun onStart() {
        super.onStart()
        registerReceiver(displayReceiver, IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED))
        widgetHostManager.startListening()
    }

    override fun onStop() {
        super.onStop()
        try { unregisterReceiver(displayReceiver) } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(taskbarReceiver) } catch (_: Exception) {}
        windowManager.release()
    }
}