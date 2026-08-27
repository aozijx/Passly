package com.aozijx.passly.presentation.feature.shell.navigation

import com.aozijx.passly.presentation.feature.shell.AppShellAuthResult

internal class ShellAuthContinuation {
    private var pending: (() -> Unit)? = null

    fun replace(onSuccess: () -> Unit) {
        pending = onSuccess
    }

    fun onResult(result: AppShellAuthResult) {
        val continuation = pending
        pending = null
        if (result == AppShellAuthResult.Success) continuation?.invoke()
    }
}
