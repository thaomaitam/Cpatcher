// core/arch/ObfsUtils.kt
package io.github.cpatcher.core.arch

import android.content.pm.ApplicationInfo
import io.github.cpatcher.core.utils.Logger
import org.json.JSONObject
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.result.FieldData
import org.luckypray.dexkit.result.MethodData
import java.io.File

val _loadDexKit by lazy { System.loadLibrary("dexkit") }

typealias ObfsTable = Map<String, ObfsInfo>

data class ObfsInfo(
    val className: String,
    val memberName: String,
)

fun MethodData.toObfsInfo() = ObfsInfo(className = className, memberName = methodName)
fun FieldData.toObfsInfo() = ObfsInfo(className = className, memberName = fieldName)

const val OBFS_KEY_APK = "apkPath"
const val OBFS_KEY_TABLE_VERSION = "tableVersion"
const val OBFS_KEY_FRAMEWORK_VERSION = "frameworkVersion"
const val OBFS_KEY_PREFIX_METHOD = "method_"
const val OBFS_FRAMEWORK_VERSION = 1

fun IHook.createObfsTable(
    name: String,
    tableVersion: Int,
    pathProvider: (ApplicationInfo) -> String = { it.sourceDir },
    creator: (DexKitBridge) -> ObfsTable
): ObfsTable {
    val appInfo = loadPackageParam.appInfo
    val apkPath = pathProvider(appInfo)
    val tableFile = File(appInfo.dataDir, "cache/obfs_table_$name.json")
    
    tableFile.parentFile?.mkdirs()
    
    // Try load from cache
    val cachedTable = loadCachedTable(tableFile, apkPath, tableVersion)
    if (cachedTable != null) {
        Logger.i("Using cached obfs-table: $name")
        return cachedTable
    }
    
    // Create new table
    _loadDexKit
    val dexKitBridge = DexKitBridge.create(apkPath)
    return creator(dexKitBridge).also { table ->
        saveTableToCache(table, tableFile, apkPath, tableVersion)
        Logger.i("Created obfs-table: $name")
    }
}

private fun loadCachedTable(tableFile: File, apkPath: String, tableVersion: Int): ObfsTable? {
    if (!tableFile.isFile) return null
    
    return try {
        val json = JSONObject(tableFile.readText())
        
        // Validation
        if (json.getInt(OBFS_KEY_FRAMEWORK_VERSION) != OBFS_FRAMEWORK_VERSION) return null
        if (json.getString(OBFS_KEY_APK) != apkPath) return null
        if (json.getInt(OBFS_KEY_TABLE_VERSION) != tableVersion) return null
        
        // Parse table
        val outMap = mutableMapOf<String, ObfsInfo>()
        for (k in json.keys()) {
            if (k.startsWith("method_")) {
                val rk = k.removePrefix(OBFS_KEY_PREFIX_METHOD)
                val rv = json[k] as JSONObject
                outMap[rk] = ObfsInfo(
                    className = rv.getString("className"),
                    memberName = rv.getString("methodName")
                )
            }
        }
        outMap
    } catch (e: Exception) {
        Logger.e("Failed to load cached table", e)
        null
    }
}

private fun saveTableToCache(table: ObfsTable, tableFile: File, apkPath: String, tableVersion: Int) {
    try {
        val json = JSONObject().apply {
            put(OBFS_KEY_APK, apkPath)
            put(OBFS_KEY_TABLE_VERSION, tableVersion)
            put(OBFS_KEY_FRAMEWORK_VERSION, OBFS_FRAMEWORK_VERSION)
            
            for ((k, v) in table) {
                put(OBFS_KEY_PREFIX_METHOD + k, JSONObject().apply {
                    put("className", v.className)
                    put("methodName", v.memberName)
                })
            }
        }
        tableFile.writeText(json.toString())
    } catch (e: Exception) {
        Logger.e("Failed to save table cache", e)
    }
}