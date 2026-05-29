package com.dexpro.launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.TextUtils

class PermissionsHelper(private val context: Context) {

    companion object {
        const val KEY_DISPLAY_OVERLAY = "display_overlay"
        const val KEY_ACCESSIBILITY = "accessibility"
        const val KEY_WRITE_SECURE = "write_secure_settings"
        const val KEY_NOTIFICATION = "notification"
        const val KEY_SHIZUKU = "shizuku"
        const val KEY_WIRELESS_DEBUG = "wireless_debug"
    }

    data class PermissionItem(
        val key: String,
        val name: String,
        val description: String,
        val isGranted: Boolean,
        val grantIntent: Intent?
    )

    fun checkAll(): List<PermissionItem> = listOf(
        PermissionItem(
            KEY_DISPLAY_OVERLAY,
            "浮窗覆盖权限",
            "允许在其他应用上方显示窗口，桌面模式必需",
            canDrawOverlays(),
            overlayIntent()
        ),
        PermissionItem(
            KEY_ACCESSIBILITY,
            "无障碍服务",
            "检测前台应用状态，支持窗口管理和任务栏切换",
            isAccessibilityEnabled(),
            accessibilityIntent()
        ),
        PermissionItem(
            KEY_WRITE_SECURE,
            "修改系统设置",
            "启用自由窗口模式 (freeform)，支持多窗口运行",
            canWriteSecureSettings(),
            writeSecureIntent()
        ),
        PermissionItem(
            KEY_NOTIFICATION,
            "通知权限",
            "在桌面模式中显示系统通知内容",
            hasNotificationAccess(),
            notificationIntent()
        ),
        PermissionItem(
            KEY_SHIZUKU,
            "Shizuku 权限",
            "通过 Shizuku 获取系统级权限，实现真正的小窗启动",
            isShizukuRunningAndGranted(),
            shizukuIntent()
        ),
        PermissionItem(
            KEY_WIRELESS_DEBUG,
            "无线调试",
            "通过无线调试启用 Shizuku，无需连接电脑",
            isWirelessDebugEnabled(),
            wirelessDebugIntent()
        )
    )

    fun getStatusMap(): Map<String, Boolean> = mapOf(
        KEY_DISPLAY_OVERLAY to canDrawOverlays(),
        KEY_ACCESSIBILITY to isAccessibilityEnabled(),
        KEY_WRITE_SECURE to canWriteSecureSettings(),
        KEY_NOTIFICATION to hasNotificationAccess(),
        KEY_SHIZUKU to isShizukuRunningAndGranted(),
        KEY_WIRELESS_DEBUG to isWirelessDebugEnabled()
    )

    fun canDrawOverlays(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context)
        else true

    fun overlayIntent(): Intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}")
    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

    fun isAccessibilityEnabled(): Boolean {
        val svc = "${context.packageName}/com.dexpro.launcher.service.TaskbarAccessibilityService"
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        return TextUtils.SimpleStringSplitter(':').apply { setString(enabled) }.contains(svc)
    }

    fun accessibilityIntent(): Intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun canWriteSecureSettings(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        return try {
            Settings.System.canWrite(context)
        } catch (_: Exception) { false }
    }

    fun writeSecureIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun hasNotificationAccess(): Boolean {
        val listeners = Settings.Secure.getString(
            context.contentResolver, "enabled_notification_listeners"
        ) ?: ""
        return listeners.contains(context.packageName)
    }

    fun notificationIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun isShizukuRunningAndGranted(): Boolean {
        return try {
            rikka.shizuku.Shizuku.pingBinder() &&
            rikka.shizuku.Shizuku.checkSelfPermission() ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) { false }
    }

    fun shizukuIntent(): Intent? {
        return try {
            context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
        } catch (_: Exception) { null }
    }

    fun isWirelessDebugEnabled(): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, "adb_wifi_enabled") == 1
        } catch (_: Exception) { false }
    }

    fun wirelessDebugIntent(): Intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun getCriticalMissing(): List<PermissionItem> =
        checkAll().filter { !it.isGranted }
}
