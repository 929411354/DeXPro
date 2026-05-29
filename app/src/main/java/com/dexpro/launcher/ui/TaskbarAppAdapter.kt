package com.dexpro.launcher.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dexpro.launcher.AppInfo
import com.dexpro.launcher.R

/**
 * Adapter for the taskbar running/pinned apps.
 * Uses ListAdapter with DiffUtil for efficient updates.
 */
class TaskbarAppAdapter(
    private val onAppClick: (AppInfo) -> Unit,
    private val onAppClose: (AppInfo) -> Unit
) : ListAdapter<AppInfo, TaskbarAppAdapter.ViewHolder>(DIFF_CALLBACK) {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iconView: ImageView = itemView.findViewById(R.id.ivTaskAppIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_taskbar_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = getItem(position)
        holder.iconView.setImageDrawable(app.icon)

        holder.itemView.setOnClickListener { onAppClick(app) }
        holder.itemView.setOnLongClickListener {
            onAppClose(app)
            true
        }
    }

    /**
     * Update apps list with DiffUtil-powered efficient updates.
     */
    fun updateApps(newApps: List<AppInfo>) {
        submitList(newApps.toList())
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AppInfo>() {
            override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
                return oldItem.packageName == newItem.packageName
            }

            override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
                return oldItem.packageName == newItem.packageName &&
                    oldItem.appName == newItem.appName
            }
        }
    }
}