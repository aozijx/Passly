package com.aozijx.passly.core.observability

private val EVENT_PATTERN = Regex("^[a-z][a-z0-9]*(?:\\.[a-z][a-z0-9_]*)+$")
private val FIELD_PATTERN = Regex("^[a-z][a-z0-9_]{0,63}$")
private val CODE_PATTERN = Regex("^[A-Z][A-Z0-9_]{2,63}$")
private val ENUM_PATTERN = Regex("^[A-Z][A-Z0-9_]{1,63}$")
private val OPERATION_PATTERN = Regex("^[a-z][a-z0-9_]{2,126}$")

@JvmInline
value class EventKey(val value: String) {
    init {
        require(value.length in 3..96 && EVENT_PATTERN.matches(value)) { "Invalid event key" }
    }
}

@JvmInline
value class LogFieldKey(val value: String) {
    init {
        require(FIELD_PATTERN.matches(value)) { "Invalid log field key" }
    }
}

@JvmInline
value class ErrorCode(val value: String) {
    init {
        require(CODE_PATTERN.matches(value)) { "Invalid error code" }
    }
}

@JvmInline
value class ReasonCode(val value: String) {
    init {
        require(CODE_PATTERN.matches(value)) { "Invalid reason code" }
    }
}

@JvmInline
value class OperationCode(val value: String) {
    init {
        require(OPERATION_PATTERN.matches(value)) { "Invalid operation code" }
    }
}

@JvmInline
value class EnumName(val value: String) {
    init {
        require(ENUM_PATTERN.matches(value)) { "Invalid enum name" }
    }
}

object Events {
    object Application {
        val PreviousProcessExit = EventKey("application.previous_process_exit")
        val TraceResumeUnavailable = EventKey("application.trace_resume_unavailable")
        val BackgroundLockSkipped = EventKey("application.background_lock_skipped")
        val UnhandledException = EventKey("application.unhandled_exception")
        val NativeLibraryLoadFailed = EventKey("application.native_library_load_failed")
    }

    object Database {
        val SessionTransition = EventKey("database.session.transition")
        val SessionTransitionFailed = EventKey("database.session.transition_failed")
    }

    object Security {
        val LockStarted = EventKey("security.lock.started")
        val LockCompleted = EventKey("security.lock.completed")
        val UnlockStarted = EventKey("security.unlock.started")
        val UnlockCompleted = EventKey("security.unlock.completed")
        val AuthenticationFailed = EventKey("security.authentication.failed")
    }

    object Autofill {
        val DiscoveryStarted = EventKey("autofill.discovery.started")
        val DiscoveryCompleted = EventKey("autofill.discovery.completed")
        val DiscoveryUnavailable = EventKey("autofill.discovery.unavailable")
        val AuthenticationStarted = EventKey("autofill.authentication.started")
        val AuthenticationCompleted = EventKey("autofill.authentication.completed")
        val ResultDeliveryStarted = EventKey("autofill.result.delivery_started")
        val ResultDelivered = EventKey("autofill.result.delivered")
        val ResultDeliveryFailed = EventKey("autofill.result.delivery_failed")
        val SessionCloseStarted = EventKey("autofill.session.close_started")
        val SessionClosed = EventKey("autofill.session.closed")
        val SessionCloseFailed = EventKey("autofill.session.close_failed")
        val SaveStarted = EventKey("autofill.save.started")
        val SaveStaged = EventKey("autofill.save.staged")
        val SaveCompleted = EventKey("autofill.save.completed")
        val SaveFailed = EventKey("autofill.save.failed")
    }

    val All: Set<EventKey> = setOf(
        Application.PreviousProcessExit,
        Application.TraceResumeUnavailable,
        Application.BackgroundLockSkipped,
        Application.UnhandledException,
        Application.NativeLibraryLoadFailed,
        Database.SessionTransition,
        Database.SessionTransitionFailed,
        Security.LockStarted,
        Security.LockCompleted,
        Security.UnlockStarted,
        Security.UnlockCompleted,
        Security.AuthenticationFailed,
        Autofill.DiscoveryStarted,
        Autofill.DiscoveryCompleted,
        Autofill.DiscoveryUnavailable,
        Autofill.AuthenticationStarted,
        Autofill.AuthenticationCompleted,
        Autofill.ResultDeliveryStarted,
        Autofill.ResultDelivered,
        Autofill.ResultDeliveryFailed,
        Autofill.SessionCloseStarted,
        Autofill.SessionClosed,
        Autofill.SessionCloseFailed,
        Autofill.SaveStarted,
        Autofill.SaveStaged,
        Autofill.SaveCompleted,
        Autofill.SaveFailed,
    )
}

