package com.aozijx.passly.data.util

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Clock @Inject constructor() {
    fun now(): Long = System.currentTimeMillis()
}