package com.dexpro.launcher.widget

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup

/**
 * Manages the AppWidgetHost for hosting system widgets on the desktop.
 *
 * Android allows apps to host widgets via AppWidgetHost.
 * This manager handles lifecycle: allocation, binding, persistence, and removal.
 *
 * Widget state is persisted via SharedPreferences to survive activity recreation.
 */
class AppWidgetHostManager private constructor(context: Context) {

    companion object {
        private const val TAG = "DeXPro-Widget"
        private const val HOST_ID = 1024
        private const val PREFS_NAME = "dexpro_widgets"
        private const val KEY_WIDGET_IDS = "widget_ids"
        const val REQUEST_BIND_WIDGET = 3001

        private var instance: AppWidgetHostManager? = null

        @Synchronized
        fun getInstance(context: Context): AppWidgetHostManager {
            return instance ?: AppWidgetHostManager(context.applicationContext).also {
                instance = it
            }
        }
    }

    private val appContext = context.applicationContext
    val appWidgetHost = AppWidgetHost(appContext, HOST_ID)
    val appWidgetManager = AppWidgetManager.getInstance(appContext)

    /**
     * Start listening for widget updates.
     * Must be called from Activity.onCreate().
     */
    fun startListening() {
        appWidgetHost.startListening()
        Log.d(TAG, "AppWidgetHost started listening")
    }

    /**
     * Stop listening to save resources.
     * Must be called from Activity.onStop().
     */
    fun stopListening() {
        appWidgetHost.stopListening()
        Log.d(TAG, "AppWidgetHost stopped listening")
    }

    /**
     * Allocate a new AppWidget ID for binding.
     */
    fun allocateWidgetId(): Int {
        return appWidgetHost.allocateAppWidgetId()
    }

    /**
     * Delete a widget and its host ID.
     */
    fun deleteWidget(widgetId: Int) {
        appWidgetHost.deleteAppWidgetId(widgetId)
        removeWidgetFromPrefs(widgetId)
        Log.d(TAG, "Widget $widgetId deleted")
    }

    /**
     * Create a host view for a given widget.
     * The caller should add this view to the layout.
     */
    fun createWidgetView(widgetId: Int): AppWidgetHostView {
        return appWidgetHost.createView(appContext, widgetId, null)
    }

    /**
     * Get a provider info for a widget ID.
     */
    fun getWidgetInfo(widgetId: Int): AppWidgetProviderInfo? {
        return try {
            appWidgetManager.getAppWidgetInfo(widgetId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get widget info for $widgetId", e)
            null
        }
    }

    /**
     * Open the system widget picker to let user select a widget.
     * @return the widget ID that was allocated, to be used when the picker returns.
     */
    fun openWidgetPicker(activity: Activity): Int {
        val widgetId = allocateWidgetId()

        val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
        activity.startActivityForResult(pickIntent, REQUEST_BIND_WIDGET)

        return widgetId
    }

    /**
     * Handle the result from widget picker.
     * @return true if widget was successfully bound
     */
    fun handlePickerResult(activity: Activity, requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode != REQUEST_BIND_WIDGET || resultCode != Activity.RESULT_OK || data == null) {
            return false
        }

        val widgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            return false
        }

        // Configure widget with default options
        configureWidget(activity, widgetId)
        saveWidgetToPrefs(widgetId)

        Log.i(TAG, "Widget $widgetId bound successfully")
        return true
    }

    /**
     * Configure a newly bound widget.
     */
    private fun configureWidget(activity: Activity, widgetId: Int) {
        val info = getWidgetInfo(widgetId) ?: return

        // If the widget has a configuration activity, launch it
        if (info.configure != null) {
            val configIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                component = info.configure
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                activity.startActivity(configIntent)
            } catch (e: Exception) {
                Log.w(TAG, "Could not launch widget config activity", e)
            }
        }
    }

    /**
     * Restore all previously saved widget IDs and return their host views.
     */
    fun restoreWidgets(): List<AppWidgetHostView> {
        val widgetIds = getSavedWidgetIds()
        val views = mutableListOf<AppWidgetHostView>()

        for (widgetId in widgetIds) {
            try {
                val view = createWidgetView(widgetId)
                views.add(view)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to restore widget $widgetId, removing", e)
                deleteWidget(widgetId)
            }
        }

        Log.d(TAG, "Restored ${views.size} widgets")
        return views
    }

    // ===== Persistence =====

    private fun getSavedWidgetIds(): List<Int> {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val set = prefs.getStringSet(KEY_WIDGET_IDS, emptySet()) ?: emptySet()
        return set.mapNotNull { it.toIntOrNull() }
    }

    private fun saveWidgetToPrefs(widgetId: Int) {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = getSavedWidgetIds().toMutableList()
        current.add(widgetId)
        prefs.edit().putStringSet(KEY_WIDGET_IDS, current.map { it.toString() }.toSet()).apply()
    }

    private fun removeWidgetFromPrefs(widgetId: Int) {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = getSavedWidgetIds().toMutableList()
        current.remove(widgetId)
        prefs.edit().putStringSet(KEY_WIDGET_IDS, current.map { it.toString() }.toSet()).apply()
    }

    fun release() {
        stopListening()
        instance = null
    }
}