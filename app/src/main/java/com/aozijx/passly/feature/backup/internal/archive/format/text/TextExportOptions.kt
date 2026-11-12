package com.aozijx.passly.feature.backup.internal.archive.format.text

internal data class TextExportOptions(
    val includeTechnicalInfo: Boolean = false,
    val maxNotesLines: Int = 50,
)
