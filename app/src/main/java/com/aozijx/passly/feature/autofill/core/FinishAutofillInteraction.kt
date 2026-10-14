package com.aozijx.passly.feature.autofill.core

import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.domain.autofill.model.AutofillInteractionState
import com.aozijx.passly.domain.autofill.model.AutofillInteractionSource
import com.aozijx.passly.feature.autofill.runtime.AutofillFinishResult
import com.aozijx.passly.feature.autofill.runtime.AutofillInteractionStore
import com.aozijx.passly.feature.autofill.runtime.AutofillUnlockLease
import javax.inject.Inject

class FinishAutofillInteraction @Inject constructor(
    private val store: AutofillInteractionStore,
    private val unlockLease: AutofillUnlockLease,
) {
    suspend fun complete(
        id: AutofillInteractionId,
        beforeRelease: suspend () -> Unit = {},
    ): AutofillFinishResult = finish(
        id = id,
        state = AutofillInteractionState.Completed,
        beforeRelease = beforeRelease,
    )

    suspend fun cancel(id: AutofillInteractionId): AutofillFinishResult =
        finish(id, AutofillInteractionState.Cancelled)

    suspend fun expire(id: AutofillInteractionId): AutofillFinishResult =
        finish(id, AutofillInteractionState.Expired)

    suspend fun expireAll(source: AutofillInteractionSource) {
        store.activeIds(source).forEach { expire(it) }
    }

    private suspend fun finish(
        id: AutofillInteractionId,
        state: AutofillInteractionState,
        beforeRelease: suspend () -> Unit = {},
    ): AutofillFinishResult {
        val result = store.finish(id, state)
        if (result.transitioned) {
            try {
                beforeRelease()
            } finally {
                unlockLease.release(id)
            }
        }
        return result
    }
}
