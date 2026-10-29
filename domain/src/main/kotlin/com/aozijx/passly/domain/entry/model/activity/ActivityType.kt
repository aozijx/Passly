package com.aozijx.passly.domain.entry.model.activity

enum class ActivityType(
    val isUsage: Boolean = false,
    val isMutation: Boolean = false,
    val isContentChange: Boolean = false,
) {
    VIEW(isUsage = true),
    COPY_USERNAME(isUsage = true),
    COPY_PASSWORD(isUsage = true),
    AUTOFILL(isUsage = true),
    EXPORT(isUsage = true),
    IMPORT(isUsage = true),
    CREATE(isMutation = true, isContentChange = true),
    UPDATE(isMutation = true, isContentChange = true),
    VERSION_RESTORED(isMutation = true, isContentChange = true),
    DELETE(isMutation = true),
    RESTORE(isMutation = true);

    companion object {
        @JvmStatic
        val USAGE_TYPES: List<ActivityType> = entries.filter { it.isUsage }

        @JvmStatic
        val MUTATION_TYPES: List<ActivityType> = entries.filter { it.isMutation }
    }
}
