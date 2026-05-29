package com.dexpro.launcher

import android.graphics.drawable.Drawable

/**
 * Represents an installed Android app with its display info.
 */
data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable
)