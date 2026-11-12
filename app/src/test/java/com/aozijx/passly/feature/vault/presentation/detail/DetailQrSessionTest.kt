package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.core.platform.qr.QrCodeMatrix
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.credential.WifiCredential
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.feature.vault.detail.EntryQrExportResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DetailQrSessionTest {
    @Test
    fun `authorization remains pending until export reports authorization`() =
        runTest {
            val started = CompletableDeferred<Unit>()
            val result = CompletableDeferred<EntryQrExportResult>()
            var onAuthorized: (() -> Unit)? = null
            val session =
                DetailQrSession(backgroundScope, Access()) { _, authorized ->
                    onAuthorized = authorized
                    started.complete(Unit)
                    result.await()
                }

            session.request(entry())
            runCurrent()

            assertTrue(started.isCompleted)
            assertEquals(DetailQrState.Authorizing(EntryQrKind.Wifi), session.state.value)
            requireNotNull(onAuthorized).invoke()
            assertEquals(DetailQrState.Loading(EntryQrKind.Wifi), session.state.value)
            result.complete(EntryQrExportResult.Cancelled)
            runCurrent()
            assertEquals(DetailQrState.Hidden, session.state.value)
        }

    @Test
    fun `cancelled export does not leave a loading sheet`() =
        runTest {
            val session =
                DetailQrSession(backgroundScope, Access()) { _, onAuthorized ->
                    onAuthorized()
                    throw CancellationException("cancelled")
                }

            session.request(entry())
            runCurrent()

            assertEquals(DetailQrState.Hidden, session.state.value)
        }

    @Test
    fun `destroying the owner scope wipes content and ignores further requests`() =
        runTest {
            val content = OwnedChars.fromString("WIFI:synthetic")
            val session =
                DetailQrSession(backgroundScope, Access(), StandardTestDispatcher(testScheduler)) { _, onAuthorized ->
                    onAuthorized()
                    EntryQrExportResult.Ready(content)
                }
            session.request(entry())
            runCurrent()
            val matrix = (session.state.value as DetailQrState.Ready).matrix

            backgroundScope.cancel()
            runCurrent()

            assertTrue(content.isEmpty)
            assertFalse(matrix.anyDarkModule())
            assertEquals(DetailQrState.Hidden, session.state.value)
            session.request(entry())
            assertEquals(DetailQrState.Hidden, session.state.value)
        }

    @Test
    fun `dismissal wipes the displayed matrix and source content`() =
        runTest {
            val content = OwnedChars.fromString("WIFI:synthetic")
            val session =
                DetailQrSession(backgroundScope, Access(), StandardTestDispatcher(testScheduler)) { _, onAuthorized ->
                    onAuthorized()
                    EntryQrExportResult.Ready(content)
                }

            session.request(entry())
            runCurrent()
            val matrix = (session.state.value as DetailQrState.Ready).matrix
            assertTrue(matrix.anyDarkModule())
            assertTrue(content.isEmpty)

            session.close()

            assertEquals(DetailQrState.Hidden, session.state.value)
            assertTrue(content.isEmpty)
            assertFalse(matrix.anyDarkModule())
        }

    @Test
    fun `locking the vault closes and wipes the code`() =
        runTest {
            val access = Access()
            val content = OwnedChars.fromString("WIFI:synthetic")
            val session =
                DetailQrSession(backgroundScope, access, StandardTestDispatcher(testScheduler)) { _, onAuthorized ->
                    onAuthorized()
                    EntryQrExportResult.Ready(content)
                }
            session.request(entry())
            runCurrent()
            val matrix = (session.state.value as DetailQrState.Ready).matrix

            access.authenticationState.value = AuthenticationState.Locked
            runCurrent()

            assertEquals(DetailQrState.Hidden, session.state.value)
            assertTrue(content.isEmpty)
            assertFalse(matrix.anyDarkModule())
        }

    @Test
    fun `late export cannot reopen a dismissed code and is wiped`() =
        runTest {
            val result = CompletableDeferred<EntryQrExportResult>()
            val content = OwnedChars.fromString("WIFI:synthetic")
            val session =
                DetailQrSession(backgroundScope, Access()) { _, onAuthorized ->
                    onAuthorized()
                    withContext(NonCancellable) { result.await() }
                }
            session.request(entry())
            runCurrent()
            assertEquals(DetailQrState.Loading(EntryQrKind.Wifi), session.state.value)

            session.close()
            result.complete(EntryQrExportResult.Ready(content))
            runCurrent()

            assertEquals(DetailQrState.Hidden, session.state.value)
            assertTrue(content.isEmpty)
        }

    @Test
    fun `request while locked never exports`() =
        runTest {
            val access = Access().apply { authenticationState.value = AuthenticationState.Locked }
            var exports = 0
            val session =
                DetailQrSession(backgroundScope, access) { _, onAuthorized ->
                    exports++
                    onAuthorized()
                    EntryQrExportResult.Unavailable
                }

            session.request(entry())
            runCurrent()

            assertEquals(0, exports)
            assertEquals(DetailQrState.Hidden, session.state.value)
        }

    @Test
    fun `replacement wipes the previous code`() =
        runTest {
            val contents = mutableListOf<OwnedChars>()
            val session =
                DetailQrSession(backgroundScope, Access(), StandardTestDispatcher(testScheduler)) { _, onAuthorized ->
                    onAuthorized()
                    val content = OwnedChars.fromString("WIFI:${contents.size}").also(contents::add)
                    EntryQrExportResult.Ready(content)
                }
            session.request(entry())
            runCurrent()
            val previousMatrix = (session.state.value as DetailQrState.Ready).matrix

            session.request(entry())
            runCurrent()
            val currentMatrix = (session.state.value as DetailQrState.Ready).matrix

            assertEquals(2, contents.size)
            assertTrue(contents.all(OwnedChars::isEmpty))
            assertFalse(previousMatrix.anyDarkModule())
            assertTrue(currentMatrix.anyDarkModule())
            session.close()
            assertFalse(currentMatrix.anyDarkModule())
        }

    @Test
    fun `empty exported content fails and is wiped`() =
        runTest {
            val content = OwnedChars.fromString("")
            val session =
                DetailQrSession(backgroundScope, Access(), StandardTestDispatcher(testScheduler)) { _, onAuthorized ->
                    onAuthorized()
                    EntryQrExportResult.Ready(content)
                }

            session.request(entry())
            runCurrent()

            assertEquals(DetailQrState.Failed(EntryQrKind.Wifi), session.state.value)
            assertTrue(content.isEmpty)
            session.close()
        }

    @Test
    fun `invalid credentials show failure while authorization cancellation stays hidden`() =
        runTest {
            var result: EntryQrExportResult = EntryQrExportResult.Unavailable
            var authorizationGranted = true
            val session =
                DetailQrSession(backgroundScope, Access()) { _, onAuthorized ->
                    if (authorizationGranted) onAuthorized()
                    result
                }
            session.request(entry())
            runCurrent()
            assertEquals(DetailQrState.Failed(EntryQrKind.Wifi), session.state.value)

            result = EntryQrExportResult.Cancelled
            authorizationGranted = false
            session.request(entry())
            runCurrent()
            assertEquals(DetailQrState.Hidden, session.state.value)
        }

    private class Access : SecureSessionAccessState {
        override val authenticationState =
            MutableStateFlow<AuthenticationState>(
                AuthenticationState.Authenticated(1L),
            )

        override fun isUnlocked() = authenticationState.value is AuthenticationState.Authenticated
    }

    private fun entry() =
        Entry(
            identity = EntryIdentity(id = EntryId("wifi"), type = EntryType.WIFI, timestamps = EntryTimestamps(1L)),
            profile = EntryProfile(title = "Office"),
            secret = EntrySecret(credential = WifiCredential(ssid = "Office")),
        )

    private fun QrCodeMatrix.anyDarkModule(): Boolean =
        (0 until height).any { row ->
            (0 until width).any { column -> isDark(column, row) }
        }
}
