package com.aozijx.passly.data.mapper.entry

import com.aozijx.passly.domain.entry.model.credential.EntryCredentialKind

internal class EntryPayloadAdapterRegistry(adapters: List<EntryPayloadAdapter>) {
    private val adaptersByKind = adapters.associateBy(EntryPayloadAdapter::kind).also { indexed ->
        require(indexed.size == adapters.size) { "Duplicate entry payload adapter kind" }
    }

    fun get(kind: EntryCredentialKind): EntryPayloadAdapter =
        requireNotNull(adaptersByKind[kind]) { "No payload adapter registered for credential kind $kind" }
}

internal val entryPayloadAdapters = EntryPayloadAdapterRegistry(
    listOf(
        OtpEntryPayloadAdapter(),
        LoginEntryPayloadAdapter(),
        CardEntryPayloadAdapter(),
        WifiEntryPayloadAdapter(),
        IdentityEntryPayloadAdapter(),
    ),
)
