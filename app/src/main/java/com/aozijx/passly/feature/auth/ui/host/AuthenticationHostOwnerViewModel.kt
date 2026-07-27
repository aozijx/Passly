package com.aozijx.passly.feature.auth.ui.host

import androidx.lifecycle.ViewModel
import com.github.f4b6a3.uuid.UuidCreator

class AuthenticationHostOwnerViewModel : ViewModel() {
    val ownerId: String = UuidCreator.getTimeOrderedEpoch().toString()
}
