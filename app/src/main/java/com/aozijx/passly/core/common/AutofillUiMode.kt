package com.aozijx.passly.core.common

enum class AutofillUiMode(
    val key: String,
    val displayName: String,
    val description: String
) {
    SYSTEM_INLINE("inline", "键盘候选", "优先使用系统候选条，贴近输入位置"),
    BOTTOM_SHEET("bottom_sheet", "底部弹层", "以半屏底部弹层展示，类似浏览器样式");

    companion object {
        fun fromKey(key: String?): AutofillUiMode {
            return entries.firstOrNull { it.key == key } ?: SYSTEM_INLINE
        }
    }
}

