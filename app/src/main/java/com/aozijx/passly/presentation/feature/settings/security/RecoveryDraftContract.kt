package com.aozijx.passly.presentation.feature.settings.security

sealed interface RecoveryDraftState {
    data object Empty : RecoveryDraftState
    data object Creating : RecoveryDraftState
    data class Ready(val generationId: String) : RecoveryDraftState
    data object DraftExpired : RecoveryDraftState
    data object Committed : RecoveryDraftState
    data object Failed : RecoveryDraftState
}

sealed interface RecoveryDraftAction {
    data object Generate : RecoveryDraftAction
    data object ConfirmAndEnable : RecoveryDraftAction
    data object Dismiss : RecoveryDraftAction
    data object Copy : RecoveryDraftAction
}

sealed interface RecoveryDraftEffect {
    data object Copied : RecoveryDraftEffect
}

fun RecoveryDraftState.messageOrNull(): String? = when (this) {
    RecoveryDraftState.DraftExpired -> "恢复码草稿已过期，请重新认证后生成。"
    else -> null
}
