package com.aozijx.passly.feature.autofill.legacy.service.builder

sealed interface AutofillRemoteItem {
    val title: String

    data class SingleLine(
        override val title: String,
    ) : AutofillRemoteItem

    data class DoubleLine(
        override val title: String,
        val subtitle: String,
    ) : AutofillRemoteItem
}
