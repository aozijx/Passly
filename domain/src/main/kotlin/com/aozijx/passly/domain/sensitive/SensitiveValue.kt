package com.aozijx.passly.domain.sensitive

/**
 * 敏感值的稳定抽象，用于 Contract 层避免直接依赖安全实现层。
 *
 * 实现类负责安全地管理内存中的敏感数据（如密码、恢复码），
 * 并提供 wipe() 机制确保使用后清除。
 */
interface SensitiveValue {
    val isEmpty: Boolean
    fun toCharArray(): CharArray
    fun wipe()
}

/**
 * 空敏感值，作为 [SensitiveValue] 的默认值，
 * 避免 Contract 层依赖具体安全实现。
 */
object EmptySensitiveValue : SensitiveValue {
    override val isEmpty: Boolean = true
    override fun toCharArray(): CharArray = CharArray(0)
    override fun wipe() {}
}