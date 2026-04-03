package com.aozijx.passly.domain.strategy

import com.aozijx.passly.core.common.EntryType
import com.aozijx.passly.domain.model.VaultEntry

/**
 * 条目类型策略基类
 *
 * 使用 Strategy 设计模式将不同条目类型的业务逻辑解耦。
 * 每种条目类型都有其专属的验证规则、字段映射和处理逻辑。
 */
interface EntryTypeStrategy {
    /**
     * 获取该策略对应的条目类型
     */
    val entryType: EntryType

    /**
     * 验证条目的必填字段
     * @return 为空表示验证通过，否则返回错误消息
     */
    fun validateRequiredFields(entry: VaultEntry): String?

    /**
     * 验证条目的字段内容
     * @return 为空表示验证通过，否则返回错误消息
     */
    fun validateFieldContent(entry: VaultEntry): String?

    /**
     * 获取该条目类型的敏感字段列表
     * 这些字段在日志、UI 预览等场景中应被掩码
     */
    fun getSensitiveFields(): Set<String>

    /**
     * 提取用于列表显示的摘要信息
     */
    fun extractSummary(entry: VaultEntry): String

    /**
     * 获取默认的分类建议
     */
    fun suggestedCategory(): String

    /**
     * 检查该类型是否支持自动填充
     */
    fun supportsAutofill(): Boolean

    /**
     * 执行类型特定的初始化逻辑
     */
    fun initializeDefaults(entry: VaultEntry): VaultEntry = entry

    /**
     * 执行类型特定的清理逻辑（删除前/归档时）
     */
    fun cleanup(entry: VaultEntry): VaultEntry = entry
}

/**
 * 条目验证策略
 * 统一的验证接口，可根据条目类型委托给具体策略
 */
interface ValidationStrategy {
    fun validate(entry: VaultEntry): ValidationResult
}

/**
 * 验证结果
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<ValidationError> = emptyList()
)

data class ValidationError(
    val field: String,
    val message: String,
    val severity: ErrorSeverity = ErrorSeverity.ERROR
)

enum class ErrorSeverity {
    WARNING,  // 警告
    ERROR     // 错误
}

/**
 * 条目显示策略
 * 控制条目的 UI 表示形式
 */
interface DisplayStrategy {
    /**
     * 获取在列表中显示的标签
     */
    fun getDisplayLabel(entry: VaultEntry): String

    /**
     * 获取在详情页显示的字段组
     */
    fun getDetailFieldGroups(entry: VaultEntry): List<FieldGroup>

    /**
     * 获取在编辑页显示的字段组
     */
    fun getEditFieldGroups(entry: VaultEntry): List<FieldGroup>
}

/**
 * 字段组（用于组织编辑/显示表单）
 */
data class FieldGroup(
    val title: String,
    val fields: List<FieldDefinition>
)

/**
 * 字段定义
 */
data class FieldDefinition(
    val key: String,
    val label: String,
    val isSensitive: Boolean = false,
    val isRequired: Boolean = false,
    val fieldType: FieldType = FieldType.TEXT
)

enum class FieldType {
    TEXT,
    PASSWORD,
    NUMBER,
    PHONE,
    EMAIL,
    URL,
    DATE,
    TIME,
    TOGGLE,
    SELECT,
    TEXTAREA,
    CUSTOM
}

/**
 * 策略工厂
 */
object EntryTypeStrategyFactory {
    private val strategies = mutableMapOf<EntryType, EntryTypeStrategy>()

    fun register(strategy: EntryTypeStrategy) {
        strategies[strategy.entryType] = strategy
    }

    fun getStrategy(entryType: EntryType): EntryTypeStrategy {
        return strategies[entryType]
            ?: throw IllegalArgumentException("没有找到类型 $entryType 对应的策略")
    }

    fun getStrategy(typeValue: Int): EntryTypeStrategy {
        val entryType = EntryType.fromValue(typeValue)
        return getStrategy(entryType)
    }

    fun hasStrategy(entryType: EntryType): Boolean = entryType in strategies

    fun getAllStrategies(): List<EntryTypeStrategy> = strategies.values.toList()
}
