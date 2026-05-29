package com.dexpro.launcher

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.dexpro.launcher.utils.PermissionsHelper

/**
 * First-run permission setup wizard.
 *
 * Guides user through granting all required permissions for full desktop mode:
 * - Display overlay (freeform title bars)
 * - Accessibility service (taskbar detection)
 * - Write secure settings (freeform support)
 * - Notifications (foreground service)
 * - Shizuku (optional, elevated shell)
 * - Wireless debugging (ADB over WiFi)
 */
class PermissionGuideActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission_guide)

        container = findViewById(R.id.permissionContainer)

        findViewById<Button>(R.id.btnContinue).setOnClickListener {
            // Check if critical permissions are granted
            val criticalMissing = PermissionsHelper.getCriticalMissing(this)
            if (criticalMissing.isEmpty()) {
                Toast.makeText(this, "All critical permissions granted!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(
                    this,
                    "Still missing: ${criticalMissing.joinToString(", ") { it.name }}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        findViewById<Button>(R.id.btnSkip).setOnClickListener {
            finish()
        }

        buildPermissionList()
    }

    private fun buildPermissionList() {
        val statuses = PermissionsHelper.getAllStatuses(this)
        container.removeAllViews()

        for (status in statuses) {
            val row = layoutInflater.inflate(R.layout.item_permission, container, false)

            val tvName = row.findViewById<TextView>(R.id.tvPermName)
            val tvDesc = row.findViewById<TextView>(R.id.tvPermDesc)
            val tvStatus = row.findViewById<TextView>(R.id.tvPermStatus)
            val btnGrant = row.findViewById<Button>(R.id.btnGrant)

            tvName.text = status.name
            tvDesc.text = status.description

            if (status.isGranted) {
                tvStatus.text = "Granted"
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.onPrimary))
                btnGrant.visibility = View.GONE
            } else {
                tvStatus.text = "Not granted"
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.windowBorder))
                btnGrant.visibility = View.VISIBLE
                btnGrant.setOnClickListener {
                    status.fixAction(this)
                }
            }

            container.addView(row)
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh statuses when returning from settings
        buildPermissionList()
    }
}