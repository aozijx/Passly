package com.aozijx.passly.di.entry

import com.aozijx.passly.domain.entry.model.EntryType
import dagger.MapKey

@MapKey
annotation class EntryTypeKey(val value: EntryType)
