package com.aozijx.passly.domain.model.entry

/**
 * 条目 ID 值类型。
 * 在领域层和 Command Handler API 中使用，避免裸 String 传递。
 */
@JvmInline
value class EntryId(val value: String) {
    companion object {
        fun fromString(value: String): EntryId = EntryId(value)
    }
}
