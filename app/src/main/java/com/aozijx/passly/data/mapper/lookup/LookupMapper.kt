package com.aozijx.passly.data.mapper.lookup

import com.aozijx.passly.data.model.payload.lookup.LookupPayload
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.model.lookup.LookupField
import com.aozijx.passly.domain.model.lookup.LookupFieldValue

fun VaultEntry.toLookupPayload(): LookupPayload = LookupPayload(
    entryId = id,
    searchText = buildSearchText()
)

private fun VaultEntry.buildSearchText(): String = buildString {
    title.takeIf { it.isNotBlank() }?.let { append(it); append("\n") }
    username.takeIf { it.isNotBlank() }?.let { append(it); append("\n") }
    credential.email?.takeIf { it.isNotBlank() }?.let { append(it); append("\n") }
    website?.matchDomains?.forEach { domain ->
        domain.takeIf { it.isNotBlank() }?.let { append(it); append("\n") }
    }
    website?.primaryUrl?.takeIf { it.isNotBlank() }?.let { append(it); append("\n") }
    website?.packageNames?.firstOrNull()?.takeIf { it.isNotBlank() }?.let { append(it) }
}.trim()

fun VaultEntry.toLookupFields(): List<LookupFieldValue> = buildList {
    title.takeIf { it.isNotBlank() }?.let {
        add(LookupFieldValue(LookupField.TITLE, it))
    }
    username.takeIf { it.isNotBlank() }?.let {
        add(LookupFieldValue(LookupField.USERNAME, it))
    }
    credential.email?.takeIf { it.isNotBlank() }?.let {
        add(LookupFieldValue(LookupField.EMAIL, it))
    }
    website?.matchDomains?.firstOrNull()
        ?.takeIf { it.isNotBlank() }
        ?.let { add(LookupFieldValue(LookupField.DOMAIN, it)) }
    website?.matchDomains?.forEach { url ->
        url.takeIf { it.isNotBlank() }?.let {
            add(LookupFieldValue(LookupField.URL, it))
        }
    }
    website?.packageNames?.firstOrNull()?.takeIf { it.isNotBlank() }?.let {
        add(LookupFieldValue(LookupField.PACKAGE, it))
    }
}

fun LookupPayload.toLookupFields(): List<LookupFieldValue> = buildList {
    val text = searchText.trim()
    if (text.isNotBlank()) {
        add(LookupFieldValue(LookupField.TITLE, text))
    }
}