package com.aozijx.passly.domain.auth.policy

import com.aozijx.passly.domain.auth.model.AuthRequirement
import com.aozijx.passly.domain.authentication.AuthenticationPurpose

/**
 * 认证策略。
 *
 * 决定每个 [AuthenticationPurpose] 的不可变 [AuthRequirement] 配置。
 * 一次返回完整 Requirement，避免分别调用多个方法读到不同配置状态。
 *
 * Policy 不参与 Grant 签发，后者由 [com.aozijx.passly.domain.auth.port.VaultAccessGate] 内部负责。
 */
interface AuthPolicy {

    /**
     * 获取指定目的的一次性完整策略要求。
     *
     * 返回空 [allowedMethods] 表示该目的不可用。
     */
    fun requirementFor(purpose: AuthenticationPurpose): AuthRequirement
}

enum class AuthMethodType {
    BIOMETRIC,
    APP_PASSWORD,
    RECOVERY_CODE,
}
