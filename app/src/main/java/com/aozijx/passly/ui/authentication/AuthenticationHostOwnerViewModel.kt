package com.aozijx.passly.ui.authentication

import androidx.lifecycle.ViewModel
import java.util.UUID

class AuthenticationHostOwnerViewModel : ViewModel() {
    val ownerId: String = UUID.randomUUID().toString()
}
