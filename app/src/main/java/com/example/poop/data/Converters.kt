package com.example.poop.data

import androidx.room.TypeConverter

/**
 * Room 数据库类型转换器
 * 用于处理 SQLite 原生不支持的复杂数据类型（如 List、Boolean 等）
 */
class Converters {

    /**
     * 将 Boolean 转换为数据库存储的 Int (0/1)
     * SQLite 不直接支持布尔类型，通常使用 0 和 1 表示
     */
    @TypeConverter
    fun fromBoolean(value: Boolean?): Int? {
        return value?.let { if (it) 1 else 0 }
    }

    /**
     * 将数据库存储的 Int (0/1) 转换为 Boolean
     * 用于从数据库读取数据时还原布尔状态
     */
    @TypeConverter
    fun toBoolean(value: Int?): Boolean? {
        return value?.let { it == 1 }
    }

    /**
     * 将 List<String> 转换为逗号分隔的字符串
     * 用于将多个标签 (tags) 或网址 (uriList) 序列化为单个字符串存储在数据库中
     */
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString(",")
    }

    /**
     * 将逗号分隔的字符串还原为 List<String>
     * 用于从数据库读取后，将扁平化的字符串重新解析为可操作的列表对象
     */
    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
    }
}