object ErrorCodes {
    val AutofillDiscoveryUnavailable = ErrorCode("AUTOFILL_DISCOVERY_UNAVAILABLE")
    val AutofillResultDeliveryFailed = ErrorCode("AUTOFILL_RESULT_DELIVERY_FAILED")
    val AutofillSessionCloseFailed = ErrorCode("AUTOFILL_SESSION_CLOSE_FAILED")
    val AutofillSaveFailed = ErrorCode("AUTOFILL_SAVE_FAILED")
    val DatabaseSessionOpenFailed = ErrorCode("DATABASE_SESSION_OPEN_FAILED")
    val DatabaseSessionCloseFailed = ErrorCode("DATABASE_SESSION_CLOSE_FAILED")
    val AuthenticationFailed = ErrorCode("AUTHENTICATION_FAILED")
    val DiagnosticsExportFailed = ErrorCode("DIAGNOSTICS_EXPORT_FAILED")
    val ApplicationUnhandledException = ErrorCode("APPLICATION_UNHANDLED_EXCEPTION")
    val NativeLibraryLoadFailed = ErrorCode("NATIVE_LIBRARY_LOAD_FAILED")
}

object Reasons {
    val NoFillableFields = ReasonCode("NO_FILLABLE_FIELDS")
    val SessionRestricted = ReasonCode("SESSION_RESTRICTED")
    val DatabaseUnavailable = ReasonCode("DATABASE_UNAVAILABLE")
    val NoMatchingEntry = ReasonCode("NO_MATCHING_ENTRY")
    val PlatformCallbackFailed = ReasonCode("PLATFORM_CALLBACK_FAILED")
    val SessionCloseFailed = ReasonCode("SESSION_CLOSE_FAILED")
    val AuthenticationRejected = ReasonCode("AUTHENTICATION_REJECTED")
    val StorageFailure = ReasonCode("STORAGE_FAILURE")
    val UnknownFailure = ReasonCode("UNKNOWN_FAILURE")
    val NativeLibraryUnavailable = ReasonCode("NATIVE_LIBRARY_UNAVAILABLE")
}

object Operations {
    val ApplicationStartup = OperationCode("application_startup")
    val ApplicationLifecycle = OperationCode("application_lifecycle")
    val DatabaseSession = OperationCode("database_session")
    val SecurityAuthentication = OperationCode("security_authentication")
    val SecurityLock = OperationCode("security_lock")
    val AutofillLegacyRequest = OperationCode("autofill_legacy_request")
    val AutofillCredentialRequest = OperationCode("autofill_credential_request")
    val AutofillResultDelivery = OperationCode("autofill_result_delivery")
    val AutofillSessionClose = OperationCode("autofill_session_close")
    val DiagnosticsExport = OperationCode("diagnostics_export")
}

object LogFieldKeys {
    val RetryCount = LogFieldKey("retry_count")
    val ElapsedMs = LogFieldKey("elapsed_ms")
    val TargetPackage = LogFieldKey("target_package")
    val BrowserPackage = LogFieldKey("browser_package")
    val FromState = LogFieldKey("from_state")
    val ToState = LogFieldKey("to_state")
    val LockType = LogFieldKey("lock_type")
    val Reason = LogFieldKey("reason")
    val Owner = LogFieldKey("owner")
    val TimeoutMs = LogFieldKey("timeout_ms")
    val ActiveLeaseCount = LogFieldKey("active_lease_count")
    val LockReason = LogFieldKey("lock_reason")
    val Trigger = LogFieldKey("trigger")
    val Result = LogFieldKey("result")
    val AuthenticationMethod = LogFieldKey("authentication_method")
    val ExitStatus = LogFieldKey("exit_status")
    val ExitReason = LogFieldKey("exit_reason")
    val Importance = LogFieldKey("importance")
    val PssKb = LogFieldKey("pss_kb")
    val RssKb = LogFieldKey("rss_kb")
    val Source = LogFieldKey("source")
}
