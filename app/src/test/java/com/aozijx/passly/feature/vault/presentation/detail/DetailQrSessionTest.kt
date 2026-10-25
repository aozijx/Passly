package com.aozijx.passly.feature.vault.presentation.detail

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
import com.aozijx.passly.feature.vault.detail.EntryQrKind
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DetailQrSessionTest {
    @Test
    fun `cancelled export does not leave a loading sheet`() = runTest {
        val session = DetailQrSession(backgroundScope, Access()) {
            throw CancellationException("cancelled")
        }

        session.request(entry())
        runCurrent()

        assertEquals(DetailQrState.Hidden, session.state.value)
    }

    @Test
    fun `destroying the owner scope wipes content and ignores further requests`() = runTest {
        val content = OwnedChars.fromString("WIFI:synthetic")
        val session = DetailQrSession(backgroundScope, Access()) {
            EntryQrExportResult.Ready(EntryQrKind.Wifi, content)
        }
        session.request(entry())
        runCurrent()

        backgroundScope.cancel()
        runCurrent()

        assertTrue(content.isEmpty)
        assertEquals(DetailQrState.Hidden, session.state.value)
        session.request(entry())
        assertEquals(DetailQrState.Hidden, session.state.value)
    }

    @Test
    fun `closing content also clears derived resources including late registration`() {
        val owned = OwnedChars.fromString("WIFI:synthetic")
        val content = DetailQrPayload(owned)
        var cleanups = 0
        content.onClose { cleanups++ }

        content.close()
        content.close()
        content.onClose { cleanups++ }

        assertTrue(owned.isEmpty)
        assertEquals(2, cleanups)
    }

    @Test
    fun `dismissal wipes the displayed payload`() = runTest {
        val content = OwnedChars.fromString("WIFI:synthetic")
        val session = DetailQrSession(backgroundScope, Access()) {
            EntryQrExportResult.Ready(EntryQrKind.Wifi, content)
        }

        session.request(entry())
        runCurrent()
        assertTrue(session.state.value is DetailQrState.Ready)
        val ready = session.state.value as DetailQrState.Ready
        assertEquals("WIFI:synthetic", ready.content.useChars(::String))

        session.close()

        assertEquals(DetailQrState.Hidden, session.state.value)
        assertTrue(content.isEmpty)
        assertTrue(ready.content.isEmpty)
    }

    @Test
    fun `locking the vault closes and wipes the code`() = runTest {
        val access = Access()
        val content = OwnedChars.fromString("WIFI:synthetic")
        val session = DetailQrSession(backgroundScope, access) {
            EntryQrExportResult.Ready(EntryQrKind.Wifi, content)
        }
        session.request(entry())
        runCurrent()

        access.authenticationState.value = AuthenticationState.Locked
        runCurrent()

        assertEquals(DetailQrState.Hidden, session.state.value)
        assertTrue(content.isEmpty)
    }

    @Test
    fun `late export cannot reopen a dismissed code and is wiped`() = runTest {
        val result = CompletableDeferred<EntryQrExportResult>()
        val content = OwnedChars.fromString("WIFI:synthetic")
        val session = DetailQrSession(backgroundScope, Access()) {
            withContext(NonCancellable) { result.await() }
        }
        session.request(entry())
        runCurrent()
        assertEquals(DetailQrState.Loading(EntryQrKind.Wifi), session.state.value)

        session.close()
        result.complete(EntryQrExportResult.Ready(EntryQrKind.Wifi, content))
        runCurrent()

        assertEquals(DetailQrState.Hidden, session.state.value)
        assertTrue(content.isEmpty)
    }

    @Test
    fun `request while locked never exports`() = runTest {
        val access = Access().apply { authenticationState.value = AuthenticationState.Locked }
        var exports = 0
        val session = DetailQrSession(backgroundScope, access) {
            exports++
            EntryQrExportResult.Unavailable
        }

        session.request(entry())
        runCurrent()

        assertEquals(0, exports)
        assertEquals(DetailQrState.Hidden, session.state.value)
    }

    @Test
    fun `replacement wipes the previous code`() = runTest {
        val contents = mutableListOf<OwnedChars>()
        val session = DetailQrSession(backgroundScope, Access()) {
            val content = OwnedChars.fromString("WIFI:${contents.size}").also(contents::add)
            EntryQrExportResult.Ready(EntryQrKind.Wifi, content)
        }
        session.request(entry())
        runCurrent()

        session.request(entry())
        runCurrent()

        assertEquals(2, contents.size)
        assertTrue(contents.first().isEmpty)
        assertTrue(!contents.last().isEmpty)
        session.close()
    }

    @Test
    fun `invalid credentials show failure while authorization cancellation stays hidden`() = runTest {
        var result: EntryQrExportResult = EntryQrExportResult.Unavailable
        val session = DetailQrSession(backgroundScope, Access()) { result }
        session.request(entry())
        runCurrent()
        assertEquals(DetailQrState.Failed(EntryQrKind.Wifi), session.state.value)

        result = EntryQrExportResult.Cancelled
        session.request(entry())
        runCurrent()
        assertEquals(DetailQrState.Hidden, session.state.value)
    }

    private class Access : SecureSessionAccessState {
        override val authenticationState = MutableStateFlow<AuthenticationState>(
            AuthenticationState.Authenticated(1L),
        )
        override fun isUnlocked() = authenticationState.value is AuthenticationState.Authenticated
    }

    private fun entry() = Entry(
        identity = EntryIdentity(id = EntryId("wifi"), type = EntryType.WIFI, timestamps = EntryTimestamps(1L)),
        profile = EntryProfile(title = "Office"),
        secret = EntrySecret(credential = WifiCredential(ssid = "Office")),
    )
}
