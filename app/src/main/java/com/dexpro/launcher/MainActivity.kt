package com.dexpro.launcher

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dexpro.launcher.ui.StartMenuAdapter
import com.dexpro.launcher.utils.DisplayHelper
import com.dexpro.launcher.utils.PermissionsHelper

/**
 * Main launcher activity — phone mode app drawer with desktop mode entry.
 *
 * Two modes:
 * - External display connected → auto-launch DesktopActivity
 * - No external display → show phone launcher with full app grid,
 *   manual "Switch to Desktop" button, and permission guide
 */
class MainActivity : AppCompatActivity() {

    private lateinit var displayHelper: DisplayHelper
    private var allApps: List<AppInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        displayHelper = DisplayHelper(this)

        if (displayHelper.isExternalDisplayConnected()) {
            startDesktopMode()
        } else {
            showPhoneLauncher()
        }
    }

    private fun showPhoneLauncher() {
        setContentView(R.layout.activity_phone_launcher)

        // Load apps
        val pm = packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        allApps = pm.queryIntentActivities(mainIntent, 0).map { ri ->
            AppInfo(
                packageName = ri.activityInfo.packageName,
                appName = ri.loadLabel(pm).toString(),
                icon = ri.loadIcon(pm)
            )
        }.sortedBy { it.appName.lowercase() }

        val recyclerView = findViewById<RecyclerView>(R.id.phoneAppGrid)
        recyclerView.layoutManager = GridLayoutManager(this, 4)
        val adapter = StartMenuAdapter(
            allApps,
            onAppClick = { appInfo ->
                val launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName)
                if (launchIntent != null) startActivity(launchIntent)
            },
            onAppLongClick = { /* no-op in phone mode */ }
        )
        adapter.updateApps(allApps)
        recyclerView.adapter = adapter

        // Search bar
        findViewById<EditText>(R.id.etPhoneSearch).addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val query = s.toString().trim().lowercase()
                    val filtered = if (query.isEmpty()) allApps else
                        allApps.filter {
                            it.appName.lowercase().contains(query) ||
                            it.packageName.lowercase().contains(query)
                        }
                    adapter.updateApps(filtered)
                }
            }
        )

        // Switch to Desktop button — wired up for real
        findViewById<Button>(R.id.btnSwitchToDesktop).setOnClickListener {
            // Check critical permissions first, warn if missing
            val missing = PermissionsHelper.getCriticalMissing(this)
            if (missing.isNotEmpty()) {
                val names = missing.joinToString(", ") { it.name }
                Toast.makeText(
                    this,
                    "Desktop mode may be limited. Missing: $names",
                    Toast.LENGTH_LONG
                ).show()
            }
            startDesktopMode()
        }
    }

    private fun startDesktopMode() {
        val intent = Intent(this, DesktopActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
        finish()
    }
}