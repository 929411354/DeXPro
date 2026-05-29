package com.dexpro.launcher.cast

import android.content.Context
import android.media.MediaRouter
import android.os.Build
import android.util.Log
import android.widget.Toast

/**
 * Screen casting / streaming manager using Android's built-in MediaRouter.
 *
 * Supports:
 * - Miracast (WiFi Direct display)
 * - Chromecast (if Google Cast Receiver app installed)
 * - DLNA / AirPlay (via third-party receivers)
 * - Wired HDMI (via DisplayPort Alt Mode)
 *
 * This is a system-level API that doesn't require Google Cast SDK.
 * Works on Android 4.2+ (API 17+).
 */
class CastManager private constructor(context: Context) : MediaRouter.Callback() {
    override fun onRouteGrouped(router: MediaRouter, route: MediaRouter.RouteInfo, group: MediaRouter.RouteGroup, index: Int) {}
    override fun onRouteUngrouped(router: MediaRouter, route: MediaRouter.RouteInfo, group: MediaRouter.RouteGroup) {}


    companion object {
        private const val TAG = "DeXPro-Cast"
        private var instance: CastManager? = null

        @Synchronized
        fun getInstance(context: Context): CastManager {
            return instance ?: CastManager(context.applicationContext).also {
                instance = it
            }
        }
    }

    private val appContext = context.applicationContext
    private val mediaRouter = appContext.getSystemService(Context.MEDIA_ROUTER_SERVICE) as MediaRouter
    private var selectedRoute: MediaRouter.RouteInfo? = null
    private var isCasting = false

    /**
     * Start discovering available cast receivers.
     */
    fun startDiscovery() {
        mediaRouter.addCallback(
            MediaRouter.ROUTE_TYPE_LIVE_VIDEO,
            this,
            MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN
        )
        Log.d(TAG, "Started casting discovery")
    }

    /**
     * Stop discovery to save battery.
     */
    fun stopDiscovery() {
        mediaRouter.removeCallback(this)
        Log.d(TAG, "Stopped casting discovery")
    }

    /**
     * Get all available cast routes.
     */
    fun getAvailableRoutes(): List<MediaRouter.RouteInfo> {
        val routes = mutableListOf<MediaRouter.RouteInfo>()
        val count = mediaRouter.routeCount
        for (i in 0 until count) {
            val route = mediaRouter.getRouteAt(i)
            if (route.supportedTypes and MediaRouter.ROUTE_TYPE_LIVE_VIDEO != 0) {
                routes.add(route)
            }
        }
        return routes
    }

    /**
     * Select a route and start casting.
     * @return true if casting started successfully
     */
    fun startCasting(route: MediaRouter.RouteInfo): Boolean {
        return try {
            mediaRouter.selectRoute(MediaRouter.ROUTE_TYPE_LIVE_VIDEO, route)
            selectedRoute = route
            isCasting = true
            Log.i(TAG, "Started casting to: ${route.name} (${route.description})")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for casting", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start casting", e)
            false
        }
    }

    /**
     * Stop current casting session.
     */
    fun stopCasting() {
        selectedRoute?.let { route ->
            mediaRouter.selectRoute(MediaRouter.ROUTE_TYPE_LIVE_VIDEO, mediaRouter.defaultRoute)
            selectedRoute = null
            isCasting = false
            Log.i(TAG, "Stopped casting")
        }
    }

    /**
     * Get current casting status.
     */
    fun isCasting(): Boolean = isCasting

    /**
     * Get currently selected route.
     */
    fun getSelectedRoute(): MediaRouter.RouteInfo? = selectedRoute

    // ===== MediaRouter.Callback overrides =====

    override fun onRouteAdded(router: MediaRouter, route: MediaRouter.RouteInfo) {
        Log.d(TAG, "Route added: ${route.name}")
        Toast.makeText(appContext, "Found cast device: ${route.name}", Toast.LENGTH_SHORT).show()
    }

    override fun onRouteRemoved(router: MediaRouter, route: MediaRouter.RouteInfo) {
        Log.d(TAG, "Route removed: ${route.name}")
        if (route == selectedRoute) {
            stopCasting()
            Toast.makeText(appContext, "Cast device disconnected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRouteSelected(router: MediaRouter, type: Int, route: MediaRouter.RouteInfo) {
        Log.d(TAG, "Route selected: ${route.name}")
        selectedRoute = route
        isCasting = true
    }

    override fun onRouteUnselected(router: MediaRouter, type: Int, route: MediaRouter.RouteInfo) {
        Log.d(TAG, "Route unselected: ${route.name}")
        if (route == selectedRoute) {
            selectedRoute = null
            isCasting = false
        }
    }

    override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) {
        Log.d(TAG, "Route changed: ${route.name}")
    }

    override fun onRouteVolumeChanged(router: MediaRouter, route: MediaRouter.RouteInfo) {
        Log.d(TAG, "Route volume changed: ${route.name}")
    }

    override fun onRoutePresentationDisplayChanged(
        router: MediaRouter,
        route: MediaRouter.RouteInfo
    ) {
        Log.d(TAG, "Route presentation display changed: ${route.name}")
    }

    /**
     * Clean up resources.
     */
    fun release() {
        stopDiscovery()
        stopCasting()
        instance = null
    }
}