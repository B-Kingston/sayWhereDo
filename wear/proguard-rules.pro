# ============================================================
# ProGuard / R8 rules for the wear module
# ============================================================

# Preserve source file names and line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---------- Room ----------

# Room entity classes must retain their field names so SQL column
# mapping works after obfuscation.
-keep class com.example.reminders.wear.data.** { *; }

# Room DAO interfaces are accessed via reflection by the generated
# implementation class.
-keep interface * extends androidx.room.Dao { *; }

# Database class and its subclasses must be kept for Room's
# generated open helper.
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class *

# Suppress warnings for Room-generated code
-dontwarn androidx.room.paging.**

# ---------- kotlinx-serialization ----------

-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep @kotlinx.serialization.Serializable class *
-keepclassmembers class * {
    *** Companion;
    *** serializer(...);
}

# ---------- Play Services (Location, Wearable) ----------

-keep class com.google.android.gms.internal.** { *; }
-dontwarn com.google.android.gms.internal.**

-keep class com.google.android.gms.wearable.** { *; }
-keep class com.google.android.gms.location.** { *; }
-dontwarn com.google.android.gms.wearable.**

# ---------- WorkManager ----------

-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }

# ---------- Kotlin coroutines ----------

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ---------- General Android ----------

-keep class * extends androidx.lifecycle.ViewModel { <init>(...); }
-keepnames class androidx.navigation.compose.**
