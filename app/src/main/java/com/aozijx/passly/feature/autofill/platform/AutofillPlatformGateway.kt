package com.aozijx.passly.feature.autofill.platform

import kotlinx.coroutines.flow.Flow

interface AutofillPlatformGateway {
    fun observeServiceEnabled(): Flow<Boolean>

    fun openSystemSettings()
}
