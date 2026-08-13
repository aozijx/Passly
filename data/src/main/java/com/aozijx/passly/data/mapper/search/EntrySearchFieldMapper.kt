package com.aozijx.passly.data.mapper.search

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.query.LookupField
import com.aozijx.passly.domain.entry.model.query.LookupFieldValue

fun Entry.buildSearchText(): String = buildString {
    profile.title.takeIf { it.isNotBlank() }?.let { append(it); append("\n") }
    profile.username.takeIf { it.isNotBlank() }?.let { append(it); append("\n") }
    val email = secret.login?.email
    email?.takeIf { it.isNotBlank() }?.let { append(it); append("\n") }
    profile.associations.domains.forEach { domain ->
        domain.takeIf { it.isNotBlank() }?.let { append(it); append("\n") }
    }
    profile.associations.primaryUrl?.takeIf { it.isNotBlank() }?.let { append(it); append("\n") }
    profile.associations.applicationIds.forEach { packageName ->
        packageName.takeIf { it.isNotBlank() }?.let { append(it); append("\n") }
    }
}.trim()

fun Entry.toLookupFields(): List<LookupFieldValue> = buildList {
    profile.title.takeIf { it.isNotBlank() }?.let {
        add(LookupFieldValue(LookupField.TITLE, it))
    }
    profile.username.takeIf { it.isNotBlank() }?.let {
        add(LookupFieldValue(LookupField.USERNAME, it))
    }
    val email = secret.login?.email
    email?.takeIf { it.isNotBlank() }?.let {
        add(LookupFieldValue(LookupField.EMAIL, it))
    }
    profile.associations.domains.forEach { domain ->
        domain.takeIf { it.isNotBlank() }?.let {
            add(LookupFieldValue(LookupField.DOMAIN, it))
        }
    }
    profile.associations.primaryUrl?.takeIf { it.isNotBlank() }?.let {
        add(LookupFieldValue(LookupField.URL, it))
    }
    profile.associations.applicationIds.forEach { packageName ->
        packageName.takeIf { it.isNotBlank() }?.let {
            add(LookupFieldValue(LookupField.APPLICATION_ID, it))
        }
    }
}
