package com.aozijx.passly.data.repository.backup

import kotlinx.coroutines.CancellationException

internal inline fun reportBestEffortBackupCleanupFailure(report: () -> Unit) {
    try {
        report()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Error) {
        throw error
    } catch (_: Exception) {
        // Cleanup is post-commit; ordinary logging failure cannot change restore success.
    }
}
