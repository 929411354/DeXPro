package com.dexpro.launcher

import android.app.ActivityOptions
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.widget.FrameLayout
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.dexpro.launcher.utils.ShizukuHelper
import com.dexpro.launcher.window.SnapEdge
import com.dexpro.launcher.window.WindowDecorator

class DeXWindowManager(
    private val context: Context,
    private val container: FrameLayout
) {
    companion object {
        const val MAX_WINDOWS = 8
    }

    data class WindowState(
        val packageName: String,
        val appName: String,
        var bounds: Rect,
        var isMinimized: Boolean = false,
        var isMaximized: Boolean = false,
        var zOrder: Int = 0
    )

    private val shizukuHelper = ShizukuHelper(context)
    private val windowStates = LinkedHashMap<String, WindowState>()
    private val windowDecorators = LinkedHashMap<String, WindowDecorator>()
    private var nextZOrder = 0

    private val _windowList = MutableLiveData<List<String>>()
    val windowList: LiveData<List<String>> get() = _windowList

    var focusedPackage: String? = null
        private set

    var windowLimit: Int = MAX_WINDOWS

    fun launchApp(appInfo: AppInfo, smallWindow: Boolean = false): Boolean {
        val pkg = appInfo.packageName
        val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        launchIntent.addFlags(
            android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
            android.content.Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        )

        val screenWidth = context.resources.displayMetrics.widthPixels
        val screenHeight = context.resources.displayMetrics.heightPixels
        val taskbarHeight = 48.dpToPx()
        val availableHeight = screenHeight - taskbarHeight

        val bounds = if (smallWindow) {
            Rect(
                (screenWidth * 0.15).toInt(), (screenHeight * 0.1).toInt(),
                (screenWidth * 0.7).toInt(), (screenHeight * 0.65).toInt()
            )
        } else {
            Rect(
                (screenWidth * 0.05).toInt(), 0,
                (screenWidth * 0.75).toInt(), availableHeight
            )
        }

        // Tier 1: Shizuku setLaunchBounds
        if (shizukuHelper.isShizukuAvailable()) {
            val success = shizukuHelper.launchInFreeform(pkg, bounds)
            if (success) {
                addWindowState(pkg, appInfo.appName, bounds)
                return true
            }
        }

        // Tier 2: ActivityOptions.setLaunchBounds
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                val options = ActivityOptions.makeBasic().apply { launchBounds = bounds }
                context.startActivity(launchIntent, options.toBundle())
                addWindowState(pkg, appInfo.appName, bounds)
                return true
            } catch (_: Exception) {}
        }

        // Tier 3: Fullscreen fallback
        context.startActivity(launchIntent)
        addWindowState(pkg, appInfo.appName, Rect(0, 0, screenWidth, availableHeight))
        return true
    }

    private fun addWindowState(pkg: String, appName: String, bounds: Rect) {
        // Enforce window limit
        if (windowStates.size >= windowLimit) {
            val oldest = windowStates.keys.firstOrNull()
            if (oldest != null) {
                closeWindow(oldest)
            }
        }

        val state = WindowState(pkg, appName, Rect(bounds), zOrder = nextZOrder++)
        windowStates[pkg] = state
        focusedPackage = pkg
        emitWindowList()
    }

    fun addWindow(packageName: String, bounds: Rect? = null) {
        val appName = try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (_: Exception) { packageName }

        val b = bounds ?: computeDefaultBounds()
        addWindowState(packageName, appName, b)
    }

    fun addWindow(packageName: String) {
        addWindow(packageName, null)
    }

    fun focusWindow(packageName: String) {
        val state = windowStates[packageName] ?: return
        state.isMinimized = false
        state.zOrder = nextZOrder++
        focusedPackage = packageName

        // Re-order in map
        windowStates.remove(packageName)
        windowStates[packageName] = state

        // Bring decorator to front
        windowDecorators[packageName]?.bringToFront()
        emitWindowList()
    }

    fun closeWindow(packageName: String) {
        windowStates.remove(packageName)
        val decorator = windowDecorators.remove(packageName)
        decorator?.let { container.removeView(it) }

        if (focusedPackage == packageName) {
            focusedPackage = windowStates.keys.lastOrNull()
        }
        emitWindowList()
    }

    fun minimizeCurrentWindow() {
        focusedPackage?.let { pkg ->
            windowStates[pkg]?.isMinimized = true
            windowDecorators[pkg]?.visibility = android.view.View.GONE
        }
    }

    fun minimizeAll() {
        windowStates.values.forEach { state ->
            state.isMinimized = true
        }
        windowDecorators.values.forEach { it.visibility = android.view.View.GONE }
        focusedPackage = null
    }

    fun restoreAll() {
        windowStates.values.forEach { state ->
            state.isMinimized = false
        }
        windowDecorators.values.forEach { it.visibility = android.view.View.VISIBLE }
    }

    fun minimizeAllWindows() = minimizeAll()

    fun cycleWindows() {
        val keys = windowStates.keys.toList()
        if (keys.isEmpty()) return
        val idx = focusedPackage?.let { keys.indexOf(it) } ?: -1
        val next = keys[(idx + 1) % keys.size]
        focusWindow(next)
    }

    fun snapWindow(packageName: String, edge: SnapEdge) {
        val state = windowStates[packageName] ?: return
        val screenWidth = context.resources.displayMetrics.widthPixels
        val screenHeight = context.resources.displayMetrics.heightPixels
        val taskbarHeight = 48.dpToPx()
        val availableH = screenHeight - taskbarHeight

        state.bounds = when (edge) {
            SnapEdge.LEFT -> Rect(0, 0, screenWidth / 2, availableH)
            SnapEdge.RIGHT -> Rect(screenWidth / 2, 0, screenWidth, availableH)
            SnapEdge.TOP -> Rect(0, 0, screenWidth, availableH / 2)
            SnapEdge.BOTTOM -> Rect(0, availableH / 2, screenWidth, availableH)
            SnapEdge.MAXIMIZE -> Rect(0, 0, screenWidth, availableH).also { state.isMaximized = true }
            SnapEdge.RESTORE -> computeDefaultBounds().also { state.isMaximized = false }
        }

        windowDecorators[packageName]?.updatePosition()
        windowDecorators[packageName]?.updateButtons()
    }

    fun moveWindow(packageName: String, dx: Int, dy: Int) {
        val state = windowStates[packageName] ?: return
        state.bounds.offset(dx, dy)
    }

    fun resizeWindow(packageName: String, newBounds: Rect) {
        windowStates[packageName]?.bounds = newBounds
    }

    fun getWindowState(packageName: String): WindowState? = windowStates[packageName]

    fun getWindowMeta(packageName: String): WindowManagerMeta? {
        val state = windowStates[packageName] ?: return null
        return WindowManagerMeta(
            packageName = state.packageName,
            bounds = state.bounds,
            isMinimized = state.isMinimized,
            isMaximized = state.isMaximized
        )
    }

    fun hasWindows(): Boolean = windowStates.isNotEmpty()

    fun getPinnedApps(): Set<String> {
        val prefs = context.getSharedPreferences("dexpro_pins", Context.MODE_PRIVATE)
        return prefs.getStringSet("pinned", emptySet()) ?: emptySet()
    }

    fun pinApp(appInfo: AppInfo) {
        val prefs = context.getSharedPreferences("dexpro_pins", Context.MODE_PRIVATE)
        val pinned = prefs.getStringSet("pinned", emptySet())?.toMutableSet() ?: mutableSetOf()
        pinned.add(appInfo.packageName)
        prefs.edit().putStringSet("pinned", pinned).apply()
    }

    fun unpinApp(packageName: String) {
        val prefs = context.getSharedPreferences("dexpro_pins", Context.MODE_PRIVATE)
        val pinned = prefs.getStringSet("pinned", emptySet())?.toMutableSet() ?: mutableSetOf()
        pinned.remove(packageName)
        prefs.edit().putStringSet("pinned", pinned).apply()
    }

    fun registerDecorator(packageName: String, decorator: WindowDecorator) {
        windowDecorators[packageName] = decorator
    }

    fun unregisterDecorator(packageName: String) {
        windowDecorators.remove(packageName)
    }

    fun getCurrentWorkspace(): Int = 1

    fun addWorkspace(): Int = 1

    fun switchWorkspace(num: Int) {}

    fun computeDefaultBounds(): Rect {
        val screenWidth = context.resources.displayMetrics.widthPixels
        val screenHeight = context.resources.displayMetrics.heightPixels
        return Rect(
            (screenWidth * 0.1).toInt(),
            (screenHeight * 0.05).toInt(),
            (screenWidth * 0.7).toInt(),
            (screenHeight * 0.75 - 48.dpToPx()).toInt()
        )
    }

    fun recalculateWindowBounds() {
        val screenWidth = context.resources.displayMetrics.widthPixels
        val screenHeight = context.resources.displayMetrics.heightPixels
        val taskbarHeight = 48.dpToPx()

        windowStates.forEach { (_, state) ->
            if (state.isMaximized) {
                state.bounds = Rect(0, 0, screenWidth, screenHeight - taskbarHeight)
            } else {
                state.bounds = Rect(
                    state.bounds.left.coerceIn(0, screenWidth - 200),
                    state.bounds.top.coerceIn(0, screenHeight - 200 - taskbarHeight),
                    state.bounds.right.coerceIn(200, screenWidth),
                    state.bounds.bottom.coerceIn(200, screenHeight - taskbarHeight)
                )
            }
        }
    }

    fun release() {
        windowStates.clear()
        windowDecorators.clear()
        container.removeAllViews()
    }

    private fun emitWindowList() {
        _windowList.postValue(windowStates.keys.toList())
    }

    private fun Int.dpToPx(): Int =
        (this * context.resources.displayMetrics.density).toInt()
}

data class WindowManagerMeta(
    val packageName: String,
    var bounds: Rect,
    var isMinimized: Boolean = false,
    var isMaximized: Boolean = false
)
