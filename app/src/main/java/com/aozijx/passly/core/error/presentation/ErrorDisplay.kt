package com.aozijx.passly.core.error.presentation

import com.aozijx.passly.core.error.AppError
import com.aozijx.passly.core.error.ErrorSeverity

/**
 * 定义错误在 UI 上的展示方式。
 */
enum class UiDisplayLevel {
    /** 轻量提示 - 短暂 Toast，不打断操作流程 */
    TOAST,

    /** 中等提示 - Snackbar，可附带操作按钮 */
    SNACKBAR,

    /** 模态提示 - Dialog，需要用户确认 */
    DIALOG,

    /** 内联提示 - 嵌入在当前页面内容中 */
    INLINE
}

/**
 * 根据错误自身属性自动判断推荐的 UI 展示级别。
 *
 * 规则：
 * - 不可恢复的 ERROR  → DIALOG（需要用户明确知晓并确认）
 * - 可恢复的 ERROR    → SNACKBAR（严重但可恢复，附带操作）
 * - WARNING           → TOAST（轻量提醒）
 */
fun AppError.displayLevel(): UiDisplayLevel = when (severity) {
    ErrorSeverity.ERROR if !recoverable -> UiDisplayLevel.DIALOG
    ErrorSeverity.ERROR if recoverable -> UiDisplayLevel.SNACKBAR
    else -> UiDisplayLevel.TOAST
}
