package io.github.cpatcher.bridge;

import android.content.pm.ApplicationInfo;

public class LoadPackageParam {
    public final String packageName;
    public final String processName;
    public final ClassLoader classLoader;
    public final ApplicationInfo appInfo;
    public final boolean isFirstApplication;
    public LoadPackageParam(de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam p) {
        packageName = p.packageName;
        processName = p.processName;
        classLoader = p.classLoader;
        appInfo = p.appInfo;
        isFirstApplication = p.isFirstApplication;
    }
}
