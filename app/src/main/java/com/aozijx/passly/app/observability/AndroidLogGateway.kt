package com.aozijx.passly.app.observability

internal enum class AndroidLogPriority { DEBUG, INFO, WARN, ERROR }

internal interface AndroidLogGateway {
    fun isLoggable(tag: String, priority: AndroidLogPriority): Boolean
    fun println(priority: AndroidLogPriority, tag: String, message: String)
}
