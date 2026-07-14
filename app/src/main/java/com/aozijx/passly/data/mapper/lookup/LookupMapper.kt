package com.aozijx.passly.data.mapper.lookup

import com.aozijx.passly.data.model.entity.LookupField
import com.aozijx.passly.data.model.payload.lookup.LookupPayload
import com.aozijx.passly.domain.model.LookupFieldValue
import com.aozijx.passly.domain.model.VaultEntry

fun VaultEntry.toLookupPayload(): LookupPayload = LookupPayload(
    entryId = id,
    searchText = buildSearchText()
)

private fun VaultEntry.buildSearchText(): String = buildString {
    title?.let { append(it); append("\n") }
    username?.let { append(it); append("\n") }
    email?.let { append(it); append("\n") }
    uriList?.forEach { url -> append(url); append("\n") }
    associatedDomain?.let { append(it); append("\n") }
    associatedAppPackage?.let { append(it) }
}.trim()

fun VaultEntry.toLookupFields(): List<LookupFieldValue> = buildList {
    title?.takeIf { it.isNotBlank() }?.let {
        add(LookupFieldValue(LookupField.TITLE, it))
    }
    username?.takeIf { it.isNotBlank() }?.let {
        add(LookupFieldValue(LookupField.USERNAME, it))
    }
    email?.takeIf { it.isNotBlank() }?.let {
        add(LookupFieldValue(LookupField.EMAIL, it))
    }
    associatedDomain?.takeIf { it.isNotBlank() }?.let {
        add(LookupFieldValue(LookupField.DOMAIN, it))
    }
    uriList?.forEach { url ->
        url.takeIf { it.isNotBlank() }?.let {
            add(LookupFieldValue(LookupField.URL, it))
        }
    }
    associatedAppPackage?.takeIf { it.isNotBlank() }?.let {
        add(LookupFieldValue(LookupField.PACKAGE, it))
    }
}

fun LookupPayload.toLookupFields(): List<LookupFieldValue> = buildList {
    val text = searchText.trim()
    if (text.isNotBlank()) {
        add(LookupFieldValue(LookupField.SEARCH_TEXT, text))
    }
}