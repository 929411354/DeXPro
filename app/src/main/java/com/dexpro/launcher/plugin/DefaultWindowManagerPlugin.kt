package com.dexpro.launcher.plugin

import android.content.Context
import android.graphics.Rect

/**
 * Window manager plugin providing customizable window behavior.
 *
 * Features:
 * - Custom snap zones (position + threshold)
 * - Window animation overrides
 * - Auto-tiling layouts (columns/rows/grid)
 * - Per-app default window sizes
 */
class DefaultWindowManagerPlugin : BasePlugin() {

    override val id = "window.manager"
    override val name = "Window Manager"
    override val type = PluginType.WINDOW
    override val version = "1.0"
    override val author = "DeX Pro"
    override val description = "Custom window snapping, tiling, and animation rules"

    /** Custom snap zones defined by user. */
    data class SnapZone(
        val id: String,
        val name: String,
        val bounds: Rect  // ratio-based (0..1000) relative to screen
    )

    /** Per-app default window size (ratio-based). */
    data class AppWindowPrefs(
        val packageName: String,
        val defaultBounds: Rect,  // ratio-based
        val alwaysMaximized: Boolean = false
    )

    private val snapZones = mutableListOf<SnapZone>()
    private val appPrefs = mutableMapOf<String, AppWindowPrefs>()

    /** Enable window animations. */
    var enableAnimations = true

    /** Animation duration in ms. */
    var animationDuration = 250L

    fun setAppWindowPrefs(packageName: String, bounds: Rect, alwaysMaximized: Boolean = false) {
        appPrefs[packageName] = AppWindowPrefs(packageName, bounds, alwaysMaximized)
    }

    fun getAppWindowPrefs(packageName: String): AppWindowPrefs? {
        return appPrefs[packageName]
    }

    fun addSnapZone(zone: SnapZone) {
        snapZones.add(zone)
    }

    fun removeSnapZone(zoneId: String) {
        snapZones.removeAll { it.id == zoneId }
    }

    fun getSnapZones(): List<SnapZone> = snapZones.toList()

    fun clearSnapZones() {
        snapZones.clear()
    }

    override fun onSettingsChanged(key: String, value: Any?) {
        when (key) {
            "window_animations" -> enableAnimations = value as? Boolean ?: true
            "animation_duration" -> animationDuration = value as? Long ?: 250L
        }
    }
}