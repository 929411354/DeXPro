package com.dexpro.launcher

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dexpro.launcher.ui.StartMenuAdapter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var ph: PermissionsHelper
    private lateinit var allApps: List<AppInfo>
    private lateinit var adapter: StartMenuAdapter
    private lateinit var searchEditText: EditText
    private lateinit var btnClear: ImageView
    private lateinit var tvTime: TextView
    private lateinit var tvBattery: TextView
    private lateinit var tvNetwork: TextView

    private lateinit var dotOverlay: ImageView
    private lateinit var dotAccessibility: ImageView
    private lateinit var dotWriteSecure: ImageView
    private lateinit var dotNotification: ImageView
    private lateinit var dotShizuku: ImageView
    private lateinit var dotWirelessDebug: ImageView

    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phone_launcher)

        ph = PermissionsHelper(this)

        initStatusBar()
        initSearchBar()
        initAppGrid()
        initBottomBar()
        checkPermissionsOnFirstLaunch()
        startClockUpdater()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionDots()
        refreshSystemInfo()
    }

    private fun initStatusBar() {
        tvTime = findViewById(R.id.tvStatusTime)
        tvBattery = findViewById(R.id.tvStatusBattery)
        tvNetwork = findViewById(R.id.tvStatusNetwork)

        // Notification icon placeholder
        val ivNotifications = findViewById<ImageView>(R.id.ivStatusNotification)
        ivNotifications.setOnClickListener {
            // Open notification settings
            try {
                startActivity(Intent("android.settings.NOTIFICATION_SETTINGS"))
            } catch (_: Exception) {}
        }

        refreshSystemInfo()
    }

    private fun refreshSystemInfo() {
        // Time
        tvTime.text = timeFormatter.format(Date())

        // Battery
        val bm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getSystemService(BATTERY_SERVICE) as? BatteryManager
        } else null
        val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        tvBattery.text = if (level > 0) "$level%" else "---"

        // Network indicator
        tvNetwork.text = getNetworkType()
    }

    private fun getNetworkType(): String {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        val activeNetwork = cm?.activeNetworkInfo
        return when {
            activeNetwork == null || !activeNetwork.isConnected -> "离线"
            activeNetwork.type == android.net.ConnectivityManager.TYPE_WIFI -> "WiFi"
            activeNetwork.type == android.net.ConnectivityManager.TYPE_MOBILE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        val tm = getSystemService(TELEPHONY_SERVICE) as android.telephony.TelephonyManager
                        when (tm.dataNetworkType) {
                            13 -> "5G LTE"
                            20 -> "5G NR"
                            else -> "${activeNetwork.subtypeName}"
                        }
                    } catch (_: Exception) { "移动网络" }
                } else "移动网络"
            }
            else -> "连接中"
        }
    }

    private fun initSearchBar() {
        searchEditText = findViewById(R.id.etPhoneSearch)
        btnClear = findViewById(R.id.btnClearSearch)
        btnClear.setOnClickListener {
            searchEditText.text.clear()
        }
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterApps(s?.toString() ?: "")
                btnClear.visibility = if (s.isNullOrEmpty()) android.view.View.GONE else android.view.View.VISIBLE
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        btnClear.visibility = android.view.View.GONE
    }

    private fun initAppGrid() {
        allApps = getInstalledApps()
        val grid = findViewById<RecyclerView>(R.id.phoneAppGrid)
        grid.layoutManager = GridLayoutManager(this, 4)
        adapter = StartMenuAdapter(allApps, { app ->
            launchApp(app)
        }, { app ->
            showAppContextMenu(app, grid)
        })
        grid.adapter = adapter
    }

    private fun filterApps(query: String) {
        val filtered = if (query.isBlank()) allApps
        else allApps.filter { it.appName.contains(query, ignoreCase = true) }
        adapter.updateApps(filtered)
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

    private fun launchApp(app: AppInfo) {
        val intent = packageManager.getLaunchIntentForPackage(app.packageName)
        if (intent != null) {
            startActivity(intent)
        }
    }

    private fun showAppContextMenu(app: AppInfo, anchor: RecyclerView) {
        val popup = PopupMenu(this, anchor, Gravity.CENTER)
        popup.menu.add(0, 0, 0, R.string.menu_open).also { mi ->
            mi.setIcon(android.R.drawable.ic_menu_edit)
        }
        popup.menu.add(0, 1, 1, R.string.menu_small_window).also { mi ->
            mi.setIcon(android.R.drawable.ic_menu_crop)
        }
        popup.menu.add(0, 2, 2, R.string.menu_app_info).also { mi ->
            mi.setIcon(android.R.drawable.ic_menu_info_details)
        }

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                0 -> launchApp(app)
                1 -> launchSmallWindow(app)
                2 -> openAppInfo(app.packageName)
            }
            true
        }
        popup.show()
    }

    private fun launchSmallWindow(app: AppInfo) {
        val intent = packageManager.getLaunchIntentForPackage(app.packageName) ?: return
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val dm = resources.displayMetrics
            val bounds = android.graphics.Rect(
                (dm.widthPixels * 0.1).toInt(),
                (dm.heightPixels * 0.15).toInt(),
                (dm.widthPixels * 0.6).toInt(),
                (dm.heightPixels * 0.6).toInt()
            )
            val options = android.app.ActivityOptions.makeBasic().apply {
                launchBounds = bounds
            }
            startActivity(intent, options.toBundle())
        } else {
            startActivity(intent)
        }
    }

    private fun openAppInfo(packageName: String) {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }

    private fun initBottomBar() {
        findViewById<android.widget.Button>(R.id.btnPermissions).setOnClickListener {
            startActivity(Intent(this, PermissionGuideActivity::class.java))
        }
        findViewById<android.widget.Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<android.widget.Button>(R.id.btnSwitchToDesktop).setOnClickListener {
            startDesktopMode()
        }

        // Permission status dots
        dotOverlay = findViewById(R.id.dotOverlay)
        dotAccessibility = findViewById(R.id.dotAccessibility)
        dotWriteSecure = findViewById(R.id.dotWriteSecure)
        dotNotification = findViewById(R.id.dotNotification)
        dotShizuku = findViewById(R.id.dotShizuku)
        dotWirelessDebug = findViewById(R.id.dotWirelessDebug)

        updatePermissionDots()
    }

    private fun updatePermissionDots() {
        val status = ph.getStatusMap()
        setDotColor(dotOverlay, status[PermissionsHelper.KEY_DISPLAY_OVERLAY] == true)
        setDotColor(dotAccessibility, status[PermissionsHelper.KEY_ACCESSIBILITY] == true)
        setDotColor(dotWriteSecure, status[PermissionsHelper.KEY_WRITE_SECURE] == true)
        setDotColor(dotNotification, status[PermissionsHelper.KEY_NOTIFICATION] == true)
        setDotColor(dotShizuku, status[PermissionsHelper.KEY_SHIZUKU] == true)
        setDotColor(dotWirelessDebug, status[PermissionsHelper.KEY_WIRELESS_DEBUG] == true)
    }

    private fun setDotColor(dot: ImageView, granted: Boolean) {
        val color = if (granted) android.graphics.Color.parseColor("#4CAF50")
        else android.graphics.Color.parseColor("#EA4335")
        dot.setColorFilter(color)
    }

    private fun checkPermissionsOnFirstLaunch() {
        val prefs = getSharedPreferences("dexpro_prefs", MODE_PRIVATE)
        val hasShown = prefs.getBoolean("permission_guide_shown", false)
        if (!hasShown) {
            prefs.edit().putBoolean("permission_guide_shown", true).apply()
            val missing = ph.getCriticalMissing()
            if (missing.isNotEmpty()) {
                AlertDialog.Builder(this)
                    .setTitle(R.string.permission_required_title)
                    .setMessage("共有 ${missing.size} 项权限未授权：\n" +
                        missing.joinToString("\n") { "• ${it.name}" } +
                        "\n\n部分功能需要这些权限才能正常工作，是否前往授权？")
                    .setPositiveButton(R.string.go_grant) { _, _ ->
                        startActivity(Intent(this, PermissionGuideActivity::class.java))
                    }
                    .setNegativeButton(R.string.later, null)
                    .show()
            }
        }
    }

    private fun startDesktopMode() {
        val intent = Intent(this, DesktopActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
    }

    private fun startClockUpdater() {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                tvTime.text = timeFormatter.format(Date())
                handler.postDelayed(this, 30000)
            }
        }
        handler.postDelayed(runnable, 30000)
    }
}
