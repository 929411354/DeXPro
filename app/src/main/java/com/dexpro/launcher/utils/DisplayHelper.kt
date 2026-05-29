package com.dexpro.launcher.utils

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display

/**
 * Helper class for detecting and managing external displays.
 *
 * Handles:
 * - Detection of external display connections (HDMI, USB-C, Miracast)
 * - Display metrics for multi-screen layouts
 * - Display change listener registration
 */
class DisplayHelper(private val context: Context) {

    private val displayManager: DisplayManager =
        context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

    /**
     * Check if any external display (non-primary) is connected.
     */
    fun isExternalDisplayConnected(): Boolean {
        val displays = displayManager.displays
        return displays.any { it.displayId != Display.DEFAULT_DISPLAY }
    }

    /**
     * Get all connected external displays.
     */
    fun getExternalDisplays(): List<Display> {
        return displayManager.displays.filter {
            it.displayId != Display.DEFAULT_DISPLAY
        }
    }

    /**
     * Get number of connected displays.
     */
    fun getDisplayCount(): Int {
        return displayManager.displays.size
    }

    /**
     * Get the primary (built-in) display.
     */
    fun getPrimaryDisplay(): Display {
        return displayManager.getDisplay(Display.DEFAULT_DISPLAY)
    }

    /**
     * Check if the device has built-in desktop mode support
     * (Android 10+ experimental desktop mode).
     */
    fun hasBuiltInDesktopMode(): Boolean {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
            return false
        }

        // Check if developer option for desktop mode is enabled
        return try {
            val value = android.provider.Settings.Global.getInt(
                context.contentResolver,
                "force_desktop_mode_on_external_displays"
            )
            value == 1
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Enable built-in Android desktop mode (requires WRITE_SECURE_SETTINGS).
     */
    fun enableBuiltInDesktopMode(): Boolean {
        return try {
            android.provider.Settings.Global.putInt(
                context.contentResolver,
                "force_desktop_mode_on_external_displays",
                1
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Register a listener for display connection/disconnection events.
     */
    fun registerDisplayListener(listener: DisplayManager.DisplayListener) {
        displayManager.registerDisplayListener(listener, null)
    }

    /**
     * Unregister a previously registered display listener.
     */
    fun unregisterDisplayListener(listener: DisplayManager.DisplayListener) {
        displayManager.unregisterDisplayListener(listener)
    }
}
