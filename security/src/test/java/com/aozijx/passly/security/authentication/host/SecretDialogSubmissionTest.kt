package com.aozijx.passly.security.authentication.host

import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class SecretDialogSubmissionTest {
    @Test
    fun lateSecretInputIsClearedWhenThereIsNoSecretRequest() {
        val chars = charArrayOf('s', 'e', 'c', 'r', 'e', 't')

        submitSecretInput(request = null, chars = chars)

        assertArrayEquals(CharArray(6), chars)
    }

    @Test
    fun secretInputIsClearedWhenDialogHasChooseMethodRequest() = runBlocking {
        val chars = charArrayOf('s', 'e', 'c', 'r', 'e', 't')
        val pendingRequest = launch(start = CoroutineStart.UNDISPATCHED) {
            suspendCancellableCoroutine<AuthenticationMethod?> { continuation ->
                submitSecretInput(
                    request = AuthenticationDialogRequest.ChooseMethod(
                        purpose = AuthenticationPurpose.UNLOCK_VAULT,
                        methods = listOf(AuthenticationMethod.APP_PASSWORD),
                        continuation = continuation,
                    ),
                    chars = chars,
                )
            }
        }

        assertArrayEquals(CharArray(6), chars)
        pendingRequest.cancel()
    }

    @Test
    fun inactiveSecretRequestClearsLateInput() = runBlocking {
        lateinit var request: AuthenticationDialogRequest.Secret
        val pendingRequest = launch(start = CoroutineStart.UNDISPATCHED) {
            suspendCancellableCoroutine<SecretHostResult> { continuation ->
                request = AuthenticationDialogRequest.Secret(
                    purpose = AuthenticationPurpose.UNLOCK_VAULT,
                    method = AuthenticationMethod.APP_PASSWORD,
                    continuation = continuation,
                )
            }
        }
        pendingRequest.cancel()
        val chars = charArrayOf('s', 'e', 'c', 'r', 'e', 't')

        submitSecretInput(request, chars)

        assertArrayEquals(CharArray(6), chars)
    }

    @Test
    fun activeSecretRequestTakesAndClearsInput() = runBlocking {
        val chars = charArrayOf('s', 'e', 'c', 'r', 'e', 't')
        val result = suspendCancellableCoroutine<SecretHostResult> { continuation ->
            submitSecretInput(
                request = AuthenticationDialogRequest.Secret(
                    purpose = AuthenticationPurpose.UNLOCK_VAULT,
                    method = AuthenticationMethod.APP_PASSWORD,
                    continuation = continuation,
                ),
                chars = chars,
            )
        }
        val secret = (result as SecretHostResult.Submitted).secret

        try {
            assertArrayEquals(CharArray(6), chars)
            assertArrayEquals(charArrayOf('s', 'e', 'c', 'r', 'e', 't'), secret.copyForWorker())
        } finally {
            secret.close()
        }
    }
}
