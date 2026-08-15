package com.aozijx.passly.data.local.database.port

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 保险库数据刷新通知器。
 *
 * 用于跨模块传递"数据库已重建、需要重新订阅 Room Flow"的信号。
 * 例如：清除数据库后通知 [VaultViewModel] 重新查询。
 */
@Singleton
class EntryDataRefreshNotifier @Inject constructor() {

    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun notifyRefresh() {
        _events.tryEmit(Unit)
    }
}
