package io.github.cpatcher.bridge;

public abstract class HotLoadHook {
    public abstract void onLoad(LoadPackageParam param);

    public abstract void onUnload();
}
