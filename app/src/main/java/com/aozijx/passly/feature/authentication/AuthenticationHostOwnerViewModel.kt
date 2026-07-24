package com.aozijx.passly.feature.authentication

import androidx.lifecycle.ViewModel
import com.github.f4b6a3.uuid.UuidCreator

class AuthenticationHostOwnerViewModel : ViewModel() {
    val ownerId: String = UuidCreator.getTimeOrderedEpoch().toString()
}
