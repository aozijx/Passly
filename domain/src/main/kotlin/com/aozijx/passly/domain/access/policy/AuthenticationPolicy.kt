package com.aozijx.passly.domain.access.policy

import com.aozijx.passly.domain.access.model.AuthenticationRequirement
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationPurpose

/**
 * 认证策略。
 *
 * 决定每个 [AuthenticationPurpose] 的不可变 [AuthenticationRequirement] 配置。
 * 一次返回完整 Requirement，避免分别调用多个方法读到不同配置状态。
 *
 * Policy 不参与 Grant 签发，后者由 [com.aozijx.passly.domain.access.port.AuthorizationGate] 内部负责。
 */
fun interface AuthenticationPolicy {

    /**
     * 获取指定目的的一次性完整策略要求。
     *
     * 返回空 [allowedMethods] 表示该目的不可用。
     */
    fun requirementFor(purpose: AuthenticationPurpose): AuthenticationRequirement
}
