package com.aozijx.passly.core.message

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class AppMessageCategory { GENERAL, ICON_DOWNLOAD, CLIPBOARD_CLEAR, APP_CLOSE }

enum class AppMessagePresentation { TOAST, STATUS_BAR }

data class AppMessage(
    val text: String,
    val category: AppMessageCategory = AppMessageCategory.GENERAL,
    val longDuration: Boolean = false,
    val presentation: AppMessagePresentation = AppMessagePresentation.TOAST,
    val title: String? = null
)

object AppMessageCenter {
    private const val DEDUPLICATION_WINDOW_MS = 1_000L
    private val events = MutableSharedFlow<AppMessage>(extraBufferCapacity = 32)
    val messages: SharedFlow<AppMessage> = events.asSharedFlow()

    private val lock = Any()
    private var lastKey: String? = null
    private var lastPublishedAt = 0L

    fun publish(message: AppMessage) {
        val now = System.nanoTime() / 1_000_000L
        val key = "${message.presentation}:${message.category}:${message.text}"
        synchronized(lock) {
            if (key == lastKey && now - lastPublishedAt < DEDUPLICATION_WINDOW_MS) return
            lastKey = key
            lastPublishedAt = now
        }
        events.tryEmit(message)
    }

    fun publish(
        text: String,
        category: AppMessageCategory = AppMessageCategory.GENERAL,
        longDuration: Boolean = false,
        presentation: AppMessagePresentation = AppMessagePresentation.TOAST,
        title: String? = null
    ) = publish(AppMessage(text, category, longDuration, presentation, title))
}
