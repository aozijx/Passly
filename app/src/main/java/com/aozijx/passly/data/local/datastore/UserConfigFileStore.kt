package com.aozijx.passly.data.local.datastore

import android.content.Context
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.domain.model.UserConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserConfigFileStore @Inject constructor(
    @param:ApplicationContext private val appContext: Context
) {
    private val mutex = Mutex()

    private val configFile: File by lazy {
        val dir = File(appContext.filesDir, "config").apply { if (!exists()) mkdirs() }
        File(dir, "user_config.json")
    }

    suspend fun read(): UserConfig = mutex.withLock {
        withContext(Dispatchers.IO) {
            if (!configFile.exists()) return@withContext UserConfig()
            try {
                val raw = configFile.readText()
                parseConfig(raw)
            } catch (e: Exception) {
                Logcat.e("UserConfigFileStore", "Failed to read config file", e)
                UserConfig()
            }
        }
    }

    suspend fun write(config: UserConfig) = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                configFile.writeText(config.toJson())
            } catch (e: Exception) {
                Logcat.e("UserConfigFileStore", "Failed to write config file", e)
            }
        }
    }
}

private fun parseConfig(raw: String): UserConfig {
    if (raw.isBlank()) return UserConfig()
    return try {
        val obj = JSONObject(raw)
        val version = obj.optInt("version", 1)
        val updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
        val categories = obj.optJSONArray("customCategories").toStringList()
        val tags = obj.optJSONArray("customTags").toStringList()
        val extras = obj.optJSONObject("extras").toStringMap()
        UserConfig(
            version = version,
            customCategories = categories,
            customTags = tags,
            extras = extras,
            updatedAt = updatedAt
        )
    } catch (_: Exception) {
        UserConfig()
    }
}

private fun UserConfig.toJson(): String {
    val obj = JSONObject()
    obj.put("version", version)
    obj.put("updatedAt", updatedAt)
    obj.put("customCategories", JSONArray(customCategories))
    obj.put("customTags", JSONArray(customTags))
    val extrasObj = JSONObject()
    extras.forEach { (key, value) -> extrasObj.put(key, value) }
    obj.put("extras", extrasObj)
    return obj.toString()
}

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    if (length() == 0) return emptyList()
    val list = ArrayList<String>(length())
    for (i in 0 until length()) {
        val value = optString(i, "")
        if (value.isNotBlank()) list.add(value)
    }
    return list
}

private fun JSONObject?.toStringMap(): Map<String, String> {
    if (this == null) return emptyMap()
    val map = LinkedHashMap<String, String>()
    val keys = keys()
    while (keys.hasNext()) {
        val key = keys.next()
        val value = optString(key, "")
        if (key.isNotBlank() && value.isNotBlank()) {
            map[key] = value
        }
    }
    return map
}