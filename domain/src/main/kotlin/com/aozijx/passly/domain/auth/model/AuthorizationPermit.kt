package com.aozijx.passly.domain.auth.model

/**
 * 不可伪造的授权许可。
 *
 * 真正的 token 存储在 [com.aozijx.passly.domain.auth.port.AuthorizationGate] 内部注册表中：
 * - 具备不可预测 token ID
 * - purpose 绑定
 * - 单次消费
 * - 超时失效（使用单调时间）
 * - 锁定时全部撤销
 *
 * 外部代码可以实现这个标记接口，但无法把伪造实例加入内部注册表；
 * 只有授权闸门签发的对象身份才能被验证器消费。
 */
interface AuthorizationPermit
