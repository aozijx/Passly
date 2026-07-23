package com.aozijx.passly.di.entry

import com.aozijx.passly.domain.model.entry.EntryType
import dagger.MapKey

@MapKey
annotation class EntryTypeKey(val value: EntryType)
