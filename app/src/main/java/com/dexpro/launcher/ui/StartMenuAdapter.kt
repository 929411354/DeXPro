package com.dexpro.launcher.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dexpro.launcher.AppInfo
import com.dexpro.launcher.R

/**
 * Adapter for the start menu app grid.
 * Supports dynamic app list updates for search filtering.
 */
class StartMenuAdapter(
    apps: List<AppInfo>,
    private val onAppClick: (AppInfo) -> Unit,
    private val onAppLongClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<StartMenuAdapter.ViewHolder>() {

    private var apps: List<AppInfo> = apps

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iconView: ImageView = itemView.findViewById(R.id.ivAppIcon)
        val nameView: TextView = itemView.findViewById(R.id.tvAppName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_icon, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.iconView.setImageDrawable(app.icon)
        holder.nameView.text = app.appName

        holder.itemView.setOnClickListener { onAppClick(app) }
        holder.itemView.setOnLongClickListener {
            onAppLongClick(app)
            true
        }
    }

    override fun getItemCount(): Int = apps.size

    /**
     * Update the app list (e.g. after search filtering) and refresh the RecyclerView.
     */
    fun updateApps(newApps: List<AppInfo>) {
        apps = newApps
        notifyDataSetChanged()
    }
}
