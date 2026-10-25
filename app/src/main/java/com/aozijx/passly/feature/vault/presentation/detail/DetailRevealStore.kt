package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.sensitive.SensitiveValue

/** Owns the wipe lifecycle of plaintext values revealed by the detail screen. */
internal class DetailRevealStore {
    private val values = mutableMapOf<FieldKey, SensitiveValue>()

    fun replace(key: FieldKey, value: SensitiveValue?) {
        val previous = if (value == null) values.remove(key) else values.put(key, value)
        if (previous !== value) previous?.wipe()
    }

    fun clear() {
        values.values.forEach(SensitiveValue::wipe)
        values.clear()
    }
}
