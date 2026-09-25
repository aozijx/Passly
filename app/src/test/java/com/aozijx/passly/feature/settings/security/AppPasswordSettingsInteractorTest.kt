package com.aozijx.passly.feature.settings.security

import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationFailureCode
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationMethods
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.CancellationReason
import com.aozijx.passly.domain.access.port.AuthenticationMethodAvailability
import com.aozijx.passly.domain.access.port.AuthenticationMethodProvisioner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppPasswordSettingsInteractorTest {
    @Test
    fun `availability and authorized entry use one method snapshot`() = runTest {
        val methods = MutableStateFlow(
            AuthenticationMethods(setOf(AuthenticationMethod.APP_PASSWORD)),
        )
        val interactor = interactor(
            methodsFlow = methods,
            provisioner = FakeProvisioner(
                authorizeResult = AuthenticationResult.Success(AuthenticationMethod.APP_PASSWORD),
            ),
        )

        assertTrue(interactor.isEnabled.first())
        assertEquals(
            AppPasswordManagementAccess.Authorized(alreadyEnabled = true),
            interactor.authorizeManagement(),
        )

        methods.value = AuthenticationMethods()
        assertFalse(interactor.isEnabled.first())
    }

    @Test
    fun `authorization preserves cancellation and structured failure`() = runTest {
        val failure = AuthenticationFailure(AuthenticationFailureCode.CREDENTIAL_INCORRECT)

        assertEquals(
            AppPasswordManagementAccess.Cancelled,
            interactor(
                provisioner = FakeProvisioner(
                    authorizeResult = AuthenticationResult.Cancelled(CancellationReason.USER),
                ),
            ).authorizeManagement(),
        )
        assertEquals(
            AppPasswordManagementAccess.Failed(failure),
            interactor(
                provisioner = FakeProvisioner(
                    authorizeResult = AuthenticationResult.Failure(failure),
                ),
            ).authorizeManagement(),
        )
    }

    @Test
    fun `password changes expose semantic outcomes`() = runTest {
        val failure = AuthenticationFailure(AuthenticationFailureCode.PASSWORD_POLICY_VIOLATION)
        val provisioner = FakeProvisioner(
            setResult = AuthenticationResult.Success(AuthenticationMethod.APP_PASSWORD),
            changeResult = AuthenticationResult.Failure(failure),
            disableResult = AuthenticationResult.Cancelled(CancellationReason.USER),
        )
        val interactor = interactor(provisioner = provisioner)

        assertEquals(AppPasswordChangeResult.Completed, interactor.set(charArrayOf('a')))
        assertEquals(
            AppPasswordChangeResult.Failed(failure),
            interactor.change(charArrayOf('a'), charArrayOf('b')),
        )
        assertEquals(AppPasswordChangeResult.Cancelled, interactor.disable())
    }

    private fun interactor(
        methodsFlow: MutableStateFlow<AuthenticationMethods> = MutableStateFlow(AuthenticationMethods()),
        provisioner: FakeProvisioner,
    ) = AppPasswordSettingsInteractor(
        authenticationMethodAvailability = object : AuthenticationMethodAvailability {
            override val methods = methodsFlow
        },
        authenticationMethodProvisioner = provisioner,
    )

    private class FakeProvisioner(
        private val authorizeResult: AuthenticationResult =
            AuthenticationResult.Cancelled(CancellationReason.USER),
        private val setResult: AuthenticationResult = authorizeResult,
        private val changeResult: AuthenticationResult = authorizeResult,
        private val disableResult: AuthenticationResult = authorizeResult,
    ) : AuthenticationMethodProvisioner {
        override suspend fun authorizeAppPasswordManagement() = authorizeResult
        override suspend fun setAppPassword(password: CharArray) = setResult
        override suspend fun changeAppPassword(
            currentPassword: CharArray,
            newPassword: CharArray,
        ) = changeResult
        override suspend fun disableAppPassword() = disableResult
        override suspend fun disableBiometric() = error("Not used")
        override suspend fun rotateBiometricPolicy(invalidateOnEnrollment: Boolean) =
            error("Not used")
        override suspend fun hasRecoveryCode() = false
        override suspend fun checkRecoveryCode(code: CharArray) = false
    }
}
