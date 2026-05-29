package com.dexpro.launcher.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.dexpro.launcher.AppInfo
import com.dexpro.launcher.R

class PinnedAppAdapter(
    private val apps: MutableList<AppInfo>,
    private val onAppClick: (AppInfo) -> Unit,
    private val onAppLongClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<PinnedAppAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iconView: ImageView = itemView.findViewById(R.id.ivPinnedIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pinned_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.iconView.setImageDrawable(app.icon)

        holder.itemView.setOnClickListener { onAppClick(app) }
        holder.itemView.setOnLongClickListener {
            onAppLongClick(app)
            true
        }
    }

    override fun getItemCount(): Int = apps.size

    fun updateApps(newApps: List<AppInfo>) {
        apps.clear()
        apps.addAll(newApps)
        notifyDataSetChanged()
    }
}