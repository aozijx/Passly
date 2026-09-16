package com.aozijx.passly.security.authentication

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BiometricProvisioningBoundaryTest {
    @Test
    fun `biometric policy rotation uses only the keystore biometric prompt`() {
        val provisionerBody = source("DefaultAuthenticationMethodProvisioner.kt")
            .substringAfter("override suspend fun rotateBiometricPolicy")
            .substringBefore("override suspend fun hasRecoveryCode")
        val rotationBody = source("RotateBiometricKeyUseCase.kt")
            .substringAfter("suspend fun rotate(")
            .substringBefore("private suspend fun rollbackCandidate")

        assertFalse(
            "Biometric rotation must not add a credential prompt before its CryptoObject prompt",
            provisionerBody.contains("authenticationManager.authenticate"),
        )
        assertTrue(provisionerBody.contains("rotateBiometricKey.rotate"))
        assertTrue(rotationBody.contains("host.authenticateBiometric"))
        assertTrue(rotationBody.contains("BiometricPrompt.CryptoObject(cipher)"))
    }

    @Test
    fun `app password management uses its exact authentication purpose`() {
        val authorizationBody = source("DefaultAuthenticationMethodProvisioner.kt")
            .substringAfter("override suspend fun authorizeAppPasswordManagement")
            .substringBefore("override suspend fun setAppPassword")

        assertTrue(authorizationBody.contains("AuthenticationPurpose.MANAGE_APP_PASSWORD"))
        assertFalse(authorizationBody.contains("AuthenticationPurpose.REAUTHENTICATE"))
    }
    private fun source(name: String): String {
        val sourceRoot = listOf(
            File("src/main/java"),
            File("app/src/main/java"),
        ).firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
        return File(
            sourceRoot,
            "com/aozijx/passly/security/authentication/$name",
        ).readText()
    }
}
