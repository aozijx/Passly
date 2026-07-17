package com.aozijx.passly.app

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.aozijx.passly.core.log.Logcat
import com.aozijx.passly.data.local.database.DatabaseSession
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 监听 ProcessLifecycleOwner 的前后台切换，触发锁库和数据库会话管理。
 */
@Singleton
class AppLifecycleObserver @Inject constructor(
    private val databaseSession: DatabaseSession
) : DefaultLifecycleObserver {

    override fun onStart(owner: LifecycleOwner) {
        databaseSession.onStart(owner)
    }

    override fun onStop(owner: LifecycleOwner) {
        databaseSession.onStop(owner)
        // 确保应用进入后台时，所有待写入的日志落盘，但不关闭线程池
        Logcat.flushLogs()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        Logcat.shutdown()
    }
}
