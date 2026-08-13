package com.aozijx.passly.data.backup.io

import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

internal fun ByteArray.decodeStrictUtf8(fieldName: String): String {
    val decoder = Charsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
    return try {
        decoder.decode(ByteBuffer.wrap(this)).toString()
    } catch (error: Exception) {
        throw IllegalArgumentException("$fieldName 不是有效的 UTF-8", error)
    }
}
