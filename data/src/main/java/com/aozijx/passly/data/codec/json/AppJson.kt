package com.aozijx.passly.data.codec.json

import kotlinx.serialization.json.Json

/**
 * 全局共享的 JSON 序列化器实例。
 * 所有 Mapper、Serializer、Repository 统一引用此实例。
 * 不可变且线程安全，可在任意协程/线程中安全使用。
 */
val AppJson = Json {
    // 是否序列化默认值（推荐 true，确保反序列化时字段不缺失）
    encodeDefaults = true

    // 忽略未知 key（导入旧版本备份文件时非常重要）
    ignoreUnknownKeys = true

    // 生产环境关闭缩进，减小备份文件体积
    prettyPrint = false

    // 可选：对枚举未知值降级（避免旧数据崩溃）
    coerceInputValues = true
}