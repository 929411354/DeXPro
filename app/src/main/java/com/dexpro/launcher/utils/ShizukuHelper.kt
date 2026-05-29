package com.dexpro.launcher.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Helper class for managing elevated permissions via Shizuku.
 *
 * Shizuku provides a bridge to system-level permissions without root.
 * Used for:
 * - WRITE_SECURE_SETTINGS (desktop mode, freeform windows)
 * - Overlay permissions (floating windows)
 * - Accessibility service enabling
 * - pm grant for elevated permissions
 * - settings put global commands
 */
class ShizukuHelper(private val context: Context) {

    companion object {
        private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    }

    /**
     * Check if Shizuku is installed.
     */
    fun isShizukuInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if Shizuku is running and available.
     */
    fun isShizukuAvailable(): Boolean {
        if (!isShizukuInstalled()) return false
        return try {
            Shizuku.pingBinder()
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Open Shizuku app or Play Store to install it.
     */
    fun openShizuku() {
        if (isShizukuInstalled()) {
            val intent = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
            if (intent != null) {
                context.startActivity(intent)
            }
        } else {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://play.google.com/store/apps/details?id=$SHIZUKU_PACKAGE")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Please install Shizuku from Google Play", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Ensure all necessary permissions are granted.
     */
    fun ensurePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(context)) {
                requestOverlayPermission()
            }
        }
        if (!isAccessibilityServiceEnabled()) {
            openAccessibilitySettings()
        }
        if (!isShizukuInstalled()) {
            Toast.makeText(context, "For full desktop mode features, install and start Shizuku", Toast.LENGTH_LONG).show()
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(intent)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val service = "${context.packageName}/.service.TaskbarAccessibilityService"
        val enabledServices = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        } catch (e: Exception) { "" }
        return enabledServices.contains(service)
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Execute a shell command via Shizuku with elevated privileges.
     */
    private fun shizukuExec(command: String): String {
        if (!isShizukuAvailable()) return ""

        return try {
            // Shizuku.newProcess is @JvmStatic but Kotlin may restrict access; use reflection
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java
            )
            method.isAccessible = true
            val process = method.invoke(null, arrayOf("/system/bin/sh", "-c", command), null, null) as java.lang.Process
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))

            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.appendLine(line)
            }
            while (errorReader.readLine().also { line = it } != null) {
                output.appendLine(line)
            }

            process.waitFor()
            reader.close()
            errorReader.close()
            process.destroy()
            output.toString().trim()
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Grant WRITE_SECURE_SETTINGS and enable freeform/desktop mode via Shizuku.
     * Commands executed:
     * - pm grant com.dexpro.launcher android.permission.WRITE_SECURE_SETTINGS
     * - settings put global enable_freeform_support 1
     * - settings put global force_desktop_mode_on_external_displays 1
     */
    fun grantSecureSettingsViaShizuku(): Boolean {
        if (!isShizukuAvailable()) {
            Toast.makeText(context, "Shizuku is not running. Start Shizuku first.", Toast.LENGTH_LONG).show()
            return false
        }

        return try {
            val pkg = context.packageName

            // Grant WRITE_SECURE_SETTINGS
            val grantResult = shizukuExec("pm grant $pkg android.permission.WRITE_SECURE_SETTINGS")

            // Enable freeform window support
            shizukuExec("settings put global enable_freeform_support 1")

            // Enable desktop mode on external displays
            shizukuExec("settings put global force_desktop_mode_on_external_displays 1")

            val success = grantResult.isEmpty() || grantResult.contains("granted", ignoreCase = true)

            if (success) {
                Toast.makeText(context, "Desktop mode permissions granted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Permission grant result: $grantResult", Toast.LENGTH_LONG).show()
            }

            success
        } catch (e: Exception) {
            Toast.makeText(context, "Shizuku grant failed: ${e.message}", Toast.LENGTH_LONG).show()
            false
        }
    }

    /**
     * Execute a custom shell command with Shizuku privileges.
     * Returns the command output as string.
     */
    fun executeAsRoot(command: String): String {
        return shizukuExec(command)
    }

    /**
     * Launch an app in freeform window mode via Shizuku.
     * Uses 'am start-activity --display <display>' with --windowingMode 5 (freeform).
     */
    fun launchInFreeform(packageName: String, bounds: android.graphics.Rect): Boolean {
        if (!isShizukuAvailable()) return false

        return try {
            val cmd = "am start -n $packageName/$(cmd package resolve-activity --brief $packageName " +
                    "| tail -n 1) --windowingMode 5 " +
                    "--bounds ${bounds.left},${bounds.top},${bounds.right},${bounds.bottom} " +
                    "--activity-clear-top"
            val result = shizukuExec(cmd)
            result.isNotEmpty() && !result.contains("Error", ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if Shizuku permission is already granted.
     */
    fun checkShizukuPermission(): Boolean {
        return try {
            Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Request Shizuku permission if not already granted.
     */
    fun requestShizukuPermission(code: Int = 101) {
        if (!checkShizukuPermission()) {
            Shizuku.requestPermission(code)
        }
    }
}