package com.aozijx.passly.data.mapper.search

import com.aozijx.passly.domain.entry.model.VaultEntry
import com.aozijx.passly.domain.entry.model.lookup.LookupField
import com.aozijx.passly.domain.entry.model.lookup.LookupFieldValue

fun VaultEntry.buildSearchText(): String = buildString {
    title.takeIf { it.isNotBlank() }?.let { append(it); append("\n") }
    username.takeIf { it.isNotBlank() }?.let { append(it); append("\n") }
    val email = secret.login?.email
    email?.takeIf { it.isNotBlank() }?.let { append(it); append("\n") }
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
    val email = secret.login?.email
    email?.takeIf { it.isNotBlank() }?.let {
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
