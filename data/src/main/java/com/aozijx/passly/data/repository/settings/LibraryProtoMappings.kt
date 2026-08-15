package com.aozijx.passly.data.repository.settings

import com.aozijx.passly.data.local.datastore.settings.CardDensity as ProtoCardDensity
import com.aozijx.passly.data.local.datastore.settings.EntryCardPresentation as ProtoEntryCardPresentation
import com.aozijx.passly.data.local.datastore.settings.VaultSortPreference
import com.aozijx.passly.data.local.datastore.settings.VaultViewPreferences
import com.aozijx.passly.domain.settings.model.CardDensity
import com.aozijx.passly.domain.settings.model.EntryCardPresentation
import com.aozijx.passly.domain.settings.model.EntryHierarchyDisplayMode
import com.aozijx.passly.domain.settings.model.LibrarySortField
import com.aozijx.passly.domain.settings.model.LibrarySortSpec
import com.aozijx.passly.domain.settings.model.LibraryViewSettings
import com.aozijx.passly.domain.settings.model.SortDirection
import com.aozijx.passly.domain.settings.model.VisibleQuickFiltersConfig

// -- CardDensity --
internal fun ProtoCardDensity.toDomain(): CardDensity = when (this) {
    ProtoCardDensity.CARD_DENSITY_COMPACT -> CardDensity.COMPACT
    ProtoCardDensity.CARD_DENSITY_STANDARD -> CardDensity.STANDARD
    ProtoCardDensity.CARD_DENSITY_COMFORTABLE -> CardDensity.COMFORTABLE
    ProtoCardDensity.CARD_DENSITY_UNSPECIFIED -> CardDensity.STANDARD
}

internal fun CardDensity.toProto(): ProtoCardDensity = when (this) {
    CardDensity.COMPACT -> ProtoCardDensity.CARD_DENSITY_COMPACT
    CardDensity.STANDARD -> ProtoCardDensity.CARD_DENSITY_STANDARD
    CardDensity.COMFORTABLE -> ProtoCardDensity.CARD_DENSITY_COMFORTABLE
}

// -- VaultSortPreference ↔ LibrarySortSpec --
internal fun VaultSortPreference.toDomain(): LibrarySortSpec {
    val sortField = LibrarySortField.entries.find { it.name == field }
        ?: LibrarySortField.LAST_USED_AT
    val direction = if (descending) SortDirection.DESC else SortDirection.ASC
    val tieBreaker = LibrarySortField.entries.find { it.name == tieBreakerField }
        ?: LibrarySortField.ID
    return LibrarySortSpec(sortField, direction, pinFavorites, tieBreaker)
}

internal fun LibrarySortSpec.toProtoSort(): VaultSortPreference =
    VaultSortPreference.newBuilder()
        .setField(field.name)
        .setDescending(direction == SortDirection.DESC)
        .setPinFavorites(pinFavorites)
        .setTieBreakerField(tieBreaker.name)
        .build()

// -- EntryCardPresentation --
internal fun ProtoEntryCardPresentation.toDomain(): EntryCardPresentation =
    EntryCardPresentation(
        entryTypeKey = entryTypeKey,
        variantKey = variantKey,
        density = density.toDomain(),
        showIcon = showIcon,
        showFavorite = showFavorite,
        showSecondaryText = showSecondaryText,
        showQuickAction = showQuickAction
    )

internal fun EntryCardPresentation.toProto(): ProtoEntryCardPresentation =
    ProtoEntryCardPresentation.newBuilder()
        .setEntryTypeKey(entryTypeKey)
        .setVariantKey(variantKey)
        .setDensity(density.toProto())
        .setShowIcon(showIcon)
        .setShowFavorite(showFavorite)
        .setShowSecondaryText(showSecondaryText)
        .setShowQuickAction(showQuickAction)
        .build()

internal fun readVault(p: VaultViewPreferences): LibraryViewSettings =
    LibraryViewSettings(
        visibleQuickFilters = if (p.hasVisibleQuickFilters()) {
            VisibleQuickFiltersConfig(
                filterKeys = p.visibleQuickFilters.filterKeysList.toSet(),
                configured = p.visibleQuickFilters.configured
            )
        } else null,
        sort = if (p.hasSort()) p.sort.toDomain() else LibrarySortSpec.DEFAULT,
        entryCardPresentations = p.entryCardPresentationsList.map { it.toDomain() },
        entryHierarchyDisplayMode =
            EntryHierarchyDisplayMode.fromKey(p.entryHierarchyDisplayMode)
    )
