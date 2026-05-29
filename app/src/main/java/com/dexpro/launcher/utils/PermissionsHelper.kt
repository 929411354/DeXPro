package com.dexpro.launcher.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast

/**
 * Centralized permission checking and guidance for DeX Pro.
 *
 * Required permissions for full desktop mode:
 * - SYSTEM_ALERT_WINDOW: overlay windows (freeform title bars, floating panels)
 * - WRITE_SECURE_SETTINGS: enable freeform support, desktop mode features
 * - Accessibility service: taskbar running-app detection, window monitoring
 * - POST_NOTIFICATIONS (API 33+): foreground service notification
 * - Shizuku: elevated shell access without root
 */
object PermissionsHelper {

    data class PermissionStatus(
        val name: String,
        val description: String,
        val isGranted: Boolean,
        val fixAction: (Activity) -> Unit
    )

    fun getAllStatuses(context: Context): List<PermissionStatus> = listOf(
        PermissionStatus(
            name = "Display Overlay",
            description = "Required for freeform window title bars and floating panels",
            isGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                Settings.canDrawOverlays(context) else true,
            fixAction = { activity ->
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                activity.startActivity(intent)
            }
        ),
        PermissionStatus(
            name = "Accessibility Service",
            description = "Detects running apps and enables taskbar window management",
            isGranted = isAccessibilityEnabled(context),
            fixAction = { activity ->
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                activity.startActivity(intent)
                Toast.makeText(
                    context,
                    "Find 'DeX Pro' in Accessibility settings and enable it",
                    Toast.LENGTH_LONG
                ).show()
            }
        ),
        PermissionStatus(
            name = "Write Secure Settings",
            description = "Enables Android freeform window support (enable_freeform_support=1)",
            isGranted = hasWriteSecureSettings(context),
            fixAction = { activity ->
                showAdbGuide(context, "Enable freeform windows",
                    "adb shell pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS && " +
                    "adb shell settings put global enable_freeform_support 1")
            }
        ),
        PermissionStatus(
            name = "Notifications",
            description = "Shows persistent desktop mode notification (Android 13+)",
            isGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
                else true,
            fixAction = { activity ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    activity.requestPermissions(
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2001
                    )
                }
            }
        ),
        PermissionStatus(
            name = "Shizuku (Optional)",
            description = "Elevated shell access for full freeform windows and system tweaks",
            isGranted = isShizukuAvailable(),
            fixAction = { activity ->
                val shizukuHelper = ShizukuHelper(context)
                shizukuHelper.openShizuku()
            }
        ),
        PermissionStatus(
            name = "Wireless Debugging",
            description = "ADB over WiFi for remote debugging and shell access",
            isGranted = isWirelessDebuggingEnabled(context),
            fixAction = { activity ->
                showAdbGuide(context, "Enable wireless debugging",
                    "1. Enable Developer Options\n" +
                    "2. Enable 'Wireless debugging' in Developer Options\n" +
                    "3. Pair device with: adb pair <ip>:<port> <code>")
            }
        )
    )

    fun getMissingPermissions(context: Context): List<PermissionStatus> =
        getAllStatuses(context).filter { !it.isGranted }

    fun getCriticalMissing(context: Context): List<PermissionStatus> =
        getMissingPermissions(context).filter { it.name != "Shizuku (Optional)" && it.name != "Wireless Debugging" }

    private fun isAccessibilityEnabled(context: Context): Boolean {
        val service = "${context.packageName}/.service.TaskbarAccessibilityService"
        val enabled = try {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""
        } catch (e: Exception) { "" }
        return enabled.contains(service)
    }

    private fun hasWriteSecureSettings(context: Context): Boolean {
        return try {
            // Test by reading a known key — if we can read it, WRITE_SECURE_SETTINGS may be granted
            // The real test is trying to write, but we avoid side effects
            val pkg = context.packageManager.getPackageInfo(context.packageName,
                android.content.pm.PackageManager.GET_PERMISSIONS)
            pkg.requestedPermissions?.any {
                it == Manifest.permission.WRITE_SECURE_SETTINGS &&
                (pkg.requestedPermissionsFlags?.getOrNull(
                    pkg.requestedPermissions.indexOf(it)
                ) ?: 0) and android.content.pm.PackageInfo.REQUESTED_PERMISSION_GRANTED != 0
            } ?: false
        } catch (e: Exception) { false }
    }

    private fun isShizukuAvailable(): Boolean {
        return try {
            rikka.shizuku.Shizuku.pingBinder()
        } catch (e: Exception) { false }
    }

    private fun isWirelessDebuggingEnabled(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Settings.Global.getInt(
                    context.contentResolver,
                    "adb_wifi_enabled"
                ) == 1
            } else false
        } catch (e: Exception) { false }
    }

    fun showAdbGuide(context: Context, title: String, commands: String) {
        val message = "$title\n\nRun this command on a computer with ADB connected:\n\n$commands\n\n" +
                "Or use Shizuku to grant permissions directly on-device."
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    /**
     * Check if freeform support is enabled in system settings.
     */
    fun isFreeformEnabled(context: Context): Boolean {
        return try {
            Settings.Global.getInt(
                context.contentResolver, "enable_freeform_support"
            ) == 1
        } catch (e: Exception) { false }
    }
}