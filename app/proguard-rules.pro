# Keep Xposed classes
-keep class de.robv.android.xposed.** { *; }
-dontwarn de.robv.android.xposed.**

# Keep MainHook entry point
-keep class com.KTA.devicespoof.hook.MainHook { *; }

# Keep Protobuf generated classes
-keep class com.KTA.devicespoof.proto.** { *; }
-dontwarn com.google.protobuf.**

# Keep JNI methods
-keepclasseswithmembernames class * {
    native <methods>;
}