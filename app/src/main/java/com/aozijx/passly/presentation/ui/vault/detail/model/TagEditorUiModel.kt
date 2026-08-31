package com.aozijx.passly.presentation.ui.vault.detail.model

data class DetailTagEditorUiModel(
    val visible: Boolean = false,
    val initialTags: Set<String> = emptySet(),
    val draftTags: Set<String> = emptySet(),
    val availableTags: Set<String> = emptySet(),
    val input: String = "",
    val suggestions: List<String> = emptyList(),
    val validationError: TagEditorValidationErrorUiModel? = null,
    val confirmDiscard: Boolean = false,
) {
    val dirty: Boolean get() = draftTags != initialTags || input.isNotBlank()
}

enum class TagEditorValidationErrorUiModel {
    TOO_MANY_TAGS,
    TAG_TOO_LONG,
}
