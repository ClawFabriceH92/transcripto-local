# Add rules here to obfuscate your application
# See: https://developer.android.com/build/shrink-code

# Keep JNI classes
-keep class com.transcripto.local.stt.** { *; }
-keep class com.transcripto.local.llm.** { *; }

# Keep Room entities
-keep class com.transcripto.local.db.** { *; }

# Keep data classes used with Gson / JSON
-keep class com.transcripto.local.models.** { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}
