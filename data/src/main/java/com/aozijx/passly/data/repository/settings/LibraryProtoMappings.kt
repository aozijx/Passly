package com.aozijx.passly.data.repository.settings

import com.aozijx.passly.data.local.datastore.settings.CardDensity as ProtoCardDensity
import com.aozijx.passly.data.local.datastore.settings.EntryCardPresentation as ProtoEntryCardPresentation
import com.aozijx.passly.data.local.datastore.settings.VaultSortPreference
import com.aozijx.passly.data.local.datastore.settings.VaultViewPreferences
import com.aozijx.passly.domain.settings.model.CardDensity
import com.aozijx.passly.domain.settings.model.EntryCardPresentation
import com.aozijx.passly.domain.settings.model.EntryHierarchyDisplayMode
import com.aozijx.passly.domain.entry.model.query.EntrySort
import com.aozijx.passly.domain.entry.model.query.EntrySortField
import com.aozijx.passly.domain.entry.model.query.SortDirection
import com.aozijx.passly.domain.settings.model.LibraryViewSettings
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

// -- VaultSortPreference ↔ EntrySort --
internal fun VaultSortPreference.toDomain(): EntrySort {
    val sortField = EntrySortField.entries.find { it.name == field }
        ?: EntrySortField.LAST_USED_AT
    val direction = if (descending) SortDirection.DESC else SortDirection.ASC
    val tieBreaker = EntrySortField.entries.find { it.name == tieBreakerField }
        ?: EntrySortField.ID
    return EntrySort(sortField, direction, pinFavorites, tieBreaker)
}

internal fun EntrySort.toProtoSort(): VaultSortPreference =
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
        sort = if (p.hasSort()) p.sort.toDomain() else EntrySort.DEFAULT,
        entryCardPresentations = p.entryCardPresentationsList.map { it.toDomain() },
        entryHierarchyDisplayMode =
            EntryHierarchyDisplayMode.fromKey(p.entryHierarchyDisplayMode)
    )
