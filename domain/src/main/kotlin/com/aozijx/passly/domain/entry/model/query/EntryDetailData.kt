package com.aozijx.passly.domain.entry.model.query

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.relation.RelatedPasswordEntry
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey

data class EntryDetailData(
    val entry: Entry,
    val sensitiveFieldKeys: Set<SensitiveFieldKey>,
)

data class EntryDetailSnapshot(
    val detail: EntryDetailData,
    val relations: EntryRelationsSnapshot,
)

data class EntryRelationsSnapshot(
    val accountGroupEntries: List<Entry>,
    val otherRelatedEntries: List<Entry>,
    val relatedPasswords: List<RelatedPasswordEntry>,
)
