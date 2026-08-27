package com.aozijx.passly.app.session

import com.aozijx.passly.runtime.session.SessionKeySource
import com.aozijx.passly.security.dek.DekManager
import javax.inject.Inject

internal class DekSessionKeySource @Inject constructor(
    private val dekManager: DekManager,
) : SessionKeySource {
    override suspend fun copyKey(): ByteArray = dekManager.withDek(ByteArray::clone)
}
