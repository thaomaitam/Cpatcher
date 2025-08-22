package io.github.cpatcher

import io.github.cpatcher.arch.IHook
import io.github.cpatcher.arch.ObfsInfo
import io.github.cpatcher.arch.createObfsTable
import io.github.cpatcher.arch.hookAllBefore

class SpotifyHandler : IHook() {
    companion object {
        private const val KEY_GET_REMOTE_CONFIG = "get_remote_config"
    }

    override fun onHook() {
        val tbl = createObfsTable("spotify", 1) { bridge ->
            val method = bridge.findMethod {
                matcher {
                    returnType = "java.lang.Enum"
                    paramTypes("java.lang.String", "java.lang.String", "java.lang.Enum")
                }
            }.single()

            mutableMapOf(
                KEY_GET_REMOTE_CONFIG to ObfsInfo(method.className, method.methodName)
            )
        }

        val info = tbl[KEY_GET_REMOTE_CONFIG]!!
        findClass(info.className).hookAllBefore(info.memberName) { param ->
            if (param.args[0] == "android-playlist-creation-createplaylistmenuimpl"
                && param.args[1] == "create_button_position"
            ) {
                param.args[1] = ""
            }
        }
    }
}
