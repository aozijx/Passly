package com.aozijx.passly.data.repository.access

import androidx.datastore.core.DataStore
import com.aozijx.passly.data.preferences.proto.SecurityPreferences
import com.aozijx.passly.domain.access.model.AuthenticationAttemptState
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ProtoAuthenticationAttemptStoreTest {
    @Test
    fun `persists attempts separately for each credential method`() = runTest {
        val dataStore = InMemoryDataStore(SecurityPreferences.getDefaultInstance())
        val store = ProtoAuthenticationAttemptStore(dataStore)

        (1..20).map {
            async {
                store.update(AuthenticationMethod.APP_PASSWORD) { state ->
                    state.copy(failedAttempts = state.failedAttempts + 1)
                }
            }
        }.awaitAll()

        store.update(AuthenticationMethod.RECOVERY_CODE) {
            AuthenticationAttemptState(failedAttempts = 2)
        }

        assertEquals(20, store.update(AuthenticationMethod.APP_PASSWORD) { it }.failedAttempts)
        assertEquals(2, store.update(AuthenticationMethod.RECOVERY_CODE) { it }.failedAttempts)
        assertEquals(
            "app_password",
            dataStore.value.credentialAttemptsList.first { it.failedAttempts == 20 }.methodKey,
        )
    }

    @Test
    fun `round trips lockout clocks and boot identity`() = runTest {
        val dataStore = InMemoryDataStore(SecurityPreferences.getDefaultInstance())
        val store = ProtoAuthenticationAttemptStore(dataStore)
        val locked =
            AuthenticationAttemptState(
                failedAttempts = 5,
                lockedUntilEpochMs = 2_000_000L,
                lockedUntilElapsedRealtimeMs = 1_500_000L,
                bootCount = 73,
            )

        store.update(AuthenticationMethod.APP_PASSWORD) { locked }

        assertEquals(locked, store.update(AuthenticationMethod.APP_PASSWORD) { it })
        val serialized = dataStore.value.toByteArray()
        val restored =
            ProtoAuthenticationAttemptStore(
                InMemoryDataStore(SecurityPreferences.parseFrom(serialized)),
            )
        assertEquals(locked, restored.update(AuthenticationMethod.APP_PASSWORD) { it })
    }

    private class InMemoryDataStore<T>(initial: T) : DataStore<T> {
        private val mutex = Mutex()
        private val state = MutableStateFlow(initial)
        val value: T get() = state.value

        override val data: Flow<T> = state

        override suspend fun updateData(transform: suspend (t: T) -> T): T =
            mutex.withLock {
                transform(state.value).also { state.value = it }
            }
    }
}
