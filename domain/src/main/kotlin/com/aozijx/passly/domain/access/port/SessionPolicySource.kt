package com.aozijx.passly.domain.access.port

import com.aozijx.passly.domain.access.model.SessionPolicy
import kotlinx.coroutines.flow.Flow

fun interface SessionPolicySource {
    fun observe(): Flow<SessionPolicy>
}
