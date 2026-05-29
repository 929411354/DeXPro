package com.dexpro.launcher.plugin

import android.content.Context
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Central plugin manager for DeX Pro.
 *
 * Handles plugin discovery, loading, lifecycle, and dependency resolution.
 * Plugins are loaded from APK assets (built-in) or external storage (user-installed).
 *
 * Plugin discovery order:
 * 1. Built-in plugins (assets/plugins/ [wildcard] .dex)
 * 2. External plugins ($ {context.filesDir}/plugins/ [wildcard] .dex)
 * 3. Dynamic classpath scanning for BasePlugin subclasses
 */
class PluginManager private constructor(context: Context) : LifecycleEventObserver {

    companion object {
        private const val TAG = "DeXPro-PluginManager"
        private var instance: PluginManager? = null

        @Synchronized
        fun getInstance(context: Context): PluginManager {
            return instance ?: PluginManager(context.applicationContext).also {
                instance = it
            }
        }
    }

    private val appContext = context.applicationContext
    private val plugins = ConcurrentHashMap<String, BasePlugin>()
    private val pluginStates = ConcurrentHashMap<String, PluginState>()
    private val pluginDependencies = ConcurrentHashMap<String, List<String>>()
    private val pluginListeners = mutableListOf<PluginListener>()
    private var isInitialized = false

    // ===== Public API =====

    /**
     * Initialize plugin manager and load all available plugins.
     * Must be called before any plugin operations.
     */
    fun init() {
        if (isInitialized) return

        CoroutineScope(Dispatchers.IO).launch {
            loadBuiltInPlugins()
            loadExternalPlugins()
            scanClasspathForPlugins()
            isInitialized = true
            notifyPluginsLoaded()
        }
    }

    /**
     * Get all loaded plugins.
     */
    fun getAllPlugins(): List<BasePlugin> = plugins.values.toList()

    /**
     * Get plugins by type.
     */
    fun getPluginsByType(type: PluginType): List<BasePlugin> {
        return plugins.values.filter { it.type == type }
    }

    /**
     * Get a specific plugin by ID.
     */
    fun getPlugin(id: String): BasePlugin? = plugins[id]

    /**
     * Enable a plugin (load and start it).
     */
    fun enablePlugin(id: String): Boolean {
        val plugin = plugins[id] ?: return false
        if (plugin.state != PluginState.UNLOADED) return true

        // Check dependencies
        val deps = pluginDependencies[id] ?: emptyList()
        for (depId in deps) {
            if (!isPluginEnabled(depId)) {
                Log.w(TAG, "Plugin $id depends on $depId, which is not enabled")
                return false
            }
        }

        return try {
            plugin.context = appContext
            plugin.onCreate()
            plugin.state = PluginState.CREATED
            plugin.onStart()
            plugin.state = PluginState.STARTED
            plugin.onResume()
            plugin.state = PluginState.RESUMED
            notifyPluginStateChanged(plugin, PluginState.RESUMED)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable plugin $id", e)
            plugin.state = PluginState.UNLOADED
            false
        }
    }

    /**
     * Disable a plugin (stop and unload).
     */
    fun disablePlugin(id: String): Boolean {
        val plugin = plugins[id] ?: return false
        if (plugin.state == PluginState.UNLOADED) return true

        return try {
            plugin.onPause()
            plugin.state = PluginState.PAUSED
            plugin.onStop()
            plugin.state = PluginState.STOPPED
            plugin.onDestroy()
            plugin.state = PluginState.DESTROYED
            notifyPluginStateChanged(plugin, PluginState.DESTROYED)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disable plugin $id", e)
            false
        }
    }

    /**
     * Check if a plugin is enabled (in RESUMED state).
     */
    fun isPluginEnabled(id: String): Boolean {
        return plugins[id]?.state == PluginState.RESUMED
    }

    /**
     * Register a plugin listener for state changes.
     */
    fun addPluginListener(listener: PluginListener) {
        pluginListeners.add(listener)
    }

    /**
     * Unregister a plugin listener.
     */
    fun removePluginListener(listener: PluginListener) {
        pluginListeners.remove(listener)
    }

    /**
     * Dispatch a setting change to all plugins.
     */
    fun dispatchSettingChange(key: String, value: Any?) {
        plugins.values.forEach { plugin ->
            if (plugin.state == PluginState.RESUMED) {
                try {
                    plugin.onSettingsChanged(key, value)
                } catch (e: Exception) {
                    Log.w(TAG, "Plugin ${plugin.id} failed to handle setting change", e)
                }
            }
        }
    }

