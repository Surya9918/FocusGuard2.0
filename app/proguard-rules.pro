# ─────────────────────────────────────────────────────────────────────────────
# FocusGuard — proguard-rules.pro
#
# Critical: TFLite and Room use reflection internally.
# Without these rules, release builds will crash silently.
# ─────────────────────────────────────────────────────────────────────────────

# ─── TensorFlow Lite ──────────────────────────────────────────────────────────
# TFLite's JNI layer looks up Java classes by name at runtime.
# Obfuscating these class names breaks the native bridge.
-keep class org.tensorflow.lite.** { *; }
-keep class org.tensorflow.lite.gpu.** { *; }
-keep class org.tensorflow.lite.support.** { *; }
-dontwarn org.tensorflow.**

# ─── Room Database ────────────────────────────────────────────────────────────
# Room generates DAO implementation classes at compile time.
# Keep all entity and DAO classes intact.
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-dontwarn androidx.room.**

# ─── App Classes ──────────────────────────────────────────────────────────────
-keep class com.focusguard.app.ml.** { *; }
-keep class com.focusguard.app.data.** { *; }
-keep class com.focusguard.app.service.** { *; }

# ─── General AndroidX ─────────────────────────────────────────────────────────
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes EnclosingMethod

# Preserve line numbers in crash stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
