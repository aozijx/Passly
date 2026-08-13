package com.aozijx.passly.core.message.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import com.aozijx.passly.app.message.contract.AppNoticePublisher

val LocalAppNoticePublisher = compositionLocalOf<AppNoticePublisher> {
    error("AppNoticePublisher must be provided at the application UI root")
}

@Composable
fun ProvideAppNoticePublisher(
    publisher: AppNoticePublisher,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalAppNoticePublisher provides publisher, content = content)
}
