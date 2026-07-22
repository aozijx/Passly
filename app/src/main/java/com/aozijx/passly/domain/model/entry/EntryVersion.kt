package com.aozijx.passly.domain.model.entry

/**
 * 条目版本值类型。
 * 版本号的唯一真相源是 [VaultMetadataEntity.entryVersion]，
 * 不再在加密 JSON (VaultMetadata) 或 UI 中维护副本。
 */
@JvmInline
value class EntryVersion(val value: Int) {
    companion object {
        /** 初始版本号（新创建条目）。 */
        val INITIAL: EntryVersion = EntryVersion(1)

        fun fromInt(value: Int): EntryVersion = EntryVersion(value)
    }

    fun next(): EntryVersion = EntryVersion(value + 1)
}
