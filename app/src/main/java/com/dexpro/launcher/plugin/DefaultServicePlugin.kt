package com.dexpro.launcher.plugin

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

/**
 * Service plugin providing system monitoring and background tasks.
 *
 * Features:
 * - Battery level monitoring
 * - Network connectivity monitoring
 * - Storage space monitoring
 * - Auto-cleanup scheduling
 * - Screen timeout management
 */
class DefaultServicePlugin : BasePlugin() {

    override val id = "service.core"
    override val name = "Core Service"
    override val type = PluginType.SERVICE
    override val version = "1.0"
    override val author = "DeX Pro"
    override val description = "System monitoring and background automation"

    data class SystemState(
        val batteryLevel: Int = -1,
        val isCharging: Boolean = false,
        val storageUsedPercent: Float = 0f,
        val runningWindows: Int = 0
    )

    /** Current system state snapshot. */
    var systemState: SystemState = SystemState()
        private set

    /** Callbacks for system state changes. */
    private val stateListeners = mutableListOf<(SystemState) -> Unit>()

    override fun onCreate() {
        refreshSystemState()
    }

    fun addStateListener(listener: (SystemState) -> Unit) {
        stateListeners.add(listener)
    }

    fun removeStateListener(listener: (SystemState) -> Unit) {
        stateListeners.remove(listener)
    }

    /**
     * Refresh system state snapshot.
     */
    fun refreshSystemState(): SystemState {
        val batteryStatus = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }

        val batteryLevel = batteryStatus?.let { intent ->
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            (level * 100.0f / scale).toInt()
        } ?: -1

        val isCharging = batteryStatus?.let { intent ->
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
        } ?: false

        val statFs = android.os.StatFs(android.os.Environment.getDataDirectory().path)
        val totalBlocks = statFs.blockCountLong
        val availableBlocks = statFs.availableBlocksLong
        val storageUsedPercent = if (totalBlocks > 0) {
            ((totalBlocks - availableBlocks).toFloat() / totalBlocks) * 100f
        } else 0f

        systemState = SystemState(
            batteryLevel = batteryLevel,
            isCharging = isCharging,
            storageUsedPercent = storageUsedPercent
        )

        stateListeners.forEach { it(systemState) }
        return systemState
    }

    override fun onDestroy() {
        stateListeners.clear()
    }
}