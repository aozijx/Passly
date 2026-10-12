package com.aozijx.passly.feature.autofill.core

import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.domain.autofill.model.AutofillInteractionState
import com.aozijx.passly.domain.autofill.model.AutofillInteractionSource
import com.aozijx.passly.domain.autofill.model.AutofillInteractionUpdate
import com.aozijx.passly.feature.autofill.runtime.AutofillInteractionStore
import com.aozijx.passly.feature.autofill.runtime.AutofillUnlockLease
import javax.inject.Inject

class FinishAutofillInteraction @Inject constructor(
    private val store: AutofillInteractionStore,
    private val unlockLease: AutofillUnlockLease,
) {
    suspend fun complete(id: AutofillInteractionId): AutofillInteractionUpdate =
        finish(id, AutofillInteractionState.Completed)

    suspend fun cancel(id: AutofillInteractionId): AutofillInteractionUpdate =
        finish(id, AutofillInteractionState.Cancelled)

    suspend fun expire(id: AutofillInteractionId): AutofillInteractionUpdate =
        finish(id, AutofillInteractionState.Expired)

    suspend fun expireAll(source: AutofillInteractionSource) {
        store.activeIds(source).forEach { expire(it) }
    }

    private suspend fun finish(
        id: AutofillInteractionId,
        state: AutofillInteractionState,
    ): AutofillInteractionUpdate {
        val result = store.finish(id, state)
        if (result.transitioned) unlockLease.release(id)
        return result.update
    }
}
