package com.aozijx.passly.domain.model

/**
 * 用户自定义配置（单独文件保存）。
 */
data class UserConfig(
    val version: Int = 1,
    val customCategories: List<String> = emptyList(),
    val customTags: List<String> = emptyList(),
    val extras: Map<String, String> = emptyMap(),
    val updatedAt: Long = System.currentTimeMillis()
)
