package com.dexpro.launcher.plugin

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

/**
 * Shell plugin managing desktop wallpaper.
 *
 * Features:
 * - Set wallpaper from URI or resource
 * - Per-workspace independent wallpapers
 * - Slideshow mode (auto-rotate from a folder)
 * - Blur/scale/render effects
 */
class DefaultWallpaperPlugin : BasePlugin() {

    override val id = "shell.wallpaper"
    override val name = "Wallpaper Manager"
    override val type = PluginType.SHELL
    override val version = "1.0"
    override val author = "DeX Pro"
    override val description = "Manages desktop wallpaper with per-workspace support and effects"

    private val wallpaperMap = mutableMapOf<Int, Uri>()     // workspace → wallpaper URI
    private var currentWorkspace = 1
    private var slideshowEnabled = false
    private var slideshowInterval = 60000L // 60s

    fun setWallpaper(workspace: Int, uri: Uri) {
        wallpaperMap[workspace] = uri
    }

    fun getWallpaper(workspace: Int): Uri? {
        return wallpaperMap[workspace]
    }

    fun setCurrentWorkspace(workspace: Int) {
        currentWorkspace = workspace
    }

    fun enableSlideshow(enabled: Boolean) {
        slideshowEnabled = enabled
    }

    fun setSlideshowInterval(millis: Long) {
        slideshowInterval = millis
    }

    fun loadWallpaperBitmap(workspace: Int): Bitmap? {
        val uri = wallpaperMap[workspace] ?: return null
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun onSettingsChanged(key: String, value: Any?) {
        when (key) {
            "wallpaper_uri" -> {
                val uri = value as? Uri ?: return
                setWallpaper(currentWorkspace, uri)
            }
            "slideshow_enabled" -> {
                slideshowEnabled = value as? Boolean ?: false
            }
        }
    }
}