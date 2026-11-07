package com.aozijx.passly.security.authentication.flow
import com.aozijx.passly.domain.access.model.AuthenticationCredentialSnapshot
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationMethods
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthenticationAvailabilityResolverTest {
    @Test
    fun `hardware availability only gates biometric configuration`() {
        val credentials =
            AuthenticationCredentialSnapshot(
                biometricConfigured = true,
                appPasswordConfigured = true,
                recoveryCodeConfigured = true,
            )

        assertEquals(
            AuthenticationMethods(
                setOf(AuthenticationMethod.APP_PASSWORD, AuthenticationMethod.RECOVERY_CODE),
            ),
            credentials.toAuthenticationMethods(biometricAvailable = false),
        )
        assertEquals(
            AuthenticationMethods(
                setOf(
                    AuthenticationMethod.BIOMETRIC,
                    AuthenticationMethod.APP_PASSWORD,
                    AuthenticationMethod.RECOVERY_CODE,
                ),
            ),
            credentials.toAuthenticationMethods(biometricAvailable = true),
        )
    }
}
