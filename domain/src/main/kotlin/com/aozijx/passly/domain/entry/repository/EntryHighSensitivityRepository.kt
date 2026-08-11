package com.aozijx.passly.domain.entry.repository

import com.aozijx.passly.domain.entry.model.EntryHighSensitivitySecret

interface EntryHighSensitivityRepository {
    suspend fun getHighSensitivitySecretForReveal(entryId: String): EntryHighSensitivitySecret?
}
