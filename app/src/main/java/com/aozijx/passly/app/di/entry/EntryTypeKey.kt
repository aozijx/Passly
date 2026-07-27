package com.aozijx.passly.app.di.entry

import com.aozijx.passly.domain.entry.model.EntryType
import dagger.MapKey

@MapKey
annotation class EntryTypeKey(val value: EntryType)
