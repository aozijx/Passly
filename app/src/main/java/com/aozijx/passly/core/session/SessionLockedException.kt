package com.aozijx.passly.core.session

class SessionLockedException(message: String = "Vault session is locked") :
    IllegalStateException(message)
