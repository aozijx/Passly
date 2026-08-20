package com.aozijx.passly.domain.autofill.port

import kotlinx.coroutines.flow.Flow

interface AutofillStatusRepository {
    fun isAutofillServiceEnabled(): Boolean
    fun observeAutofillStatus(): Flow<Boolean>
    fun isAutofillSupported(): Boolean
    fun openAutofillSettings()
}
