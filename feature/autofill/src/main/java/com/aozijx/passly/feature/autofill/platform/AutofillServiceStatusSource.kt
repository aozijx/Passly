package com.aozijx.passly.feature.autofill.platform

import kotlinx.coroutines.flow.Flow

fun interface AutofillServiceStatusSource {
    fun observeServiceEnabled(): Flow<Boolean>
}
