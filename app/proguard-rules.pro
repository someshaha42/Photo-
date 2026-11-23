# Android ProGuard & R8 Configuration for PixelForge

# 1. Native JNI Bridge preservation (C++ NDK)
-keepclassmembers class * {
    native <methods>;
}
-keep class com.example.compat.NativePixelEngine { *; }
-keep class com.example.compat.LegacyCompatBridge { *; }

# 2. Kotlin Serialization preservation
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * {
    *** Companion;
}
-keepclassmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# 3. Room Database preservation
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# 4. Standard Compose and Coroutines optimization
-dontwarn java.lang.invoke.**
