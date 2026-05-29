package com.dexpro.launcher.plugin

import android.content.Context
import android.view.KeyEvent

/**
 * Input plugin managing keyboard shortcuts and gestures.
 *
 * Features:
 * - Custom keyboard shortcut bindings
 * - Global hotkey capture
 * - Gesture recognition (swipe, pinch)
 * - Mouse button remapping
 */
class DefaultInputManagerPlugin : BasePlugin() {

    override val id = "input.manager"
    override val name = "Input Manager"
    override val type = PluginType.INPUT
    override val version = "1.0"
    override val author = "DeX Pro"
    override val description = "Custom keyboard shortcuts and gesture bindings"

    data class ShortcutBinding(
        val id: String,
        val name: String,
        val keyCode: Int,
        val modifiers: Int,  // KeyEvent.META_CTRL_ON / META_ALT_ON / META_SHIFT_ON
        val action: String   // action identifier dispatched to desktop
    )

    private val shortcuts = mutableMapOf<String, ShortcutBinding>()

    fun registerShortcut(binding: ShortcutBinding) {
        shortcuts[binding.id] = binding
    }

    fun unregisterShortcut(id: String) {
        shortcuts.remove(id)
    }

    fun getAllShortcuts(): List<ShortcutBinding> = shortcuts.values.toList()

    /**
     * Match a key event against registered shortcuts.
     * Returns the matched ShortcutBinding or null.
     */
    fun matchShortcut(event: KeyEvent): ShortcutBinding? {
        if (event.action != KeyEvent.ACTION_DOWN) return null

        val eventModifiers = event.modifiers and
                (KeyEvent.META_CTRL_ON or KeyEvent.META_ALT_ON or KeyEvent.META_SHIFT_ON or
                 KeyEvent.META_META_ON or KeyEvent.META_SYM_ON)

        return shortcuts.values.find { binding ->
            binding.keyCode == event.keyCode && binding.modifiers == eventModifiers
        }
    }

    override fun onCreate() {
        // Register default shortcuts
        registerShortcut(ShortcutBinding("win_d", "Show Desktop",
            KeyEvent.KEYCODE_D, KeyEvent.META_META_ON, "show_desktop"))
        registerShortcut(ShortcutBinding("win_e", "File Explorer",
            KeyEvent.KEYCODE_E, KeyEvent.META_META_ON, "open_files"))
        registerShortcut(ShortcutBinding("win_r", "Run",
            KeyEvent.KEYCODE_R, KeyEvent.META_META_ON, "run_command"))
        registerShortcut(ShortcutBinding("alt_tab", "Switch Window",
            KeyEvent.KEYCODE_TAB, KeyEvent.META_ALT_ON, "switch_window"))
        registerShortcut(ShortcutBinding("alt_f4", "Close Window",
            KeyEvent.KEYCODE_F4, KeyEvent.META_ALT_ON, "close_window"))
        registerShortcut(ShortcutBinding("win_i", "Settings",
            KeyEvent.KEYCODE_I, KeyEvent.META_META_ON, "open_settings"))
        registerShortcut(ShortcutBinding("win_m", "Minimize All",
            KeyEvent.KEYCODE_M, KeyEvent.META_META_ON, "minimize_all"))
        registerShortcut(ShortcutBinding("win_left", "Snap Left",
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.META_META_ON, "snap_left"))
        registerShortcut(ShortcutBinding("win_right", "Snap Right",
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.META_META_ON, "snap_right"))
        registerShortcut(ShortcutBinding("win_up", "Maximize",
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.META_META_ON, "maximize_window"))
        registerShortcut(ShortcutBinding("win_down", "Restore",
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.META_META_ON, "restore_window"))
    }
}