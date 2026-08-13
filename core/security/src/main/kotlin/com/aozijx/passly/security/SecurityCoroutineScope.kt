package com.aozijx.passly.security

import javax.inject.Qualifier

/** Long-lived coroutine scope reserved for security state and key-expiration work. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SecurityCoroutineScope
