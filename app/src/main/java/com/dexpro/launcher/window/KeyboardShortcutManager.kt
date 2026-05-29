package com.dexpro.launcher.window

import android.view.KeyEvent

/**
 * Central keyboard shortcut dispatcher for desktop mode.
 *
 * Maps key combinations to desktop actions, supports:
 * - Windows key (Meta) + letter/number combos
 * - Alt + letter combos
 * - Ctrl + letter combos
 * - Function key combos
 *
 * Actions are string identifiers dispatched to DesktopActivity.
 */
class KeyboardShortcutManager(
    private val dispatcher: (action: String) -> Unit
) {
    /** Registered shortcut bindings. */
    data class Shortcut(
        val id: String,
        val name: String,
        val keyCode: Int,
        val modifiers: Int,
        val action: String
    )

    private val shortcuts = mutableListOf<Shortcut>()

    init {
        registerDefaults()
    }

    /**
     * Process a key event. Returns true if the event was consumed by a shortcut.
     */
    fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false

        val eventModifiers = event.modifiers and
                (KeyEvent.META_CTRL_ON or KeyEvent.META_ALT_ON or KeyEvent.META_SHIFT_ON or
                 KeyEvent.META_META_ON or KeyEvent.META_SYM_ON)

        val matched = shortcuts.find { binding ->
            binding.keyCode == event.keyCode && binding.modifiers == eventModifiers
        } ?: return false

        dispatcher(matched.action)
        return true
    }

    /**
     * Register a custom shortcut.
     */
    fun register(id: String, name: String, keyCode: Int, modifiers: Int, action: String) {
        shortcuts.add(Shortcut(id, name, keyCode, modifiers, action))
    }

    /**
     * Unregister a shortcut by ID.
     */
    fun unregister(id: String) {
        shortcuts.removeAll { it.id == id }
    }

    /**
     * Get all registered shortcuts.
     */
    fun getAllShortcuts(): List<Shortcut> = shortcuts.toList()

    /**
     * Clear all custom shortcuts and restore defaults.
     */
    fun resetToDefaults() {
        shortcuts.clear()
        registerDefaults()
    }

    private fun registerDefaults() {
        // Win + key shortcuts
        register("win_d", "Show Desktop", KeyEvent.KEYCODE_D, KeyEvent.META_META_ON, "show_desktop")
        register("win_e", "File Manager", KeyEvent.KEYCODE_E, KeyEvent.META_META_ON, "open_files")
        register("win_r", "Run", KeyEvent.KEYCODE_R, KeyEvent.META_META_ON, "run_command")
        register("win_i", "Settings", KeyEvent.KEYCODE_I, KeyEvent.META_META_ON, "open_settings")
        register("win_m", "Minimize All", KeyEvent.KEYCODE_M, KeyEvent.META_META_ON, "minimize_all")
        register("win_t", "Terminal", KeyEvent.KEYCODE_T, KeyEvent.META_META_ON, "open_terminal")
        register("win_a", "App Drawer", KeyEvent.KEYCODE_A, KeyEvent.META_META_ON, "toggle_app_drawer")
        register("win_s", "Search", KeyEvent.KEYCODE_S, KeyEvent.META_META_ON, "toggle_search")
        register("win_l", "Lock", KeyEvent.KEYCODE_L, KeyEvent.META_META_ON, "lock_screen")
        register("win_b", "Notification Panel", KeyEvent.KEYCODE_B, KeyEvent.META_META_ON, "toggle_notifications")
        register("win_p", "Project/Display", KeyEvent.KEYCODE_P, KeyEvent.META_META_ON, "project_display")
        register("win_x", "Quick Link Menu", KeyEvent.KEYCODE_X, KeyEvent.META_META_ON, "quick_link_menu")
        register("win_1", "Workspace 1", KeyEvent.KEYCODE_1, KeyEvent.META_META_ON, "switch_workspace_1")
        register("win_2", "Workspace 2", KeyEvent.KEYCODE_2, KeyEvent.META_META_ON, "switch_workspace_2")
        register("win_3", "Workspace 3", KeyEvent.KEYCODE_3, KeyEvent.META_META_ON, "switch_workspace_3")
        register("win_4", "Workspace 4", KeyEvent.KEYCODE_4, KeyEvent.META_META_ON, "switch_workspace_4")

        // Win + arrows (Aero Snap)
        register("win_left", "Snap Left", KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.META_META_ON, "snap_left")
        register("win_right", "Snap Right", KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.META_META_ON, "snap_right")
        register("win_up", "Maximize", KeyEvent.KEYCODE_DPAD_UP, KeyEvent.META_META_ON, "maximize_window")
        register("win_down", "Restore/Minimize", KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.META_META_ON, "restore_window")

        // Alt combos
        register("alt_tab", "Switch Window", KeyEvent.KEYCODE_TAB, KeyEvent.META_ALT_ON, "switch_window")
        register("alt_f4", "Close Window", KeyEvent.KEYCODE_F4, KeyEvent.META_ALT_ON, "close_window")
        register("alt_space", "Window Menu", KeyEvent.KEYCODE_SPACE, KeyEvent.META_ALT_ON, "window_menu")
        register("alt_enter", "Properties", KeyEvent.KEYCODE_ENTER, KeyEvent.META_ALT_ON, "show_properties")

        // Ctrl combos
        register("ctrl_c", "Copy", KeyEvent.KEYCODE_C, KeyEvent.META_CTRL_ON, "copy")
        register("ctrl_v", "Paste", KeyEvent.KEYCODE_V, KeyEvent.META_CTRL_ON, "paste")
        register("ctrl_x", "Cut", KeyEvent.KEYCODE_X, KeyEvent.META_CTRL_ON, "cut")
        register("ctrl_z", "Undo", KeyEvent.KEYCODE_Z, KeyEvent.META_CTRL_ON, "undo")
        register("ctrl_y", "Redo", KeyEvent.KEYCODE_Y, KeyEvent.META_CTRL_ON, "redo")
        register("ctrl_a", "Select All", KeyEvent.KEYCODE_A, KeyEvent.META_CTRL_ON, "select_all")
        register("ctrl_s", "Save", KeyEvent.KEYCODE_S, KeyEvent.META_CTRL_ON, "save")

        // Function keys
        register("f5", "Refresh", KeyEvent.KEYCODE_F5, 0, "refresh")
        register("f11", "Fullscreen", KeyEvent.KEYCODE_F11, 0, "toggle_fullscreen")

        // Special
        register("printscreen", "Screenshot", KeyEvent.KEYCODE_SYSRQ, 0, "take_screenshot")
        register("escape", "Close Overlay", KeyEvent.KEYCODE_ESCAPE, 0, "close_overlay")
    }

    companion object {
        fun modifierToString(modifiers: Int): String {
            val parts = mutableListOf<String>()
            if (modifiers and KeyEvent.META_META_ON != 0) parts.add("Win")
            if (modifiers and KeyEvent.META_ALT_ON != 0) parts.add("Alt")
            if (modifiers and KeyEvent.META_CTRL_ON != 0) parts.add("Ctrl")
            if (modifiers and KeyEvent.META_SHIFT_ON != 0) parts.add("Shift")
            return parts.joinToString(" + ")
        }
    }
}