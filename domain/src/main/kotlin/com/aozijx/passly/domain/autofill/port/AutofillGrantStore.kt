package com.aozijx.passly.domain.autofill.port

import com.aozijx.passly.domain.autofill.model.AutofillGrantContext

/** Stores the short-lived authorization attached to an autofill interaction. */
interface AutofillGrantStore {
    fun grant(context: AutofillGrantContext)
    fun isGranted(context: AutofillGrantContext): Boolean
    fun clear()
}
