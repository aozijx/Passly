package com.aozijx.passly.core.autofill.model

/**
 * 解耦系统 API 的纯数据填充请求。
 *
 * 严禁包含任何 android.service.autofill 或 android.service.credentials 包引用。
 * 仅通过纯 Kotlin 数据类描述"用户想填什么，目标 App 有什么字段"。
 */
data class InternalFillRequest(
    /** 来源 App 包名（如 "com.android.chrome"） */
    val parentPackage: String,
    /** Web 场景下的域名（如 "accounts.google.com"），非 Web 场景为 null */
    val webDomain: String? = null,
    /** 页面标题/Activity 名称 */
    val activity: String? = null,
    /** 结构中的所有字段描述 */
    val fields: List<FieldDescriptor>,
    /** 目标 App 与应用当前进程是否同包（同包时可跳过部分安全检查） */
    val isInlineRequest: Boolean = false,
)