package com.aozijx.passly.data.local.database.session

import com.aozijx.passly.runtime.session.SecureSessionState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DatabaseReadAccessTest {
    @Test
    fun `read access follows both authorization and database session availability`() = runTest {
        val authorized = MutableStateFlow(true)
        val databaseState = MutableStateFlow(SecureSessionState.UNLOCKED)
        val observed = mutableListOf<Boolean>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observeDatabaseReadAccess(authorized, databaseState).toList(observed)
        }

        databaseState.value = SecureSessionState.SEALED
        databaseState.value = SecureSessionState.UNLOCKED
        authorized.value = false
        databaseState.value = SecureSessionState.UNLOCKED

        assertEquals(listOf(true, false, true, false), observed)
    }
}
