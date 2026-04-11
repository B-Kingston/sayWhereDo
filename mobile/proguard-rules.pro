# ============================================================
# ProGuard / R8 rules for the mobile module
# ============================================================

# Preserve source file names and line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---------- Room ----------

# Room entity classes must retain their field names so SQL column
# mapping works after obfuscation.
-keep class com.example.reminders.data.model.** { *; }

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

# Keep the serializer companion objects so reflection-based lookups
# find them at runtime.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep @Serializable classes and their serializers
-keep @kotlinx.serialization.Serializable class *
-keepclassmembers class * {
    *** Companion;
    *** serializer(...);
}

# ---------- OkHttp ----------

# OkHttp uses reflection for platform-specific TLS setup.
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# OkHttp interceptor chains reference method names in logging.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# ---------- Play Services (Billing, Location, Wearable) ----------

# Play Services uses reflection to load API client factories.
-keep class com.google.android.gms.internal.** { *; }
-dontwarn com.google.android.gms.internal.**

# BillingClient and related types are used across process boundaries.
-keep class com.android.billingclient.api.** { *; }

# Play Services wearable and location API stubs
-keep class com.google.android.gms.wearable.** { *; }
-keep class com.google.android.gms.location.** { *; }
-dontwarn com.google.android.gms.wearable.**

# ---------- WorkManager ----------

# WorkManager creates Worker instances via reflection.
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }

# ---------- Kotlin coroutines ----------

# Coroutine debug artefacts — harmless to keep, helpful for stack traces.
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ---------- General Android ----------

# ViewModels are created via reflection by ViewModelProvider.Factory.
-keep class * extends androidx.lifecycle.ViewModel { <init>(...); }

# Navigation Compose uses reflection for route serialization.
-keepnames class androidx.navigation.compose.**
