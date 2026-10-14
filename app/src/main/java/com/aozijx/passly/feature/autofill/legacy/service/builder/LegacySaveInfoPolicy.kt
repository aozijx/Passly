package com.aozijx.passly.feature.autofill.legacy.service.builder

import com.aozijx.passly.domain.autofill.model.FieldRole

data class LegacySaveInfoPlan(
    val requiredPasswordFieldIds: List<String>,
    val optionalUsernameFieldIds: List<String>,
    val dataTypes: Set<FieldRole>,
)

object LegacySaveInfoPolicy {
    fun plan(roleMap: Map<String, FieldRole>): LegacySaveInfoPlan? {
        val passwordIds = roleMap.entries
            .filter { it.value == FieldRole.PASSWORD }
            .map(Map.Entry<String, FieldRole>::key)
        if (passwordIds.isEmpty()) return null

        val usernameIds = roleMap.entries
            .filter { it.value == FieldRole.USERNAME }
            .map(Map.Entry<String, FieldRole>::key)
        val dataTypes = linkedSetOf(FieldRole.PASSWORD).apply {
            if (usernameIds.isNotEmpty()) add(FieldRole.USERNAME)
        }
        return LegacySaveInfoPlan(
            requiredPasswordFieldIds = passwordIds,
            optionalUsernameFieldIds = usernameIds,
            dataTypes = dataTypes,
        )
    }
}
