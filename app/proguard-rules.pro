# DeX Pro ProGuard Rules
# ========================

# Keep Shizuku API
-keep class rikka.shizuku.** { *; }
-keepclassmembers class rikka.shizuku.** { *; }
-dontwarn rikka.shizuku.**

# Keep Shizuku Provider
-keep class moe.shizuku.** { *; }
-dontwarn moe.shizuku.**

# Keep Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# Keep DataStore
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# Keep AppInfo data class (used via reflection)
-keep class com.dexpro.launcher.AppInfo { *; }

# Keep WindowManager data classes
-keep class com.dexpro.launcher.window.WindowManager$WindowMeta { *; }
-keep class com.dexpro.launcher.window.SnapEdge { *; }

# Keep accessibility service
-keep class com.dexpro.launcher.service.TaskbarAccessibilityService { *; }

# Keep activities
-keep class com.dexpro.launcher.MainActivity { *; }
-keep class com.dexpro.launcher.DesktopActivity { *; }
-keep class com.dexpro.launcher.SettingsActivity { *; }

# Keep adapters (ViewHolder reflection)
-keep class com.dexpro.launcher.ui.** { *; }

# General Android rules
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes Signature
-keepattributes Exceptions

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}