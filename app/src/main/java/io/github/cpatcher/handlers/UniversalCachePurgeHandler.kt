package io.github.cpatcher.handlers

import android.app.Application
import android.content.Context
import io.github.cpatcher.arch.IHook
import io.github.cpatcher.arch.hookAllAfter
import io.github.cpatcher.arch.hookAfter
import io.github.cpatcher.logI
import io.github.cpatcher.logE
import java.io.File
import kotlin.concurrent.thread

class UniversalCachePurgeHandler : IHook() {
    companion object {
        // Configuration flags for selective purging
        private const val PURGE_ON_STARTUP = true
        private const val PURGE_INTERNAL_CACHE = true
        private const val PURGE_EXTERNAL_CACHE = true
        private const val PURGE_CODE_CACHE = true
        private const val PRESERVE_ROOT_DIRS = true
        private const val ASYNC_EXECUTION = true
    }
    
    override fun onHook() {
        // CRITICAL: Package-scoped execution verification
        val targetPackage = loadPackageParam.packageName
        val targetProcess = loadPackageParam.processName
        
        // Process isolation check - ensures main app process only
        if (targetProcess != targetPackage) {
            logI("${this::class.simpleName}: Skipping sub-process $targetProcess")
            return
        }
        
        // Strategy 1: Application lifecycle interception
        implementStartupPurge()
        
        // Strategy 2: Activity-based triggering (optional)
        implementActivityTrigger()
        
        logI("${this::class.simpleName}: Cache elimination active for $targetPackage")
    }
    
    private fun implementStartupPurge() {
        // Primary hook: Application.onCreate() - guaranteed single execution
        Application::class.java.hookAllAfter("onCreate") { param ->
            val application = param.thisObject as? Application ?: return@hookAllAfter
            
            if (PURGE_ON_STARTUP) {
                executeTargetedPurge(application, "startup")
            }
        }
        
        // Alternative hook: attachBaseContext for earlier intervention
        Application::class.java.hookAllAfter("attachBaseContext") { param ->
            val baseContext = param.args.getOrNull(0) as? Context ?: return@hookAllAfter
            
            // Deferred execution to prevent initialization interference
            if (ASYNC_EXECUTION) {
                thread(start = true) {
                    Thread.sleep(500) // Strategic delay for app stabilization
                    executeTargetedPurge(baseContext, "attach_base")
                }
            }
        }
    }
    
    private fun implementActivityTrigger() {
        // Optional: First activity launch trigger
        runCatching {
            val activityClass = classLoader.loadClass("android.app.Activity")
            
            activityClass.hookAfter("onCreate", 
                classLoader.loadClass("android.os.Bundle")) { param ->
                
                // One-time execution flag via static tracking
                val executed = param.thisObject.javaClass
                    .getDeclaredField("__cache_purged__")
                    .also { it.isAccessible = true }
                    .getBoolean(null)
                
                if (!executed) {
                    param.thisObject.javaClass
                        .getDeclaredField("__cache_purged__")
                        .also { it.isAccessible = true }
                        .setBoolean(null, true)
                    
                    val activity = param.thisObject as? Context
                    activity?.let { executeTargetedPurge(it, "activity") }
                }
            }
        }.onFailure {
            // Silent failure - activity trigger is optional
        }
    }
    
    private fun executeTargetedPurge(context: Context, trigger: String) {
        runCatching {
            val startTime = System.currentTimeMillis()
            val packageName = context.packageName
            
            // Cache directory resolution
            val cacheTargets = buildList<File> {
                // Internal cache: /data/data/[package]/cache
                if (PURGE_INTERNAL_CACHE) {
                    context.cacheDir?.let { add(it) }
                }
                
                // External cache: /sdcard/Android/data/[package]/cache
                if (PURGE_EXTERNAL_CACHE) {
                    context.externalCacheDir?.let { add(it) }
                    
                    // Additional external locations
                    context.getExternalFilesDirs(null)?.forEach { dir ->
                        dir?.parentFile?.let { parent ->
                            File(parent, "cache").takeIf { it.exists() }?.let { add(it) }
                        }
                    }
                }
                
                // Code cache: /data/data/[package]/code_cache
                if (PURGE_CODE_CACHE) {
                    context.codeCacheDir?.let { add(it) }
                }
            }
            
            // Execution strategy
            val purgeTask = {
                var totalSize = 0L
                var fileCount = 0
                
                cacheTargets.forEach { cacheDir ->
                    val result = performRecursiveDeletion(cacheDir, PRESERVE_ROOT_DIRS)
                    totalSize += result.first
                    fileCount += result.second
                }
                
                val duration = System.currentTimeMillis() - startTime
                logI("Cache purge [$trigger] for $packageName: " +
                    "${formatSize(totalSize)} cleared " +
                    "($fileCount files, ${duration}ms)")
            }
            
            if (ASYNC_EXECUTION) {
                thread(name = "CachePurge-$packageName", start = true) {
                    purgeTask()
                }
            } else {
                purgeTask()
            }
            
        }.onFailure { t ->
            logE("Cache purge failed [${context.packageName}]", t)
        }
    }
    
    private fun performRecursiveDeletion(
        file: File, 
        preserveRoot: Boolean
    ): Pair<Long, Int> {
        var totalSize = 0L
        var fileCount = 0
        
        if (!file.exists()) return Pair(0L, 0)
        
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                val result = performRecursiveDeletion(child, preserveRoot = false)
                totalSize += result.first
                fileCount += result.second
            }
            
            if (!preserveRoot && file.list()?.isEmpty() == true) {
                file.delete()
            }
        } else {
            totalSize = file.length()
            if (file.delete()) {
                fileCount++
            }
        }
        
        return Pair(totalSize, fileCount)
    }
    
    private fun formatSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.2f KB".format(bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> "%.2f MB".format(bytes / (1024.0 * 1024))
        else -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
    }
}