# ProGuard rules for Game Launcher

# Preserve our own classes
-keep class com.gamebooster.launcher.** { *; }

# Preserve data classes
-keep class com.gamebooster.launcher.GameInfo { *; }

# Android framework
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Preserve enum names
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Preserve R classes
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }

# AndroidX
-keep class androidx.** { *; }
-keep interface androidx.** { *; }

# Material Design
-keep class com.google.android.material.** { *; }
-keep interface com.google.android.material.** { *; }
