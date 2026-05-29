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
 * Adapter for desktop shortcut icons.
 * Supports long-click to remove and click to launch.
 */
class DesktopShortcutAdapter(
    private val shortcuts: MutableList<AppInfo>,
    private val onAppClick: (AppInfo) -> Unit,
    private val onAppLongClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<DesktopShortcutAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iconView: ImageView = itemView.findViewById(R.id.ivShortcutIcon)
        val nameView: TextView = itemView.findViewById(R.id.tvShortcutName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_desktop_shortcut, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = shortcuts[position]
        holder.iconView.setImageDrawable(app.icon)
        holder.nameView.text = app.appName

        holder.itemView.setOnClickListener { onAppClick(app) }
        holder.itemView.setOnLongClickListener {
            onAppLongClick(app)
            true
        }
    }

    override fun getItemCount(): Int = shortcuts.size

    fun addShortcut(app: AppInfo) {
        shortcuts.add(app)
        notifyItemInserted(shortcuts.size - 1)
    }

    fun removeShortcut(position: Int) {
        if (position in shortcuts.indices) {
            shortcuts.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun getShortcuts(): List<AppInfo> = shortcuts.toList()
}