    // ===== Lifecycle integration =====

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_CREATE -> init()
            Lifecycle.Event.ON_START -> onAppStart()
            Lifecycle.Event.ON_RESUME -> onAppResume()
            Lifecycle.Event.ON_PAUSE -> onAppPause()
            Lifecycle.Event.ON_STOP -> onAppStop()
            Lifecycle.Event.ON_DESTROY -> onAppDestroy()
            else -> {}
        }
    }

    private fun onAppStart() {
        plugins.values.forEach { plugin ->
            if (plugin.state == PluginState.CREATED) {
                try {
                    plugin.onStart()
                    plugin.state = PluginState.STARTED
                } catch (e: Exception) {
                    Log.e(TAG, "Plugin ${plugin.id} failed onStart", e)
                }
            }
        }
    }

    private fun onAppResume() {
        plugins.values.forEach { plugin ->
            if (plugin.state == PluginState.STARTED) {
                try {
                    plugin.onResume()
                    plugin.state = PluginState.RESUMED
                } catch (e: Exception) {
                    Log.e(TAG, "Plugin ${plugin.id} failed onResume", e)
                }
            }
        }
    }

    private fun onAppPause() {
        plugins.values.forEach { plugin ->
            if (plugin.state == PluginState.RESUMED) {
                try {
                    plugin.onPause()
                    plugin.state = PluginState.PAUSED
                } catch (e: Exception) {
                    Log.e(TAG, "Plugin ${plugin.id} failed onPause", e)
                }
            }
        }
    }

    private fun onAppStop() {
        plugins.values.forEach { plugin ->
            if (plugin.state == PluginState.PAUSED) {
                try {
                    plugin.onStop()
                    plugin.state = PluginState.STOPPED
                } catch (e: Exception) {
                    Log.e(TAG, "Plugin ${plugin.id} failed onStop", e)
                }
            }
        }
    }

    private fun onAppDestroy() {
        plugins.values.forEach { plugin ->
            if (plugin.state != PluginState.UNLOADED && plugin.state != PluginState.DESTROYED) {
                try {
                    plugin.onDestroy()
                    plugin.state = PluginState.DESTROYED
                } catch (e: Exception) {
                    Log.e(TAG, "Plugin ${plugin.id} failed onDestroy", e)
                }
            }
        }
        plugins.clear()
        pluginStates.clear()
        instance = null
    }

    // ===== Plugin loading =====

    private fun loadBuiltInPlugins() {
        try {
            // Built-in plugins are compiled into the APK as assets/plugins/**.dex
            // In a real implementation, we'd use DexClassLoader to load them.
            // For now, we'll just register the built-in plugin classes directly.
            registerBuiltInPlugins()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load built-in plugins", e)
        }
    }

    private fun loadExternalPlugins() {
        val pluginsDir = File(appContext.filesDir, "plugins")
        if (!pluginsDir.exists()) {
            pluginsDir.mkdirs()
            return
        }

        pluginsDir.listFiles { file ->
            file.isFile && file.extension == "dex"
        }?.forEach { dexFile ->
            try {
                // Load external .dex file with DexClassLoader
                // val classLoader = DexClassLoader(dexFile.absolutePath, ...)
                // val pluginClass = classLoader.loadClass("...")
                // val plugin = pluginClass.newInstance() as BasePlugin
                // registerPlugin(plugin)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load external plugin ${dexFile.name}", e)
            }
        }
    }

    private fun scanClasspathForPlugins() {
        // This is a simplified implementation.
        // In a real app, you'd use reflection to scan all classes in the classpath.
        // For now, we'll just manually register known plugin classes.
    }

    private fun registerBuiltInPlugins() {
        // Register core built-in plugins
        registerPlugin(DefaultWallpaperPlugin())
        registerPlugin(DefaultWindowManagerPlugin())
        registerPlugin(DefaultInputManagerPlugin())
        registerPlugin(DefaultServicePlugin())
    }

    private fun registerPlugin(plugin: BasePlugin) {
        plugins[plugin.id] = plugin
        pluginStates[plugin.id] = PluginState.UNLOADED
        Log.d(TAG, "Registered plugin: ${plugin.id} (${plugin.name})")
    }

    // ===== Notifications =====

    private fun notifyPluginsLoaded() {
        pluginListeners.forEach { listener ->
            try {
                listener.onPluginsLoaded(getAllPlugins())
            } catch (e: Exception) {
                Log.w(TAG, "Plugin listener failed", e)
            }
        }
    }

    private fun notifyPluginStateChanged(plugin: BasePlugin, newState: PluginState) {
        pluginListeners.forEach { listener ->
            try {
                listener.onPluginStateChanged(plugin, newState)
            } catch (e: Exception) {
                Log.w(TAG, "Plugin listener failed", e)
            }
        }
    }

    interface PluginListener {
        fun onPluginsLoaded(plugins: List<BasePlugin>)
        fun onPluginStateChanged(plugin: BasePlugin, newState: PluginState)
    }
}


