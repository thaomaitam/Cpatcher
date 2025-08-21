package io.github.cpatcher.handlers.youtube

import io.github.cpatcher.core.arch.IHook
import io.github.cpatcher.handlers.youtube.patches.VideoAds
import io.github.cpatcher.handlers.youtube.patches.SponsorBlock

class YouTubeHandler : IHook() {
    override fun onHook() {
        subHook(VideoAds())
        subHook(SponsorBlock())
    }
}