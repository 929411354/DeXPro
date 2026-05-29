package com.dexpro.launcher

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PermissionGuideActivity : AppCompatActivity() {

    private lateinit var permHelper: PermissionsHelper
    private lateinit var adapter: PermissionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission_guide)

        permHelper = PermissionsHelper(this)

        val recyclerView = findViewById<RecyclerView>(R.id.rvPermissions)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = PermissionAdapter { item ->
            item.grantIntent?.let {
                try { startActivity(it) } catch (_: Exception) {}
            }
        }
        recyclerView.adapter = adapter

        findViewById<Button>(R.id.btnSkip).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.btnContinue).setOnClickListener {
            val missing = permHelper.getCriticalMissing()
            if (missing.isEmpty()) {
                // All granted
                val intent = Intent(this, DesktopActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(intent)
                finish()
            } else {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(R.string.permission_incomplete_title)
                    .setMessage("仍有 ${missing.size} 项权限未授权：\n${
                        missing.joinToString("\n") { "  ${it.name}" }
                    }\n\n缺少这些权限可能影响桌面模式的完整功能。\n确定要进入桌面模式吗？")
                    .setPositiveButton(R.string.enter_desktop_anyway) { _, _ ->
                        val intent = Intent(this, DesktopActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        }
                        startActivity(intent)
                        finish()
                    }
                    .setNegativeButton(R.string.continue_grant, null)
                    .show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        adapter.updateItems(permHelper.checkAll())
    }

    inner class PermissionAdapter(
        private val onGrantClick: (PermissionsHelper.PermissionItem) -> Unit
    ) : RecyclerView.Adapter<PermissionAdapter.VH>() {

        private var items: List<PermissionsHelper.PermissionItem> = emptyList()

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvPermName)
            val tvDesc: TextView = view.findViewById(R.id.tvPermDesc)
            val tvStatus: TextView = view.findViewById(R.id.tvPermStatus)
            val btnGrant: Button = view.findViewById(R.id.btnGrant)
            val expandArea: View = view.findViewById(R.id.expandGuide)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_permission, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.tvName.text = item.name
            holder.tvDesc.text = item.description

            if (item.isGranted) {
                holder.tvStatus.text = "\u2713"
                holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                holder.btnGrant.text = getString(R.string.granted)
                holder.btnGrant.isEnabled = false
                holder.btnGrant.alpha = 0.5f
            } else {
                holder.tvStatus.text = "\u2717"
                holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#EA4335"))
                holder.btnGrant.text = getString(R.string.go_grant)
                holder.btnGrant.isEnabled = true
                holder.btnGrant.alpha = 1.0f
            }

            holder.btnGrant.setOnClickListener {
                onGrantClick(item)
            }

            // Expand guide for wireless debug
            if (item.key == PermissionsHelper.KEY_WIRELESS_DEBUG) {
                holder.expandArea.visibility = View.VISIBLE
                holder.tvDesc.text = item.description +
                    "\n\n操作步骤：\n1. 打开「开发者选项」\n2. 开启「无线调试」\n3. 点按「使用配对码配对设备」\n4. 在 Shizuku 中输入显示的配对码"
            } else {
                holder.expandArea.visibility = View.GONE
            }
        }

        override fun getItemCount(): Int = items.size

        fun updateItems(newItems: List<PermissionsHelper.PermissionItem>) {
            items = newItems
            notifyDataSetChanged()
        }
    }
}
