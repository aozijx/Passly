package com.aozijx.passly.core.platform.cache

import java.util.Locale

object ByteSizeFormatter {
    fun format(bytes: Long): String = when {
        bytes < KIB -> "$bytes B"
        bytes < MIB -> decimal(bytes / KIB.toDouble(), "KB")
        bytes < GIB -> decimal(bytes / MIB.toDouble(), "MB")
        else -> decimal(bytes / GIB.toDouble(), "GB")
    }

    private fun decimal(value: Double, unit: String): String =
        String.format(Locale.ROOT, "%.1f %s", value, unit)

    private const val KIB = 1024L
    private const val MIB = KIB * 1024
    private const val GIB = MIB * 1024
}
