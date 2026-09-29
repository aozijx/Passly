package com.aozijx.passly.presentation.feature.vault.detail.history

import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.sensitive.SensitiveValue

/** Owns historical plaintext independently from saveable detail state. */
internal class DetailRevisionRevealStore {
    private val values = mutableMapOf<SensitiveFieldKey, SensitiveValue>()

    fun replace(key: SensitiveFieldKey, value: SensitiveValue?) {
        val previous = if (value == null) values.remove(key) else values.put(key, value)
        if (previous !== value) previous?.wipe()
    }

    fun snapshot(): Map<SensitiveFieldKey, SensitiveValue> = values.toMap()

    fun clear() {
        values.values.forEach(SensitiveValue::wipe)
        values.clear()
    }
}
