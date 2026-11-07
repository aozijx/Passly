package com.aozijx.passly.security.authentication.method.credential
import com.aozijx.passly.core.crypto.KeyDerivation
import com.aozijx.passly.domain.access.model.AuthenticationKdfParameters
import com.aozijx.passly.domain.access.port.AuthenticationKdfConfigurationStore
import com.aozijx.passly.domain.access.port.AuthenticationKdfParameterSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val KIB_PER_MIB = 1024
private const val NANOS_PER_MILLI = 1_000_000L
private const val FAST_BASELINE_MILLIS = 150L
private const val MODERATE_BASELINE_MILLIS = 250L
private const val LARGE_HEAP_MIB = 384
private const val MIN_PROCESSORS_FOR_LARGE_MEMORY = 4
private const val BASELINE_MEMORY_KIB = 65_536
private const val LARGE_MEMORY_KIB = 131_072
private const val BASELINE_ITERATIONS = 3
private const val INCREASED_ITERATIONS = 4
private const val AUTH_PARALLELISM = 4
private const val AUTH_KEY_LENGTH_BITS = 256

internal object DeviceKdfPolicy {
    fun select(
        heapMiB: Int,
        processors: Int,
        baselineMillis: Long,
    ): AuthenticationKdfParameters {
        val largeMemory =
            heapMiB >= LARGE_HEAP_MIB &&
                processors >= MIN_PROCESSORS_FOR_LARGE_MEMORY &&
                baselineMillis <= FAST_BASELINE_MILLIS
        return AuthenticationKdfParameters(
            version = KeyDerivation.ARGON2_VERSION_13,
            memoryKiB = if (largeMemory) LARGE_MEMORY_KIB else BASELINE_MEMORY_KIB,
            iterations = if (!largeMemory && baselineMillis <= MODERATE_BASELINE_MILLIS) INCREASED_ITERATIONS else BASELINE_ITERATIONS,
            parallelism = AUTH_PARALLELISM,
            keyLengthBits = AUTH_KEY_LENGTH_BITS,
        )
    }
}

@Singleton
internal class DeviceKdfConfiguration
    @Inject
    constructor(
        private val store: AuthenticationKdfConfigurationStore,
    ) : AuthenticationKdfParameterSource {
        private val mutex = Mutex()

        override suspend fun selected(): AuthenticationKdfParameters =
            mutex.withLock {
                val persisted = store.load()
                if (persisted != null) return@withLock persisted

                val candidate = withContext(Dispatchers.Default) { calibrate() }
                store.initializeIfAbsent(candidate)
            }

        private fun calibrate(): AuthenticationKdfParameters {
            val password = "Passly device calibration".toCharArray()
            val salt = KeyDerivation.generateSalt()
            val heapMiB = (Runtime.getRuntime().maxMemory() / KIB_PER_MIB / KIB_PER_MIB).toInt()
            return try {
                val started = System.nanoTime()
                KeyDerivation.deriveKeyBytesArgon2id(password, salt, KeyDerivation.BASELINE_ARGON2ID_PARAMETERS).fill(0)
                val elapsedMillis = (System.nanoTime() - started) / NANOS_PER_MILLI
                DeviceKdfPolicy.select(heapMiB, Runtime.getRuntime().availableProcessors(), elapsedMillis)
            } finally {
                password.fill('\u0000')
                salt.fill(0)
            }
        }
    }

internal fun AuthenticationKdfParameters.toCore(): KeyDerivation.Argon2idParameters =
    KeyDerivation.Argon2idParameters(
        version = version,
        memoryKiB = memoryKiB,
        iterations = iterations,
        parallelism = parallelism,
        keyLengthBits = keyLengthBits,
    )
