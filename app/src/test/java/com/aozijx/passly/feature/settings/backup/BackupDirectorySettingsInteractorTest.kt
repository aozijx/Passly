package com.aozijx.passly.feature.settings.backup

import com.aozijx.passly.core.observability.Events
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.domain.settings.port.BackupDirectorySettingsRepository
import com.aozijx.passly.testing.RecordingTestLogger
import java.io.IOException
import kotlinx.coroutines.async
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class BackupDirectorySettingsInteractorTest {
    private fun createInteractor(
        repository: BackupDirectorySettingsRepository,
        access: BackupDirectoryAccess,
    ) = BackupDirectorySettingsInteractor(repository, access, RecordingTestLogger())

    @Test
    fun `overlapping selections serialize grant ownership`() =
        runTest {
            val operations = mutableListOf<String>()
            val repository = ControlledRepository("content://tree/old", operations)
            val access =
                ControlledAccess(
                    operations = operations,
                    pausedUri = "content://tree/first",
                )
            val interactor = createInteractor(repository, access)

            val first = async { interactor.select("content://tree/first") }
            runCurrent()
            access.firstAcquireStarted.await()
            val second = async { interactor.select("content://tree/second") }
            runCurrent()

            assertEquals(listOf("content://tree/first"), access.acquiredUris)
            access.finishFirstAcquire.complete(Unit)
            assertEquals(BackupDirectorySelectionResult.Saved, first.await())
            assertEquals(BackupDirectorySelectionResult.Saved, second.await())

            assertEquals("content://tree/second", repository.savedUri)
            assertEquals(
                listOf(
                    "acquire:content://tree/first",
                    "save:content://tree/first",
                    "release:content://tree/old",
                    "acquire:content://tree/second",
                    "save:content://tree/second",
                    "release:content://tree/first",
                ),
                operations,
            )
        }

    @Test
    fun `clear after a suspended selection releases the newly persisted uri`() =
        runTest {
            val operations = mutableListOf<String>()
            val repository = ControlledRepository("content://tree/old", operations)
            val access =
                ControlledAccess(
                    operations = operations,
                    pausedUri = "content://tree/new",
                )
            val interactor = createInteractor(repository, access)

            val selection = async { interactor.select("content://tree/new") }
            runCurrent()
            access.firstAcquireStarted.await()
            val clear = async { interactor.clear() }
            runCurrent()
            assertEquals("content://tree/old", repository.savedUri)

            access.finishFirstAcquire.complete(Unit)
            assertEquals(BackupDirectorySelectionResult.Saved, selection.await())
            clear.await()

            assertNull(repository.savedUri)
            assertEquals(
                listOf(
                    "acquire:content://tree/new",
                    "save:content://tree/new",
                    "release:content://tree/old",
                    "clear",
                    "release:content://tree/new",
                ),
                operations,
            )
        }

    @Test
    fun `selection persists only a granted original tree uri`() =
        runTest {
            val repository = FakeRepository()
            val access =
                FakeAccess(
                    acquireResult = BackupDirectoryGrantResult.Granted("content://tree/original"),
                )
            val interactor = createInteractor(repository, access)

            assertEquals(
                BackupDirectorySelectionResult.Saved,
                interactor.select("content://tree/original"),
            )
            assertEquals("content://tree/original", repository.savedUri)
            assertEquals(listOf("content://tree/original"), access.acquiredUris)
        }

    @Test
    fun `cancelled or denied selection never changes settings`() =
        runTest {
            val repository = FakeRepository()
            val access = FakeAccess(BackupDirectoryGrantResult.PermissionDenied)
            val interactor = createInteractor(repository, access)

            assertEquals(BackupDirectorySelectionResult.Cancelled, interactor.select(null))
            assertEquals(
                BackupDirectorySelectionResult.PermissionDenied,
                interactor.select("content://tree/denied"),
            )
            assertNull(repository.savedUri)
        }

    @Test
    fun `clear clears settings before releasing the persisted grant`() =
        runTest {
            val operations = mutableListOf<String>()
            val repository =
                FakeRepository(
                    initialUri = "content://tree/original",
                    operations = operations,
                )
            val access =
                FakeAccess(
                    acquireResult = BackupDirectoryGrantResult.Unavailable,
                    operations = operations,
                )
            val interactor = createInteractor(repository, access)

            interactor.clear()

            assertEquals(
                listOf("clear", "release:content://tree/original"),
                operations,
            )
        }

    @Test
    fun `failed grant release does not restore cleared directory setting`() =
        runTest {
            val operations = mutableListOf<String>()
            val repository =
                FakeRepository(
                    initialUri = "content://tree/original",
                    operations = operations,
                )
            val access =
                FakeAccess(
                    acquireResult = BackupDirectoryGrantResult.Unavailable,
                    operations = operations,
                    failOnReleaseUri = "content://tree/original",
                )
            val logger = RecordingTestLogger()
            val interactor = BackupDirectorySettingsInteractor(repository, access, logger)

            interactor.clear()

            assertNull(repository.savedUri)
            assertEquals(
                listOf("clear", "release:content://tree/original"),
                operations,
            )
            assertEquals(1, logger.events.size)
            assertEquals(Events.Backup.ResourceCleanupFailed, logger.events.single().event)
            assertEquals(Reasons.Application.Storage.StorageFailure, logger.events.single().reason)
            assertEquals(
                IllegalStateException::class.java,
                logger.events
                    .single()
                    .throwable
                    ?.javaClass,
            )
        }

    @Test
    fun `cancellation after clear commit still releases the persisted grant`() =
        runTest {
            val operations = mutableListOf<String>()
            val repository =
                FakeRepository(
                    initialUri = "content://tree/original",
                    operations = operations,
                    cancelAfterClear = true,
                )
            val access =
                FakeAccess(
                    acquireResult = BackupDirectoryGrantResult.Unavailable,
                    operations = operations,
                )
            val interactor = createInteractor(repository, access)

            val cancellation =
                try {
                    interactor.clear()
                    null
                } catch (error: CancellationException) {
                    error
                }

            assertEquals("settings cleared before cancellation", cancellation?.message)
            assertNull(repository.savedUri)
            assertEquals(
                listOf("clear", "release:content://tree/original"),
                operations,
            )
        }

    @Test
    fun `failed clear keeps grant when settings still reference it`() =
        runTest {
            val operations = mutableListOf<String>()
            val repository =
                FakeRepository(
                    initialUri = "content://tree/original",
                    operations = operations,
                    failOnClear = true,
                )
            val access =
                FakeAccess(
                    acquireResult = BackupDirectoryGrantResult.Unavailable,
                    operations = operations,
                )
            val interactor = createInteractor(repository, access)

            val failure =
                try {
                    interactor.clear()
                    null
                } catch (error: IllegalStateException) {
                    error
                }

            assertEquals("settings clear failed", failure?.message)
            assertEquals("content://tree/original", repository.savedUri)
            assertEquals(listOf("clear"), operations)
        }

    @Test
    fun `replacing directory saves new uri before releasing previous grant`() =
        runTest {
            val operations = mutableListOf<String>()
            val repository =
                FakeRepository(
                    initialUri = "content://tree/previous",
                    operations = operations,
                )
            val access =
                FakeAccess(
                    acquireResult = BackupDirectoryGrantResult.Granted("content://tree/new"),
                    operations = operations,
                )
            val interactor = createInteractor(repository, access)

            assertEquals(
                BackupDirectorySelectionResult.Saved,
                interactor.select("content://tree/new"),
            )

            assertEquals(
                listOf(
                    "acquire:content://tree/new",
                    "save:content://tree/new",
                    "release:content://tree/previous",
                ),
                operations,
            )
        }

    @Test
    fun `failed settings write rolls back newly acquired grant`() =
        runTest {
            val operations = mutableListOf<String>()
            val repository =
                FakeRepository(
                    initialUri = "content://tree/previous",
                    operations = operations,
                    failOnSave = true,
                )
            val access =
                FakeAccess(
                    acquireResult = BackupDirectoryGrantResult.Granted("content://tree/new"),
                    operations = operations,
                )
            val interactor = createInteractor(repository, access)

            assertEquals(
                BackupDirectorySelectionResult.Unavailable,
                interactor.select("content://tree/new"),
            )

            assertEquals(
                listOf(
                    "acquire:content://tree/new",
                    "save:content://tree/new",
                    "release:content://tree/new",
                ),
                operations,
            )
        }

    @Test
    fun `cancellation after settings commit retains the newly referenced grant`() =
        runTest {
            val operations = mutableListOf<String>()
            val repository =
                FakeRepository(
                    initialUri = "content://tree/previous",
                    operations = operations,
                    cancelAfterSave = true,
                )
            val access =
                FakeAccess(
                    acquireResult = BackupDirectoryGrantResult.Granted("content://tree/new"),
                    operations = operations,
                )
            val interactor = createInteractor(repository, access)

            val cancellation =
                try {
                    interactor.select("content://tree/new")
                    null
                } catch (error: CancellationException) {
                    error
                }

            assertEquals("settings committed before cancellation", cancellation?.message)
            assertEquals("content://tree/new", repository.savedUri)
            assertEquals(
                listOf("acquire:content://tree/new", "save:content://tree/new"),
                operations,
            )
        }

    @Test
    fun `failure releasing previous grant does not revoke newly saved directory`() =
        runTest {
            val operations = mutableListOf<String>()
            val repository =
                FakeRepository(
                    initialUri = "content://tree/previous",
                    operations = operations,
                )
            val access =
                FakeAccess(
                    acquireResult = BackupDirectoryGrantResult.Granted("content://tree/new"),
                    operations = operations,
                    failOnReleaseUri = "content://tree/previous",
                )
            val logger = RecordingTestLogger()
            val interactor = BackupDirectorySettingsInteractor(repository, access, logger)

            assertEquals(
                BackupDirectorySelectionResult.Saved,
                interactor.select("content://tree/new"),
            )

            assertEquals("content://tree/new", repository.savedUri)
            assertEquals(1, logger.events.size)
            assertEquals(Events.Backup.ResourceCleanupFailed, logger.events.single().event)
            assertEquals(Reasons.Application.Storage.StorageFailure, logger.events.single().reason)
            assertEquals(
                listOf(
                    "acquire:content://tree/new",
                    "save:content://tree/new",
                    "release:content://tree/previous",
                ),
                operations,
            )
        }

    @Test
    fun `fatal settings write failure releases unreferenced grant and propagates`() =
        runTest {
            val operations = mutableListOf<String>()
            val fatal = LinkageError("fatal settings write")
            val repository =
                FakeRepository(
                    initialUri = "content://tree/previous",
                    operations = operations,
                    fatalOnSave = fatal,
                )
            val access =
                FakeAccess(
                    acquireResult = BackupDirectoryGrantResult.Granted("content://tree/new"),
                    operations = operations,
                )
            val interactor = createInteractor(repository, access)

            val observed =
                try {
                    interactor.select("content://tree/new")
                    null
                } catch (failure: Throwable) {
                    failure
                }

            assertEquals(LinkageError::class.java, observed?.javaClass)
            assertEquals("fatal settings write", observed?.message)
            assertEquals("content://tree/previous", repository.savedUri)
            assertEquals(
                listOf("acquire:content://tree/new", "save:content://tree/new", "release:content://tree/new"),
                operations,
            )
        }

    @Test
    fun `fatal compensation read failure propagates and keeps acquired grant`() =
        runTest {
            val fatal = LinkageError("fatal settings read")
            var subscriptions = 0
            val repository =
                object : BackupDirectorySettingsRepository {
                    override val backupDirectoryUri: Flow<String?> =
                        flow {
                            subscriptions++
                            if (subscriptions == 2) throw fatal
                            emit("content://tree/previous")
                        }

                    override suspend fun setBackupDirectoryUri(uri: String): Unit = throw IOException("settings write failed")

                    override suspend fun clearBackupDirectoryUri() = Unit
                }
            val access = FakeAccess(BackupDirectoryGrantResult.Granted("content://tree/new"))
            val interactor = createInteractor(repository, access)

            val observed =
                try {
                    interactor.select("content://tree/new")
                    null
                } catch (failure: Throwable) {
                    failure
                }

            assertEquals(LinkageError::class.java, observed?.javaClass)
            assertEquals("fatal settings read", observed?.message)
            assertEquals("content://tree/new", access.acquiredUris.single())
        }

    @Test
    fun `fatal grant release failure propagates with write failure retained`() =
        runTest {
            val fatal = LinkageError("fatal grant release")
            val operations = mutableListOf<String>()
            val repository =
                FakeRepository(
                    initialUri = "content://tree/previous",
                    operations = operations,
                    failOnSave = true,
                )
            val access =
                FakeAccess(
                    acquireResult = BackupDirectoryGrantResult.Granted("content://tree/new"),
                    operations = operations,
                    failOnReleaseUri = "content://tree/new",
                    fatalOnRelease = fatal,
                )
            val interactor = createInteractor(repository, access)

            val observed =
                try {
                    interactor.select("content://tree/new")
                    null
                } catch (failure: Throwable) {
                    failure
                }

            assertEquals(LinkageError::class.java, observed?.javaClass)
            assertEquals("fatal grant release", observed?.message)
            assertEquals(
                listOf("acquire:content://tree/new", "save:content://tree/new", "release:content://tree/new"),
                operations,
            )
        }

    @Test
    fun `ordinary clear failure after durable clear reports committed success`() =
        runTest {
            val operations = mutableListOf<String>()
            val repository =
                FakeRepository(
                    initialUri = "content://tree/original",
                    operations = operations,
                    failAfterClear = true,
                )
            val access =
                FakeAccess(
                    acquireResult = BackupDirectoryGrantResult.Unavailable,
                    operations = operations,
                )
            val interactor = createInteractor(repository, access)

            interactor.clear()

            assertNull(repository.savedUri)
            assertEquals(listOf("clear", "release:content://tree/original"), operations)
        }

    private class FakeRepository(
        initialUri: String? = null,
        private val operations: MutableList<String> = mutableListOf(),
        private val failOnSave: Boolean = false,
        private val fatalOnSave: Throwable? = null,
        private val cancelAfterSave: Boolean = false,
        private val cancelAfterClear: Boolean = false,
        private val failOnClear: Boolean = false,
        private val failAfterClear: Boolean = false,
    ) : BackupDirectorySettingsRepository {
        private val state = MutableStateFlow(initialUri)
        override val backupDirectoryUri: Flow<String?> = state
        var savedUri: String? = initialUri
            private set

        override suspend fun setBackupDirectoryUri(uri: String) {
            operations += "save:$uri"
            fatalOnSave?.let { throw it }
            if (failOnSave) error("settings write failed")
            savedUri = uri
            state.value = uri
            if (cancelAfterSave) throw CancellationException("settings committed before cancellation")
        }

        override suspend fun clearBackupDirectoryUri() {
            operations += "clear"
            if (failOnClear) error("settings clear failed")
            savedUri = null
            state.value = null
            if (failAfterClear) throw IOException("settings clear failed after commit")
            if (cancelAfterClear) throw CancellationException("settings cleared before cancellation")
        }
    }

    private class FakeAccess(
        private val acquireResult: BackupDirectoryGrantResult,
        private val operations: MutableList<String> = mutableListOf(),
        private val failOnReleaseUri: String? = null,
        private val fatalOnRelease: Throwable? = null,
    ) : BackupDirectoryAccess {
        val acquiredUris = mutableListOf<String>()

        override suspend fun acquire(treeUri: String): BackupDirectoryGrantResult {
            operations += "acquire:$treeUri"
            acquiredUris += treeUri
            return acquireResult
        }

        override suspend fun release(treeUri: String) {
            operations += "release:$treeUri"
            if (treeUri == failOnReleaseUri) fatalOnRelease?.let { throw it }
            if (treeUri == failOnReleaseUri) error("grant release failed")
        }
    }

    private class ControlledRepository(
        initialUri: String?,
        private val operations: MutableList<String>,
    ) : BackupDirectorySettingsRepository {
        private val state = MutableStateFlow(initialUri)
        override val backupDirectoryUri: Flow<String?> = state
        val savedUri: String? get() = state.value

        override suspend fun setBackupDirectoryUri(uri: String) {
            operations += "save:$uri"
            state.value = uri
        }

        override suspend fun clearBackupDirectoryUri() {
            operations += "clear"
            state.value = null
        }
    }

    private class ControlledAccess(
        private val operations: MutableList<String>,
        private val pausedUri: String,
    ) : BackupDirectoryAccess {
        val acquiredUris = mutableListOf<String>()
        val firstAcquireStarted = CompletableDeferred<Unit>()
        val finishFirstAcquire = CompletableDeferred<Unit>()

        override suspend fun acquire(treeUri: String): BackupDirectoryGrantResult {
            acquiredUris += treeUri
            operations += "acquire:$treeUri"
            if (treeUri == pausedUri) {
                firstAcquireStarted.complete(Unit)
                finishFirstAcquire.await()
            }
            return BackupDirectoryGrantResult.Granted(treeUri)
        }

        override suspend fun release(treeUri: String) {
            operations += "release:$treeUri"
        }
    }
}
