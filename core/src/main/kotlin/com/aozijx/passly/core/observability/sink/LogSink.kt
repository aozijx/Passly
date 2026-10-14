package com.aozijx.passly.core.observability.sink

import com.aozijx.passly.core.observability.SanitizedLogRecord

fun interface LogSink {
    fun emit(record: SanitizedLogRecord)
}
