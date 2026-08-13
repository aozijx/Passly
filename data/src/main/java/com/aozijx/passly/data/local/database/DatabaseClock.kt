package com.aozijx.passly.data.local.database

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DatabaseClock @Inject constructor() {
    fun now(): Long = System.currentTimeMillis()
}
