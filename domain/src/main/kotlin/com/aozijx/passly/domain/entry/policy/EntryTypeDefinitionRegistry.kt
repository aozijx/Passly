package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryTypeDefinition

internal fun buildEntryTypeDefinitionIndex(
    definitions: List<EntryTypeDefinition>,
    expectedTypes: Set<EntryType>,
): Map<EntryType, EntryTypeDefinition> {
    val duplicateTypes = definitions
        .groupingBy(EntryTypeDefinition::type)
        .eachCount()
        .filterValues { it > 1 }
        .keys
    require(duplicateTypes.isEmpty()) {
        "Entry type catalog contains duplicate registrations: ${duplicateTypes.sortedBy(EntryType::name)}"
    }

    val byType = definitions.associateBy(EntryTypeDefinition::type)
    val missingTypes = expectedTypes - byType.keys
    val unexpectedTypes = byType.keys - expectedTypes
    require(missingTypes.isEmpty() && unexpectedTypes.isEmpty()) {
        "Entry type catalog coverage mismatch; " +
            "missing=${missingTypes.sortedBy(EntryType::name)}, " +
            "unexpected=${unexpectedTypes.sortedBy(EntryType::name)}"
    }
    return byType
}
