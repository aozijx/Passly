package com.aozijx.passly.app.autofill

import kotlinx.coroutines.flow.Flow

internal fun interface AutofillServiceStatusSource {
    fun observeServiceEnabled(): Flow<Boolean>
}
