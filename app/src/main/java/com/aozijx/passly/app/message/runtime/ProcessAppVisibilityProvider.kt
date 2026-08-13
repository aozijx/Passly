package com.aozijx.passly.app.message.runtime

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.aozijx.passly.app.message.contract.AppVisibility
import com.aozijx.passly.app.message.contract.AppVisibilityProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProcessAppVisibilityProvider @Inject constructor() :
    AppVisibilityProvider,
    DefaultLifecycleObserver {
    @Volatile
    private var visibility = AppVisibility.BACKGROUND

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun current(): AppVisibility = visibility

    override fun onStart(owner: LifecycleOwner) {
        visibility = AppVisibility.FOREGROUND
    }

    override fun onStop(owner: LifecycleOwner) {
        visibility = AppVisibility.BACKGROUND
    }
}
