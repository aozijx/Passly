package com.aozijx.passly.domain.access.model

/** Public configuration of the committed binding, without aliases or key material. */
sealed interface BiometricConfiguration {
    data object NotConfigured : BiometricConfiguration
    data class Configured(val invalidateOnEnrollment: Boolean) : BiometricConfiguration

    /** The first key uses enrollment invalidation; replacements retain the committed policy. */
    val invalidateOnEnrollmentForNextKey: Boolean
        get() = when (this) {
            NotConfigured -> true
            is Configured -> invalidateOnEnrollment
        }
}
