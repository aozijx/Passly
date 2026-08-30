package com.aozijx.passly.domain.access.port

import kotlinx.coroutines.flow.StateFlow

interface SensitiveKeyFreshnessState {
    val generation: StateFlow<Long>
}
