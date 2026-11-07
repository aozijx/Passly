package com.aozijx.passly.data.repository.entry

import com.aozijx.passly.domain.sensitive.SensitiveValue

internal suspend fun <K, V : SensitiveValue, R> buildSensitiveResults(
    keys: Iterable<K>,
    create: suspend (K) -> V?,
    assemble: (K, V) -> R,
): List<R> {
    val values = mutableListOf<Pair<K, V>>()
    return try {
        keys.forEach { key -> create(key)?.let { values += key to it } }
        values.map { (key, value) -> assemble(key, value) }
    } catch (failure: Throwable) {
        values.forEach { (_, value) ->
            try {
                value.wipe()
            } catch (cleanupFailure: Throwable) {
                if (cleanupFailure !== failure) failure.addSuppressed(cleanupFailure)
            }
        }
        throw failure
    }
}
