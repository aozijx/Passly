package com.aozijx.passly.data.repository.settings

import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.data.local.datastore.UserConfigFileStore
import com.aozijx.passly.domain.model.UserConfig
import com.aozijx.passly.domain.repository.userconfig.UserConfigRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserConfigRepositoryImpl @Inject constructor(
    private val store: UserConfigFileStore
) : UserConfigRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val _config = MutableStateFlow(UserConfig())

    init {
        scope.launch {
            try {
                _config.value = store.read()
            } catch (e: Exception) {
                Logcat.e("UserConfigRepo", "Failed to load user config", e)
            }
        }
    }

    override val userConfig: Flow<UserConfig> = _config.asStateFlow()

    override suspend fun save(config: UserConfig) {
        mutex.withLock {
            store.write(config)
            _config.value = config
        }
    }

    override suspend fun update(transform: (UserConfig) -> UserConfig) {
        mutex.withLock {
            val updated = transform(_config.value).copy(updatedAt = System.currentTimeMillis())
            store.write(updated)
            _config.value = updated
        }
    }

    override suspend fun reset() {
        save(UserConfig())
    }
}