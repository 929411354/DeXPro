package com.dexpro.launcher

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

class PhoneAppAdapter(
    private val context: Context,
    private val apps: List<AppInfo>,
    private val onAppClick: (AppInfo) -> Unit
) : BaseAdapter() {

    override fun getCount(): Int = apps.size

    override fun getItem(position: Int): Any = apps[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_app_icon, parent, false)

        val app = apps[position]
        view.findViewById<ImageView>(R.id.ivAppIcon).setImageDrawable(app.icon)
        view.findViewById<TextView>(R.id.tvAppName).text = app.appName
        view.setOnClickListener { onAppClick(app) }
        return view
    }
}
