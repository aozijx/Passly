package com.aozijx.passly.feature.vault.presentation.editor

/** Parses the comma-separated tag input used by entry creation forms. */
internal fun parseRawTags(rawTags: String): List<String> =
    rawTags.split(',').map(String::trim).filter(String::isNotEmpty)
