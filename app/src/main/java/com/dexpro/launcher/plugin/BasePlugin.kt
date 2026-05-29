package com.dexpro.launcher.plugin

import android.content.Context

/**
 * Base abstract class for all DeX Pro plugins.
 *
 * Lifecycle mirrors Android lifecycle: onCreate → onStart → onResume → (active)
 * → onPause → onStop → onDestroy.
 *
 * Plugin types (4 categories):
 * - Shell: desktop UI (wallpaper, theme, start menu, dock)
 * - Window: window behavior (animations, layout, snap rules)
 * - Input: user input (gestures, keyboard, mouse, pen)
 * - Service: background processing (sync, monitoring, automation)
 */
abstract class BasePlugin {

    /** Unique plugin ID. */
    abstract val id: String

    /** Human-readable name. */
    abstract val name: String

    /** Plugin type category. */
    abstract val type: PluginType

    /** Version string for compatibility checking. */
    abstract val version: String

    /** Minimum DeX Pro version required. */
    open val minAppVersion: String = "1.0"

    /** Author / developer name. */
    open val author: String = ""

    /** Short description. */
    open val description: String = ""

    /** Whether this plugin is enabled by default. */
    open val enabledByDefault: Boolean = true

    /** Plugin state. Managed by PluginManager. */
    var state: PluginState = PluginState.UNLOADED
        internal set

    /** Application context (set by PluginManager). */
    lateinit var context: Context
        internal set

    // ===== Lifecycle =====

    /** Called when plugin is first loaded. Initialize resources here. */
    open fun onCreate() {}

    /** Called when plugin becomes visible / active. */
    open fun onStart() {}

    /** Called when plugin is fully active (foreground). */
    open fun onResume() {}

    /** Called when plugin is partially hidden. */
    open fun onPause() {}

    /** Called when plugin is fully hidden. */
    open fun onStop() {}

    /** Called when plugin is being destroyed. Clean up resources. */
    open fun onDestroy() {}

    /** Called when plugin settings are changed. */
    open fun onSettingsChanged(key: String, value: Any?) {}

    // ===== Plugin metadata =====

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BasePlugin) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

enum class PluginType {
    SHELL,
    WINDOW,
    INPUT,
    SERVICE
}

enum class PluginState {
    UNLOADED,
    CREATED,
    STARTED,
    RESUMED,
    PAUSED,
    STOPPED,
    DESTROYED
}