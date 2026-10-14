package com.aozijx.passly.app.observability

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build

enum class PreviousExitReason {
    EXIT_SELF,
    SIGNALED,
    LOW_MEMORY,
    CRASH,
    CRASH_NATIVE,
    ANR,
    INITIALIZATION_FAILURE,
    PERMISSION_CHANGE,
    EXCESSIVE_RESOURCE_USAGE,
    USER_REQUESTED,
    USER_STOPPED,
    DEPENDENCY_DIED,
    OTHER,
    UNKNOWN,
}

enum class PreviousExitImportance {
    FOREGROUND,
    FOREGROUND_SERVICE,
    VISIBLE,
    PERCEPTIBLE,
    SERVICE,
    CACHED,
    GONE,
    UNKNOWN,
}

data class PreviousExit(
    val timestampMs: Long,
    val reason: PreviousExitReason,
    val status: Int,
    val importance: PreviousExitImportance,
    val pssKb: Long,
    val rssKb: Long,
)

internal data class PlatformPreviousExit(
    val timestampMs: Long,
    val reason: PreviousExitReason,
    val status: Int,
    val importance: PreviousExitImportance,
    val pssKb: Long,
    val rssKb: Long,
)

internal fun interface PreviousProcessExitGateway {
    fun historicalExits(): List<PlatformPreviousExit>
}

internal interface PreviousExitCursor {
    fun readTimestampMs(): Long
    fun writeTimestampMs(timestampMs: Long)
}

fun interface PreviousExitSource {
    fun readUnrecorded(): PreviousExit?
}

class PreviousProcessExitReader internal constructor(
    private val gateway: PreviousProcessExitGateway,
    private val cursor: PreviousExitCursor,
) : PreviousExitSource {
    private var inMemoryTimestampMs = 0L

    @Synchronized
    override fun readUnrecorded(): PreviousExit? {
        val recordedTimestamp = try {
            cursor.readTimestampMs()
        } catch (_: Exception) {
            0L
        }.coerceAtLeast(inMemoryTimestampMs)
        val candidate = try {
            gateway.historicalExits()
        } catch (_: Exception) {
            emptyList()
        }.asSequence()
            .filter { it.timestampMs > recordedTimestamp }
            .filter { it.timestampMs >= 0 && it.pssKb >= 0 && it.rssKb >= 0 }
            .maxByOrNull { it.timestampMs }
            ?: return null

        inMemoryTimestampMs = candidate.timestampMs
        try {
            cursor.writeTimestampMs(candidate.timestampMs)
        } catch (_: Exception) {
            // Diagnostics cursor persistence cannot affect application startup.
        }
        return PreviousExit(
            timestampMs = candidate.timestampMs,
            reason = candidate.reason,
            status = candidate.status,
            importance = candidate.importance,
            pssKb = candidate.pssKb,
            rssKb = candidate.rssKb,
        )
    }
}

internal class AndroidPreviousProcessExitGateway(
    context: Context,
) : PreviousProcessExitGateway {
    private val activityManager = context.getSystemService(ActivityManager::class.java)

    override fun historicalExits(): List<PlatformPreviousExit> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return emptyList()
        return activityManager.getHistoricalProcessExitReasons(null, 0, MAX_EXIT_RECORDS).map { exit ->
            PlatformPreviousExit(
                timestampMs = exit.timestamp,
                reason = exit.reason.toSafeReason(),
                status = exit.status,
                importance = exit.importance.toSafeImportance(),
                pssKb = exit.pss,
                rssKb = exit.rss,
            )
        }
    }

    private fun Int.toSafeReason(): PreviousExitReason = when (this) {
        ApplicationExitInfo.REASON_EXIT_SELF -> PreviousExitReason.EXIT_SELF
        ApplicationExitInfo.REASON_SIGNALED -> PreviousExitReason.SIGNALED
        ApplicationExitInfo.REASON_LOW_MEMORY -> PreviousExitReason.LOW_MEMORY
        ApplicationExitInfo.REASON_CRASH -> PreviousExitReason.CRASH
        ApplicationExitInfo.REASON_CRASH_NATIVE -> PreviousExitReason.CRASH_NATIVE
        ApplicationExitInfo.REASON_ANR -> PreviousExitReason.ANR
        ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> PreviousExitReason.INITIALIZATION_FAILURE
        ApplicationExitInfo.REASON_PERMISSION_CHANGE -> PreviousExitReason.PERMISSION_CHANGE
        ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> PreviousExitReason.EXCESSIVE_RESOURCE_USAGE
        ApplicationExitInfo.REASON_USER_REQUESTED -> PreviousExitReason.USER_REQUESTED
        ApplicationExitInfo.REASON_USER_STOPPED -> PreviousExitReason.USER_STOPPED
        ApplicationExitInfo.REASON_DEPENDENCY_DIED -> PreviousExitReason.DEPENDENCY_DIED
        ApplicationExitInfo.REASON_OTHER -> PreviousExitReason.OTHER
        else -> PreviousExitReason.UNKNOWN
    }

    private fun Int.toSafeImportance(): PreviousExitImportance = when (this) {
        ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND -> PreviousExitImportance.FOREGROUND
        ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE ->
            PreviousExitImportance.FOREGROUND_SERVICE
        ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE,
        ActivityManager.RunningAppProcessInfo.IMPORTANCE_TOP_SLEEPING,
        -> PreviousExitImportance.VISIBLE
        ActivityManager.RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE -> PreviousExitImportance.PERCEPTIBLE
        ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE -> PreviousExitImportance.SERVICE
        ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED -> PreviousExitImportance.CACHED
        ActivityManager.RunningAppProcessInfo.IMPORTANCE_GONE -> PreviousExitImportance.GONE
        else -> PreviousExitImportance.UNKNOWN
    }

    private companion object {
        const val MAX_EXIT_RECORDS = 8
    }
}

internal class SharedPreferencesPreviousExitCursor(context: Context) : PreviousExitCursor {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun readTimestampMs(): Long = preferences.getLong(KEY_TIMESTAMP_MS, 0)

    override fun writeTimestampMs(timestampMs: Long) {
        preferences.edit().putLong(KEY_TIMESTAMP_MS, timestampMs).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "observability_state"
        const val KEY_TIMESTAMP_MS = "previous_exit_timestamp_ms"
    }
}
