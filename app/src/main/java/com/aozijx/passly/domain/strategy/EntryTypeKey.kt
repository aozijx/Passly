package com.aozijx.passly.domain.strategy

import com.aozijx.passly.domain.model.entry.EntryType
import dagger.MapKey

@MapKey
annotation class EntryTypeKey(val value: EntryType)
