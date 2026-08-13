package com.aozijx.passly.domain.entry.model.activity

enum class ActivityType(
    val isUsage: Boolean = false,
    val isVersionChange: Boolean = false
) {
    VIEW(isUsage = true),
    COPY_USERNAME(isUsage = true),
    COPY_PASSWORD(isUsage = true),
    AUTOFILL(isUsage = true),
    EXPORT(isUsage = true),
    IMPORT(isUsage = true),
    CREATE(isVersionChange = true),
    UPDATE(isVersionChange = true),
    SENSITIVE_CHANGE(isVersionChange = true),
    DELETE(isVersionChange = true),
    RESTORE(isVersionChange = true);

    companion object {
        @JvmStatic
        val USAGE_TYPES: List<ActivityType> = entries.filter { it.isUsage }

        @JvmStatic
        val VERSION_TYPES: List<ActivityType> = entries.filter { it.isVersionChange }
    }
}
