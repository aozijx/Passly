package com.aozijx.passly.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.runtime.session.SecureSessionState

/** Prevents a Room paging source from continuing after the vault is locked. */
class SessionLockedException : IllegalStateException("Database session is locked")

internal class SessionAwarePagingSource<Value : Any>(
    private val session: AppDatabaseSession,
    private val delegate: PagingSource<Int, Value>,
) : PagingSource<Int, Value>() {

    init {
        checkUnlocked()
        delegate.registerInvalidatedCallback { invalidate() }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Value> = try {
        checkUnlocked()
        val result = delegate.load(params)
        checkUnlocked()
        result
    } catch (error: SessionLockedException) {
        delegate.invalidate()
        invalidate()
        LoadResult.Error(error)
    }

    override fun getRefreshKey(state: PagingState<Int, Value>): Int? =
        delegate.getRefreshKey(state)

    private fun checkUnlocked() {
        if (session.lockState != SecureSessionState.UNLOCKED) {
            throw SessionLockedException()
        }
    }
}
