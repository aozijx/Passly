package com.aozijx.passly.feature.backup.internal.archive.format.json

import com.aozijx.passly.feature.backup.internal.archive.BackupBundleValidator
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.StreamReadConstraints
import java.io.InputStream

/** Applies the same parser safety limits to every supported JSON backup format. */
internal object BackupJsonParserFactory {
    private val factory =
        JsonFactory
            .builder()
            .streamReadConstraints(
                StreamReadConstraints
                    .builder()
                    .maxNestingDepth(MAX_NESTING_DEPTH)
                    .maxStringLength(MAX_RESOURCE_BASE64_CHARS)
                    .build(),
            ).build()

    fun create(input: InputStream): JsonParser = factory.createParser(input)

    private const val MAX_NESTING_DEPTH = 100
    private const val MAX_RESOURCE_BASE64_CHARS = ((BackupBundleValidator.MAX_RESOURCE_BYTES + 2) / 3 * 4) + 8
}
