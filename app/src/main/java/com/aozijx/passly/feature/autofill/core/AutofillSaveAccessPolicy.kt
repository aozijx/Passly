package com.aozijx.passly.feature.autofill.core

import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import javax.inject.Inject

/** Decides whether an Autofill save may write without an authentication continuation. */
class AutofillSaveAccessPolicy @Inject constructor(
    private val secureSessionAccessState: SecureSessionAccessState,
) {
    fun canPersistImmediately(): Boolean =
        secureSessionAccessState.hasFullSecureSessionAccess()
}
