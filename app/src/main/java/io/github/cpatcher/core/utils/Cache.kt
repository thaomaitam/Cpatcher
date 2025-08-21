// core/utils/Cache.kt
package io.github.cpatcher.core.utils

import android.content.Context
import java.io.File

object Cache {
    private lateinit var cacheDir: File
    
    fun init(context: Context) {
        cacheDir = File(context.cacheDir, "cpatcher")
        cacheDir.mkdirs()
    }
    
    fun getCacheFile(name: String): File {
        return File(cacheDir, name)
    }
    
    fun clearCache() {
        cacheDir.deleteRecursively()
        cacheDir.mkdirs()
    }
}