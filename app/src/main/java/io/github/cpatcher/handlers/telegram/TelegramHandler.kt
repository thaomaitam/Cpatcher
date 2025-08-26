package io.github.cpatcher.handlers.telegram

import io.github.cpatcher.handlers.telegramSettings
import io.github.cpatcher.arch.IHook
import io.github.cpatcher.bridge.LoadPackageParam
import io.github.cpatcher.logE
import io.github.cpatcher.logI
import java.io.File

abstract class DynHook : IHook() {
    private var hooked = false

    @Synchronized
    override fun hook(param: LoadPackageParam) {
        if (hooked) {
            // logD("already hooked ${this::class.simpleName}")
            return
        }
        if (!isEnabled()) {
            // logD("not enabled: ${this::class.java.simpleName}")
            return
        }
        // logD("hooking ${this::class.java.simpleName}")
        super.hook(param)
        hooked = true
    }

    abstract fun isFeatureEnabled(): Boolean

    protected fun isEnabled(): Boolean = !TelegramHandler.settings.disabled && isFeatureEnabled()
}

object TelegramHandler : IHook() {

    lateinit var settings: TelegramSettings
        private set
    private lateinit var settingFile: File

    fun updateSettings(s: TelegramSettings) {
        settings = s
        hooks.forEach {
            subHook(it as IHook)
        }
        runCatching {
            settingFile.outputStream().use {
                s.writeTo(it)
            }
        }.onFailure {
            logE("persist settings", it)
        }
    }

    private fun readSettings() {
        settings = runCatching {
            if (settingFile.canRead()) {
                settingFile.inputStream().use {
                    TelegramSettings.parseFrom(it)
                }
            } else {
                TelegramSettings.getDefaultInstance()
            }
        }.onFailure {
            logE("read settings failed", it)
            settingFile.delete()
        }.getOrDefault(TelegramSettings.getDefaultInstance())
        logI("current settings $settings")
    }

    private val hooks = mutableListOf<DynHook>()

    override fun onHook() {
        settingFile = File(loadPackageParam.appInfo.dataDir, "my_injector_settings")
        readSettings()

        subHook(OpenLinkDialog())
        subHook(MutualContact())
        subHook(ContactPermission())
        subHook(AutoCheckDeleteMessageOption())
        subHook(AutoUncheckSharePhoneNumber())
        subHook(DisableVoiceOrCameraButton())
        subHook(LongClickMention())
        subHook(FakeInstallPermission())
        subHook(NoGoogleMaps())
        subHook(CustomEmojiMapping)
        subHook(EmojiStickerMenu())
        subHook(FixHasAppToOpen())
        subHook(DefaultSearchTab())
        subHook(CustomMapPosition())
        subHook(AvatarPagerScrollToCurrent())
        subHook(SendImageWithHighQualityByDefault())
        subHook(HidePhoneNumber())
        subHook(Settings())
        subHook(AlwaysShowStorySaveIcon())
        subHook(RemoveArchiveFolder())
        subHook(AlwaysShowDownloadManager())
        subHook(HideFloatFab())
        subHook(OpenTgUserLink())
        subHook(CopyPrivateChatLink())
    }

    private fun subHook(hook: DynHook) {
        hooks.add(hook)
        subHook(hook as IHook)
    }
}
