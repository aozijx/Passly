package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey

/** Authorization field-set policy for an exact high-sensitivity snapshot restore. */
object SensitiveRevisionRestorePolicy {
    fun affectedFields(
        currentFields: Set<SensitiveFieldKey>,
        historicalFields: Set<SensitiveFieldKey>,
    ): Set<SensitiveFieldKey> =
        buildSet {
            addAll(currentFields)
            addAll(historicalFields)
        }

}
