package com.aozijx.passly.core.auth.session

import kotlinx.coroutines.flow.Flow

/**
 * 空闲超时设置接口
 * 定义在核心层，避免核心层直接依赖领域层的 UseCase
 */
interface IdleTimeoutSettings {
    val lockOnBackground: Flow<Boolean>
}