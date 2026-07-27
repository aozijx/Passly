package com.aozijx.passly.data.backup.format.text

data class TextExportOptions(
    val includeTechnicalInfo: Boolean = false,
    val maxNotesLines: Int = 50
)
