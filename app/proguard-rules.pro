-keepattributes Signature
-keepattributes *Annotation*
-keep class com.carlauncher.data.model.** { *; }
-dontwarn kotlinx.coroutines.**

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }

# Keep serializer classes
-keep,includedescriptorclasses class com.carlauncher.data.model.**$$serializer { *; }
-keepclassmembers class com.carlauncher.data.model.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
