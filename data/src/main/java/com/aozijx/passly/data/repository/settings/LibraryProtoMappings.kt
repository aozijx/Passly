package com.aozijx.passly.data.repository.settings

import com.aozijx.passly.data.preferences.proto.EntryHierarchyDisplayModePreference
import com.aozijx.passly.data.preferences.proto.SortDirectionPreference
import com.aozijx.passly.data.preferences.proto.VaultSortFieldPreference
import com.aozijx.passly.data.preferences.proto.VaultSortPreference
import com.aozijx.passly.data.preferences.proto.VaultViewPreferences
import com.aozijx.passly.domain.entry.model.query.EntryHierarchyDisplayMode
import com.aozijx.passly.domain.entry.model.query.EntrySort
import com.aozijx.passly.domain.entry.model.query.EntrySortField
import com.aozijx.passly.domain.entry.model.query.SortDirection
import com.aozijx.passly.domain.settings.model.LibraryViewSettings

internal fun VaultSortFieldPreference.toDomain(): EntrySortField = when (this) {
    VaultSortFieldPreference.VAULT_SORT_FIELD_TITLE -> EntrySortField.TITLE
    VaultSortFieldPreference.VAULT_SORT_FIELD_CREATED_AT -> EntrySortField.CREATED_AT
    VaultSortFieldPreference.VAULT_SORT_FIELD_UPDATED_AT -> EntrySortField.UPDATED_AT
    VaultSortFieldPreference.VAULT_SORT_FIELD_LAST_USED_AT -> EntrySortField.LAST_USED_AT
    VaultSortFieldPreference.VAULT_SORT_FIELD_USAGE_FREQUENCY -> EntrySortField.USAGE_FREQUENCY
    VaultSortFieldPreference.VAULT_SORT_FIELD_ENTRY_TYPE -> EntrySortField.ENTRY_TYPE
    VaultSortFieldPreference.VAULT_SORT_FIELD_ID -> EntrySortField.ID
    VaultSortFieldPreference.VAULT_SORT_FIELD_UNSPECIFIED ->
        throw IllegalArgumentException("Unsupported vault sort field: $this")
}

internal fun EntrySortField.toProto(): VaultSortFieldPreference = when (this) {
    EntrySortField.TITLE -> VaultSortFieldPreference.VAULT_SORT_FIELD_TITLE
    EntrySortField.CREATED_AT -> VaultSortFieldPreference.VAULT_SORT_FIELD_CREATED_AT
    EntrySortField.UPDATED_AT -> VaultSortFieldPreference.VAULT_SORT_FIELD_UPDATED_AT
    EntrySortField.LAST_USED_AT -> VaultSortFieldPreference.VAULT_SORT_FIELD_LAST_USED_AT
    EntrySortField.USAGE_FREQUENCY -> VaultSortFieldPreference.VAULT_SORT_FIELD_USAGE_FREQUENCY
    EntrySortField.ENTRY_TYPE -> VaultSortFieldPreference.VAULT_SORT_FIELD_ENTRY_TYPE
    EntrySortField.ID -> VaultSortFieldPreference.VAULT_SORT_FIELD_ID
}

internal fun SortDirectionPreference.toDomain(): SortDirection = when (this) {
    SortDirectionPreference.SORT_DIRECTION_ASCENDING -> SortDirection.ASC
    SortDirectionPreference.SORT_DIRECTION_DESCENDING -> SortDirection.DESC
    SortDirectionPreference.SORT_DIRECTION_UNSPECIFIED ->
        throw IllegalArgumentException("Unsupported sort direction: $this")
}

internal fun SortDirection.toProto(): SortDirectionPreference = when (this) {
    SortDirection.ASC -> SortDirectionPreference.SORT_DIRECTION_ASCENDING
    SortDirection.DESC -> SortDirectionPreference.SORT_DIRECTION_DESCENDING
}

internal fun VaultSortPreference.toDomain(): EntrySort = EntrySort(
    field = field.toDomain(),
    direction = direction.toDomain(),
    pinFavorites = pinFavorites,
    tieBreaker = EntrySortField.ID,
)

internal fun EntrySort.toProtoSort(): VaultSortPreference =
    VaultSortPreference.newBuilder()
        .setField(field.toProto())
        .setDirection(direction.toProto())
        .setPinFavorites(pinFavorites)
        .build()

internal fun EntryHierarchyDisplayModePreference.toDomain(): EntryHierarchyDisplayMode = when (this) {
    EntryHierarchyDisplayModePreference.ENTRY_HIERARCHY_DISPLAY_MODE_COLLAPSED ->
        EntryHierarchyDisplayMode.COLLAPSED
    EntryHierarchyDisplayModePreference.ENTRY_HIERARCHY_DISPLAY_MODE_EXPANDED ->
        EntryHierarchyDisplayMode.EXPANDED
    EntryHierarchyDisplayModePreference.ENTRY_HIERARCHY_DISPLAY_MODE_SEPARATE ->
        EntryHierarchyDisplayMode.SEPARATE
    EntryHierarchyDisplayModePreference.ENTRY_HIERARCHY_DISPLAY_MODE_UNSPECIFIED ->
        throw IllegalArgumentException("Unsupported hierarchy display mode: $this")
}

internal fun EntryHierarchyDisplayMode.toProto(): EntryHierarchyDisplayModePreference = when (this) {
    EntryHierarchyDisplayMode.COLLAPSED ->
        EntryHierarchyDisplayModePreference.ENTRY_HIERARCHY_DISPLAY_MODE_COLLAPSED
    EntryHierarchyDisplayMode.EXPANDED ->
        EntryHierarchyDisplayModePreference.ENTRY_HIERARCHY_DISPLAY_MODE_EXPANDED
    EntryHierarchyDisplayMode.SEPARATE ->
        EntryHierarchyDisplayModePreference.ENTRY_HIERARCHY_DISPLAY_MODE_SEPARATE
}

internal fun readVault(p: VaultViewPreferences): LibraryViewSettings =
    LibraryViewSettings(
        sort = p.sort.toDomain(),
        entryHierarchyDisplayMode = p.hierarchyDisplayMode.toDomain(),
    )
