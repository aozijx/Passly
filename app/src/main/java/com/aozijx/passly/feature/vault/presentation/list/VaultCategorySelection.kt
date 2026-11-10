package com.aozijx.passly.feature.vault.presentation.list

/** The page's category-filter choice. The Domain query converts [All] to its optional filter. */
sealed interface VaultCategorySelection {
    data object All : VaultCategorySelection

    data class Selected(
        val name: String,
    ) : VaultCategorySelection {
        init {
            require(name.isNotBlank()) { "Selected category must have a name" }
        }
    }
}
