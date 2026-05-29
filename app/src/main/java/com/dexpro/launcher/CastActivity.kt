package com.dexpro.launcher

import android.content.Context
import android.media.MediaRouter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.dexpro.launcher.cast.CastManager

/**
 * Cast / screen mirroring device selector.
 *
 * Lists available cast receivers (Miracast, Chromecast, etc.)
 * and allows connecting/disconnecting.
 */
class CastActivity : AppCompatActivity() {

    private lateinit var castManager: CastManager
    private lateinit var container: LinearLayout
    private lateinit var tvStatus: TextView
    private lateinit var btnRefresh: Button
    private lateinit var btnDisconnect: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cast)

        castManager = CastManager.getInstance(this)
        container = findViewById(R.id.castDeviceContainer)
        tvStatus = findViewById(R.id.tvCastStatus)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnDisconnect = findViewById(R.id.btnDisconnect)

        btnRefresh.setOnClickListener { refreshDevices() }
        btnDisconnect.setOnClickListener {
            castManager.stopCasting()
            updateStatus()
            refreshDevices()
        }

        castManager.startDiscovery()
        refreshDevices()
        updateStatus()
    }

    private fun refreshDevices() {
        container.removeAllViews()
        val routes = castManager.getAvailableRoutes()
        val selectedRoute = castManager.getSelectedRoute()

        if (routes.isEmpty()) {
            val emptyView = layoutInflater.inflate(
                R.layout.item_cast_device, container, false
            )
            emptyView.findViewById<TextView>(R.id.tvDeviceName).text = "No devices found"
            emptyView.findViewById<TextView>(R.id.tvDeviceDesc).text =
                "Make sure your TV or display supports Miracast / Chromecast"
            emptyView.findViewById<Button>(R.id.btnConnect).visibility = View.GONE
            container.addView(emptyView)
            return
        }

        for (route in routes) {
            val itemView = layoutInflater.inflate(
                R.layout.item_cast_device, container, false
            )

            val tvName = itemView.findViewById<TextView>(R.id.tvDeviceName)
            val tvDesc = itemView.findViewById<TextView>(R.id.tvDeviceDesc)
            val btnConnect = itemView.findViewById<Button>(R.id.btnConnect)

            tvName.text = route.name.toString()
            tvDesc.text = route.description?.toString() ?: ""

            val isSelected = route == selectedRoute
            if (isSelected) {
                btnConnect.text = "Connected"
                btnConnect.isEnabled = false
                btnConnect.setBackgroundColor(
                    ContextCompat.getColor(this, R.color.accentGreen)
                )
            } else {
                btnConnect.text = "Connect"
                btnConnect.isEnabled = true
                btnConnect.setBackgroundColor(
                    ContextCompat.getColor(this, R.color.primary)
                )
                btnConnect.setOnClickListener {
                    val success = castManager.startCasting(route)
                    if (success) {
                        Toast.makeText(
                            this,
                            "Casting to ${route.name}",
                            Toast.LENGTH_SHORT
                        ).show()
                        updateStatus()
                        refreshDevices()
                    } else {
                        Toast.makeText(
                            this,
                            "Failed to connect to ${route.name}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            container.addView(itemView)
        }
    }

    private fun updateStatus() {
        if (castManager.isCasting()) {
            val route = castManager.getSelectedRoute()
            tvStatus.text = "Casting to: ${route?.name ?: "Unknown"}"
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.accentGreen))
            btnDisconnect.visibility = View.VISIBLE
        } else {
            tvStatus.text = "Not casting"
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.onSurface))
            btnDisconnect.visibility = View.GONE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        castManager.stopDiscovery()
    }
}