# NexusBlock ProGuard / R8 Rules
# Keep all reflection-heavy libraries intact

# LiteRT (TensorFlow Lite) — native library loading
-keep class org.tensorflow.lite.** { *; }
-keepclassmembers class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**
-keep class org.tensorflow.lite.support.** { *; }
-dontwarn org.tensorflow.lite.support.**

# QuickJS — JNI bindings (enable when dependency is added)
# -keep class com.dokar.quickjs.** { *; }
# -keepclassmembers class com.dokar.quickjs.** { *; }
# -dontwarn com.dokar.quickjs.**

# BouncyCastle — reflection for security providers
-keep class org.bouncycastle.** { *; }
-keepclassmembers class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# dnsjava — reflection for record types
-keep class org.xbill.DNS.** { *; }
-keepclassmembers class org.xbill.DNS.** { *; }
-dontwarn org.xbill.DNS.**

# Gson/Guava — keep generic signatures
-keepattributes Signature
-keepattributes *Annotation*

# Room — entities and DAOs
-keep class com.nexusblock.data.model.** { *; }
-keep class com.nexusblock.data.db.** { *; }
-keep class com.nexusblock.data.repository.** { *; }

# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }
-keepclassmembers,allowobfuscation class * {
    @javax.inject.* <fields>;
    @javax.inject.* <methods>;
}

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep class com.nexusblock.data.model.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Android TV / Compose
-keep class androidx.tv.** { *; }
-keep class androidx.compose.** { *; }
