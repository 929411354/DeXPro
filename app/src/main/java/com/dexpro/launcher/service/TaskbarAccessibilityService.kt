package com.dexpro.launcher.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent

/**
 * Accessibility service for DeX Pro desktop mode.
 *
 * Used to:
 * - Detect running/windowed apps for taskbar display
 * - Monitor window state changes
 * - Provide enhanced keyboard navigation
 */
class TaskbarAccessibilityService : AccessibilityService() {

    companion object {
        var instance: TaskbarAccessibilityService? = null
            private set

        const val ACTION_UPDATE_TASKBAR = "com.dexpro.launcher.UPDATE_TASKBAR"
        const val EXTRA_RUNNING_APPS = "running_apps"
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                // Window opened, closed, or changed state
                val packageName = event.packageName?.toString() ?: return
                val runningApps = getRunningAppPackages()
                broadcastUpdate(runningApps)
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    /**
     * Get list of package names for currently running apps.
     */
    private fun getRunningAppPackages(): List<String> {
        val packages = mutableSetOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            windows?.forEach { window ->
                window.root?.packageName?.toString()?.let { packages.add(it) }
            }
        }
        return packages.toList()
    }

    /**
     * Broadcast running apps list to update taskbar.
     */
    private fun broadcastUpdate(runningApps: List<String>) {
        val intent = Intent(ACTION_UPDATE_TASKBAR).apply {
            putStringArrayListExtra(EXTRA_RUNNING_APPS, ArrayList(runningApps))
        }
        sendBroadcast(intent)
    }
}
