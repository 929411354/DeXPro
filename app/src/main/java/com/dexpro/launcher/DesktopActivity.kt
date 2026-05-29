package com.dexpro.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dexpro.launcher.ui.StartMenuAdapter
import com.dexpro.launcher.window.WindowDecorator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DesktopActivity : AppCompatActivity() {

    private lateinit var workspaceContainer: FrameLayout
    private lateinit var desktopShortcuts: RecyclerView
    private lateinit var taskbarRoot: LinearLayout
    private lateinit var startMenuView: View
    private lateinit var systemTrayView: View
    private lateinit var btnStart: ImageButton
    private lateinit var taskbarAppList: RecyclerView
    private lateinit var tvClock: TextView
    private lateinit var startMenuGrid: RecyclerView
    private lateinit var etStartMenuSearch: android.widget.EditText

    private lateinit var windowManager: DeXWindowManager
    private lateinit var permHelper: PermissionsHelper
    private lateinit var settingsStore: SettingsDataStore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val clockFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormatter = SimpleDateFormat("MM/dd", Locale.getDefault())

    private var startMenuAppAdapter: StartMenuAdapter? = null
    private var allApps: List<AppInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Immersive mode - hide system status bar and nav bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContentView(R.layout.activity_desktop)

        settingsStore = SettingsDataStore(this)
        permHelper = PermissionsHelper(this)

        workspaceContainer = findViewById(R.id.workspaceContainer)
        desktopShortcuts = findViewById(R.id.desktopShortcuts)
        taskbarRoot = findViewById(R.id.taskbarRoot)
        startMenuView = findViewById(R.id.startMenu)
        systemTrayView = findViewById(R.id.systemTray)

        windowManager = DeXWindowManager(this, workspaceContainer)

        scope.launch {
            windowManager.windowLimit = settingsStore.getWindowLimit()
        }

        initTaskbar()
        initStartMenu()
        initSystemTray()
        initDesktop()
        startClock()
        setupBackBehavior()
        loadWallpaper()
    }

    override fun onResume() {
        super.onResume()
        refreshBatteryInfo()
        refreshNetworkInfo()
    }

    override fun onDestroy() {
        scope.cancel()
        windowManager.release()
        super.onDestroy()
    }

    private fun initTaskbar() {
        btnStart = findViewById(R.id.btnStart)
        btnStart.setOnClickListener {
            toggleStartMenu()
        }

        // Taskbar app list
        taskbarAppList = findViewById(R.id.taskbarAppList)
        taskbarAppList.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)

        // Running apps adapter
        val taskAdapter = TaskbarAppAdapter(
            onAppClick = { pkg -> windowManager.focusWindow(pkg) },
            onAppClose = { pkg -> windowManager.closeWindow(pkg) }
        )
        taskbarAppList.adapter = taskAdapter

        // Observe window list
        windowManager.windowList.observe(this) { windowList ->
            taskAdapter.updateApps(windowList)
        }

        // Settings button in tray
        findViewById<ImageButton>(R.id.btnSettingsQuick).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<ImageButton>(R.id.btnPermissionsQuick).setOnClickListener {
            startActivity(Intent(this, PermissionGuideActivity::class.java))
        }
    }

    private fun initStartMenu() {
        startMenuView.visibility = View.GONE

        // All apps
        allApps = getInstalledApps()
        startMenuGrid = startMenuView.findViewById(R.id.startMenuGrid)
        startMenuGrid.layoutManager = GridLayoutManager(this, 4)

        startMenuAppAdapter = StartMenuAdapter(allApps, { app ->
            launchAppInWindow(app)
            startMenuView.visibility = View.GONE
        }, { app ->
            showAppContextMenu(app)
        })
        startMenuGrid.adapter = startMenuAppAdapter

        // Search in start menu
        etStartMenuSearch = startMenuView.findViewById(R.id.etStartMenuSearch)
        etStartMenuSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val q = s?.toString() ?: ""
                val filtered = if (q.isBlank()) allApps
                else allApps.filter { it.appName.contains(q, ignoreCase = true) }
                startMenuAppAdapter?.updateApps(filtered)
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun initSystemTray() {
        systemTrayView.visibility = View.GONE

        findViewById<ImageButton>(R.id.btnExpandTray).setOnClickListener {
            systemTrayView.visibility =
                if (systemTrayView.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        // Bluetooth toggle
        findViewById<ImageButton>(R.id.btnBluetooth).setOnClickListener {
            val btIntent = Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
            startActivity(btIntent)
        }

        // WiFi toggle
        findViewById<ImageButton>(R.id.btnWifi).setOnClickListener {
            val wifiIntent = Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
            startActivity(wifiIntent)
        }

        // Cast
        findViewById<ImageButton>(R.id.btnCast).setOnClickListener {
            val castIntent = Intent(android.provider.Settings.ACTION_CAST_SETTINGS)
            try { startActivity(castIntent) } catch (_: Exception) {
                Toast.makeText(this, R.string.cast_not_supported, Toast.LENGTH_SHORT).show()
            }
        }

        // Notifications
        findViewById<ImageButton>(R.id.btnNotifications).setOnClickListener {
            openNotificationPanel()
        }
    }

    private fun initDesktop() {
        // Desktop shortcuts grid
        val spacing = resources.getDimensionPixelSize(R.dimen.desktop_item_spacing)
        desktopShortcuts.addItemDecoration(
            androidx.recyclerview.widget.GridLayoutManager(this, 4).let {
                desktopShortcuts.layoutManager = it
                object : androidx.recyclerview.widget.RecyclerView.ItemDecoration() {
                    override fun getItemOffsets(
                        outRect: android.graphics.Rect,
                        view: View,
                        parent: RecyclerView,
                        state: RecyclerView.State
                    ) {
                        outRect.set(spacing / 2, spacing / 2, spacing / 2, spacing / 2)
                    }
                }
            }
        )

        // Show pinned apps on desktop
        desktopShortcuts.adapter = DesktopShortcutAdapter(
            windowManager.getPinnedApps().toList(),
            onAppClick = { pkg ->
                val info = allApps.find { it.packageName == pkg }
                if (info != null) launchAppInWindow(info)
                else {
                    windowManager.addWindow(pkg)
                    addDecoratorForWindow(pkg)
                }
            },
            onAppRemove = { pkg -> windowManager.unpinApp(pkg) }
        )

        // Long press for context menu
        workspaceContainer.setOnLongClickListener {
            showDesktopContextMenu()
            true
        }
    }

    private fun showDesktopContextMenu() {
        val items = arrayOf(
            getString(R.string.menu_change_wallpaper),
            getString(R.string.menu_add_widget),
            getString(R.string.menu_desktop_settings)
        )
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.desktop_menu_title)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> pickWallpaper()
                    1 -> Toast.makeText(this, R.string.widget_coming_soon, Toast.LENGTH_SHORT).show()
                    2 -> startActivity(Intent(this, SettingsActivity::class.java))
                }
            }
            .show()
    }

    private fun launchAppInWindow(app: AppInfo) {
        windowManager.launchApp(app)
        // Create and add a WindowDecorator overlay
        addDecoratorForWindow(app.packageName)
    }

    private fun addDecoratorForWindow(packageName: String) {
        val decorator = WindowDecorator(this, windowManager, packageName)
        windowManager.registerDecorator(packageName, decorator)
        decorator.updatePosition()
        workspaceContainer.addView(decorator)
    }

    private fun getInstalledApps(): List<AppInfo> {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        return pm.queryIntentActivities(intent, 0).mapNotNull { ri ->
            try {
                val ap = ri.activityInfo
                AppInfo(
                    packageName = ap.packageName,
                    appName = ap.loadLabel(pm).toString(),
                    icon = ap.loadIcon(pm)
                )
            } catch (_: Exception) { null }
        }.sortedBy { it.appName.lowercase() }
    }

    private fun showAppContextMenu(app: AppInfo) {
        val items = arrayOf(
            getString(R.string.menu_open_window),
            getString(R.string.menu_pin_to_desktop),
            getString(R.string.menu_app_info)
        )
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(app.appName)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> launchAppInWindow(app)
                    1 -> {
                        windowManager.pinApp(app)
                        Toast.makeText(this, "已固定到桌面", Toast.LENGTH_SHORT).show()
                    }
                    2 -> openAppInfo(app.packageName)
                }
            }
            .show()
    }

    private fun openAppInfo(pkg: String) {
        startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.parse("package:$pkg")
        })
    }

    private fun toggleStartMenu() {
        startMenuView.visibility =
            if (startMenuView.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        if (startMenuView.visibility == View.VISIBLE) {
            etStartMenuSearch.text.clear()
            startMenuAppAdapter?.updateApps(allApps)
        }
    }

    private fun openNotificationPanel() {
        try {
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                windowManager.focusedPackage?.let { pkg ->
                    // Try opening notification settings for focused window
                    val intent = Intent("android.settings.APP_NOTIFICATION_SETTINGS").apply {
                        putExtra("app_package", pkg)
                        putExtra("android.provider.extra.APP_PACKAGE", pkg)
                    }
                    startActivity(intent)
                } ?: run {
                    startActivity(Intent("android.settings.NOTIFICATION_SETTINGS"))
                }
            }
        } catch (_: Exception) {}
    }

    private fun refreshBatteryInfo() {
        val bm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getSystemService(BATTERY_SERVICE) as? BatteryManager
        } else null
        val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val tvBattery = findViewById<TextView>(R.id.tvBattery)
        tvBattery.text = if (level > 0) "$level%" else "---"
    }

    private fun refreshNetworkInfo() {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        val nw = cm?.activeNetworkInfo
        val tvNetwork = findViewById<TextView>(R.id.tvNetwork)
        tvNetwork.text = when {
            nw == null || !nw.isConnected -> "离线"
            nw.type == android.net.ConnectivityManager.TYPE_WIFI -> "WiFi"
            nw.type == android.net.ConnectivityManager.TYPE_MOBILE -> "5G"
            else -> ""
        }
    }

    private fun pickWallpaper() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        startActivityForResult(intent, 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                // Persist permission
                contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                scope.launch {
                    settingsStore.setWallpaperUri(uri.toString())
                }
                loadWallpaperImage(uri.toString())
            }
        }
    }

    private fun loadWallpaper() {
        scope.launch {
            val uri = settingsStore.getWallpaperUri()
            if (uri.isNotEmpty()) {
                loadWallpaperImage(uri)
            }
        }
    }

    private fun loadWallpaperImage(uriString: String) {
        try {
            val uri = android.net.Uri.parse(uriString)
            workspaceContainer.background = android.graphics.drawable.BitmapDrawable(
                resources,
                android.provider.MediaStore.Images.Media.getBitmap(contentResolver, uri)
            )
        } catch (_: Exception) {}
    }

    private fun startClock() {
        tvClock = findViewById(R.id.tvClock)
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                tvClock.text = clockFormatter.format(Date())
                handler.postDelayed(this, 30000)
            }
        }
        handler.post(runnable)
    }

    private fun setupBackBehavior() {
        // Back key behavior: go back to MainActivity, not exit
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.desktopRoot)) { _, insets ->
            insets
        }
    }

    override fun onBackPressed() {
        // Close start menu if open
        if (startMenuView.visibility == View.VISIBLE) {
            startMenuView.visibility = View.GONE
            return
        }
        if (systemTrayView.visibility == View.VISIBLE) {
            systemTrayView.visibility = View.GONE
            return
        }

        // If windows are open, minimize all
        if (windowManager.hasWindows()) {
            windowManager.minimizeAllWindows()
            return
        }

        // Go back to MainActivity (phone launcher mode)
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
    }
}

