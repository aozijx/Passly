package com.aozijx.passly.core.message

fun interface AppMessagePublisher {
    fun publish(message: AppMessage)
}

object DefaultAppMessagePublisher : AppMessagePublisher {
    override fun publish(message: AppMessage) = AppMessageCenter.publish(message)
}
