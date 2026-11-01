package com.aozijx.passly.security.authentication.host

import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SecretDialogSubmissionTest {
    @Test
    fun lateSecretInputIsClearedWhenThereIsNoSecretRequest() {
        val chars = charArrayOf('s', 'e', 'c', 'r', 'e', 't')

        submitSecretInput(
            request = null,
            expectedIdentity = AuthUiRequestId(),
            chars = chars,
        )

        assertArrayEquals(CharArray(6), chars)
    }

    @Test
    fun secretInputIsClearedWhenDialogHasChooseMethodRequest() = runBlocking {
        val chars = charArrayOf('s', 'e', 'c', 'r', 'e', 't')
        val identity = AuthUiRequestId()
        val pendingRequest = launch(start = CoroutineStart.UNDISPATCHED) {
            suspendCancellableCoroutine<AuthenticationMethod?> { continuation ->
                submitSecretInput(
                    request = AuthenticationDialogRequest.ChooseMethod(
                        purpose = AuthenticationPurpose.UNLOCK_VAULT,
                        methods = listOf(AuthenticationMethod.APP_PASSWORD),
                        continuation = continuation,
                        identity = identity,
                    ),
                    expectedIdentity = identity,
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

        submitSecretInput(request, request.identity, chars)

        assertArrayEquals(CharArray(6), chars)
    }

    @Test
    fun activeSecretRequestTakesAndClearsInput() = runBlocking {
        val chars = charArrayOf('s', 'e', 'c', 'r', 'e', 't')
        val result = suspendCancellableCoroutine<SecretHostResult> { continuation ->
            val request = AuthenticationDialogRequest.Secret(
                purpose = AuthenticationPurpose.UNLOCK_VAULT,
                method = AuthenticationMethod.APP_PASSWORD,
                continuation = continuation,
            )
            submitSecretInput(request, request.identity, chars)
        }
        val secret = (result as SecretHostResult.Submitted).secret

        try {
            assertArrayEquals(CharArray(6), chars)
            assertArrayEquals(charArrayOf('s', 'e', 'c', 'r', 'e', 't'), secret.copyForWorker())
        } finally {
            secret.close()
        }
    }

    @Test
    fun staleSecretCallbackCannotResumeRequestWithAnotherIdentity() = runBlocking<Unit> {
        val oldIdentity = AuthUiRequestId()
        val currentIdentity = AuthUiRequestId()
        val chars = charArrayOf('s', 'e', 'c', 'r', 'e', 't')
        val oldRequest = launch(start = CoroutineStart.UNDISPATCHED) {
            suspendCancellableCoroutine<SecretHostResult> { continuation ->
                submitSecretInput(
                    request = AuthenticationDialogRequest.Secret(
                        purpose = AuthenticationPurpose.UNLOCK_VAULT,
                        method = AuthenticationMethod.APP_PASSWORD,
                        continuation = continuation,
                        identity = oldIdentity,
                    ),
                    expectedIdentity = currentIdentity,
                    chars = chars,
                )
            }
        }

        assertArrayEquals(CharArray(6), chars)
        assertFalse(oldRequest.isCompleted)
        oldRequest.cancelAndJoin()
    }
}
