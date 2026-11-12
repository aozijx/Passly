package com.aozijx.passly.data.local.database.session

import com.aozijx.passly.runtime.session.SecureSessionState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DatabaseReadAccessTest {
    @Test
    fun `read access follows both authorization and database session availability`() =
        runTest {
            val authorized = MutableStateFlow(true)
            val databaseState = MutableStateFlow(SecureSessionState.UNLOCKED)
            val lockGeneration = MutableStateFlow(0L)
            val observed = mutableListOf<DatabaseReadAccess>()

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                observeDatabaseReadAccess(authorized, databaseState, lockGeneration).collect { observed += it }
            }

            databaseState.value = SecureSessionState.SEALED
            lockGeneration.value = 1L
            lockGeneration.value = 2L
            databaseState.value = SecureSessionState.UNLOCKED
            authorized.value = false
            databaseState.value = SecureSessionState.UNLOCKED

            assertEquals(
                listOf(
                    DatabaseReadAccess(true, 0L),
                    DatabaseReadAccess(false, 0L),
                    DatabaseReadAccess(true, 2L),
                    DatabaseReadAccess(false, 0L),
                ),
                observed,
            )
        }

    @Test
    fun `fast lock and reopen transition remains observable through generation`() =
        runTest {
            val authorized = MutableStateFlow(true)
            val databaseState = MutableStateFlow(SecureSessionState.UNLOCKED)
            val lockGeneration = MutableStateFlow(0L)
            val observed = mutableListOf<DatabaseReadAccess>()
            backgroundScope.launch {
                observeDatabaseReadAccess(authorized, databaseState, lockGeneration).collect { observed += it }
            }
            runCurrent()

            databaseState.value = SecureSessionState.SEALED
            lockGeneration.value = 1L
            lockGeneration.value = 2L
            databaseState.value = SecureSessionState.UNLOCKED
            runCurrent()

            assertEquals(DatabaseReadAccess(true, 2L), observed.last())
        }
}
