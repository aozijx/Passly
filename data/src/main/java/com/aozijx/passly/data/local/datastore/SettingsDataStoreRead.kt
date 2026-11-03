package com.aozijx.passly.data.local.datastore

import androidx.datastore.core.CorruptionException
import com.aozijx.passly.domain.settings.port.CorruptSettingsDataException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

internal fun <T> Flow<T>.tagCorruptSettingsData(): Flow<T> =
    catch { error ->
        if (error is CorruptionException) throw CorruptSettingsDataException(error)
        throw error
    }
