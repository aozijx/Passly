package com.aozijx.passly.domain.auth.model

/**
 * 不可伪造的授权许可。
 *
 * 真正的 token 存储在 [com.aozijx.passly.domain.auth.port.VaultAccessGate] 内部注册表中：
 * - 具备不可预测 token ID
 * - purpose 绑定
 * - 单次消费
 * - 超时失效（使用单调时间）
 * - 锁定时全部撤销
 *
 * 外部代码无法构造 [AuthorizationPermit] 实现类。
 * 调用者只能读取 [purpose]，不能检查或消费内部 token。
 */
interface AuthorizationPermit {
    val purpose: com.aozijx.passly.domain.authentication.AuthenticationPurpose
}