// Taskbar running app adapter
class TaskbarAppAdapter(
    private val onAppClick: (String) -> Unit,
    private val onAppClose: (String) -> Unit
) : RecyclerView.Adapter<TaskbarAppAdapter.VH>() {

    private var apps: List<String> = emptyList()

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val iconView: ImageView = view.findViewById(R.id.ivTaskAppIcon)
        val closeButton: ImageButton = view.findViewById(R.id.btnCloseTaskApp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_taskbar_app, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val pkg = apps[position]
        try {
            val ctx = holder.itemView.context
            val icon = ctx.packageManager.getApplicationIcon(pkg)
            holder.iconView.setImageDrawable(icon)
        } catch (_: Exception) {}

        holder.iconView.setOnClickListener { onAppClick(pkg) }
        holder.closeButton.setOnClickListener { onAppClose(pkg) }
    }

    override fun getItemCount(): Int = apps.size

    fun updateApps(newApps: List<String>) {
        apps = newApps
        notifyDataSetChanged()
    }
}

// Desktop shortcut adapter
class DesktopShortcutAdapter(
    private var apps: List<String>,
    private val onAppClick: (String) -> Unit,
    private val onAppRemove: (String) -> Unit
) : RecyclerView.Adapter<DesktopShortcutAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val iconView: ImageView = view.findViewById(R.id.ivAppIcon)
        val nameView: TextView = view.findViewById(R.id.tvAppName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_icon, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val pkg = apps[position]
        val ctx = holder.itemView.context
        try {
            val pm = ctx.packageManager
            val info = pm.getApplicationInfo(pkg, 0)
            holder.iconView.setImageDrawable(info.loadIcon(pm))
            holder.nameView.text = info.loadLabel(pm).toString()
        } catch (_: Exception) {
            holder.nameView.text = pkg
        }
        holder.itemView.setOnClickListener { onAppClick(pkg) }
        holder.itemView.setOnLongClickListener {
            onAppRemove(pkg)
            true
        }
    }

    override fun getItemCount(): Int = apps.size

    fun updateList(list: List<String>) {
        apps = list
        notifyDataSetChanged()
    }
}
