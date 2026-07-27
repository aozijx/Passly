package com.aozijx.passly.core.autofill.model

/**
 * 字段角色：标识自动填充字段的类型。
 *
 * 由 [com.aozijx.passly.core.autofill.matcher.FieldMatchStrategy] 通过启发式或严格规则解析。
 * 用于 [com.aozijx.passly.core.autofill.pipeline.ResponseFactory] 构建字段填充映射。
 */
enum class FieldRole {
    PASSWORD,
    USERNAME,
    OTP,
    SUBMIT,
    UNKNOWN,
}
