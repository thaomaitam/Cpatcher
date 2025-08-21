# proguard-rules.pro
# Cpatcher ProGuard configuration

# Basic Android optimizations
-allowaccessmodification
-overloadaggressively
-repackageclasses

# Keep Xposed entry point
-keep class io.github.cpatcher.Entry {
    public <methods>;
}

# Keep all Hook classes
-keep class io.github.cpatcher.core.arch.IHook
-keep class * extends io.github.cpatcher.core.arch.IHook {
    public <methods>;
    protected <methods>;
}

# Keep all Handler classes
-keepnames class * extends io.github.cpatcher.core.arch.IHook
-keep class io.github.cpatcher.handlers.** {
    public <methods>;
    protected <methods>;
}

# Keep Bridge classes (needed for Xposed integration)
-keep class io.github.cpatcher.core.bridge.** {
    public <methods>;
    protected <methods>;
}

# Keep ReVanced extension classes (if using submodule)
-keep class app.revanced.extension.** {
    public <methods>;
    public <fields>;
}

# Keep DexKit related classes
-keep class org.luckypray.dexkit.** {
    public <methods>;
}

# Keep Xposed API classes
-keep class de.robv.android.xposed.** {
    public <methods>;
}

# Remove debugging code
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Keep custom Logger but remove debug logs in release
-assumenosideeffects class io.github.cpatcher.core.utils.Logger {
    public static void d(...);
}

# Remove Kotlin intrinsics (reduce size)
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void check*(...);
    public static void throw*(...);
}

# Keep obfuscation info classes
-keep class io.github.cpatcher.core.arch.ObfsInfo {
    public <fields>;
    public <methods>;
}

# Keep JSON serialization classes
-keepclassmembers class ** {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# Keep annotation classes
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable

# Keep generic signatures for reflection
-keepattributes Signature

# Specific rules for TikTok patches
-keep class io.github.cpatcher.handlers.tiktok.** {
    public <methods>;
    protected <methods>;
}

# Keep cache classes
-keep class io.github.cpatcher.core.utils.Cache {
    public <methods>;
}

# Don't obfuscate hook method names (for debugging)
-keepclassmembernames class * extends io.github.cpatcher.core.arch.IHook {
    protected void onHook();
}

# Remove unused resources
-shrinkresources

# Optimize method calls
-optimizationpasses 5
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

# Keep reflection usage
-keepattributes InnerClasses
-keep class java.lang.reflect.** { *; }

# Warning suppressions
-dontwarn org.slf4j.**
-dontwarn org.apache.commons.**
-dontwarn org.apache.http.**

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Final size optimization
-verbose