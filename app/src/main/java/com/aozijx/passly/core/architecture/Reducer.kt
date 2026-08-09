package com.aozijx.passly.core.architecture

/**
 * MVI Reducer: 纯函数，将当前状态和意图转换为新状态。
 *
 * 使用场景：
 * - 当状态转换逻辑简单且可测试时，使用 Reducer 提取纯函数
 * - 当需要异步操作（数据库、网络、加密）时，在 ViewModel.onIntent 中处理
 *
 * @param S 状态类型（通常是 UiState data class）
 * @param I 意图类型（通常是 Intent sealed interface）
 */
fun interface Reducer<S, I> {
    fun reduce(state: S, intent: I): S
}