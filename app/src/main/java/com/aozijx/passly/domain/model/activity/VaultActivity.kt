package com.aozijx.passly.domain.model.activity

import com.github.f4b6a3.uuid.UuidCreator

/**
 * 活动类型。
 *
 * 按语义分为两类：
 * - **使用记录** ([isUsage] == true)：操作型行为，如查看、复制、自动填充、导入/导出
 * - **版本更新** ([isVersionChange] == true)：数据变更，如创建、更新、删除、恢复
 */
enum class ActivityType(
    /** 是否为使用记录（非数据变更）。 */
    val isUsage: Boolean = false,
    /** 是否为版本更新（数据变更）。 */
    val isVersionChange: Boolean = false
) {
    // ---- 使用记录 ----
    VIEW(isUsage = true),
    COPY_USERNAME(isUsage = true),
    COPY_PASSWORD(isUsage = true),
    AUTOFILL(isUsage = true),
    EXPORT(isUsage = true),
    IMPORT(isUsage = true),

    // ---- 版本更新 ----
    CREATE(isVersionChange = true),
    UPDATE(isVersionChange = true),
    DELETE(isVersionChange = true),
    RESTORE(isVersionChange = true);

    companion object {
        /** 所有使用记录类型的集合。 */
        @JvmStatic
        val USAGE_TYPES: List<ActivityType> = entries.filter { it.isUsage }

        /** 所有版本更新类型的集合。 */
        @JvmStatic
        val VERSION_TYPES: List<ActivityType> = entries.filter { it.isVersionChange }
    }
}

data class VaultActivity(
    val activityId: String = UuidCreator.getTimeOrderedEpoch().toString(),
    val entryId: String,
    val activityType: ActivityType,
    val source: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
