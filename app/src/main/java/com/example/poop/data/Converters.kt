package com.example.poop.data

import androidx.room.TypeConverter

class Converters {
    /**
     * 将 Boolean 转换为数据库存储的 Int (0/1)
     */
    @TypeConverter
    fun fromBoolean(value: Boolean?): Int? {
        return value?.let { if (it) 1 else 0 }
    }

    /**
     * 将数据库存储的 Int (0/1) 转换为 Boolean
     */
    @TypeConverter
    fun toBoolean(value: Int?): Boolean? {
        return value?.let { it == 1 }
    }

    /**
     * 将 List<String> 转换为逗号分隔的字符串
     * 用于 uriList 和 tags 字段的存储
     */
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString(",")
    }

    /**
     * 将逗号分隔的字符串还原为 List<String>
     */
    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
    }
}
