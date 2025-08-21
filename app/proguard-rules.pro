-allowaccessmodification
-overloadaggressively

-keep class io.github.cpatcher.Entry

-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void check*(...);
    public static void throw*(...);
}

-keepnames class * extends io.github.cpatcher.arch.IHook
-keep class io.github.cpatcher.bridge.* {
    *;
}
