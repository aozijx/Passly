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
        val Error = EventKey("application.error.reported")
        val PreviousProcessExit = EventKey("application.previous_process_exit")
        val TraceResumeUnavailable = EventKey("application.trace_resume_unavailable")
        val BackgroundLockSkipped = EventKey("application.background_lock_skipped")
        val UnhandledException = EventKey("application.unhandled_exception")
        val NativeLibraryLoadFailed = EventKey("application.native_library_load_failed")
        val PackageIconUnavailable = EventKey("application.package_icon.unavailable")
        val ResourceCleanupFailed = EventKey("application.resource_cleanup.failed")
        val DeviceLockCompleted = EventKey("application.device_lock.completed")
        val DiagnosticsExportFailed = EventKey("application.diagnostics_export.failed")
        val ScannedOtpInvalid = EventKey("application.scanned_otp.invalid")
        val ScannerFailed = EventKey("application.scanner.failed")
        val LogOverflow = EventKey("application.log.overflow")
    }

    object Database {
        val Error = EventKey("database.error.reported")
        val SessionTransition = EventKey("database.session.transition")
        val SessionTransitionFailed = EventKey("database.session.transition_failed")
        val Opened = EventKey("database.opened")
        val OpenFailed = EventKey("database.open_failed")
        val ResetFailed = EventKey("database.reset_failed")
    }

    object Security {
        val Error = EventKey("security.error.reported")
        val LockStarted = EventKey("security.lock.started")
        val LockCompleted = EventKey("security.lock.completed")
        val UnlockStarted = EventKey("security.unlock.started")
        val UnlockCompleted = EventKey("security.unlock.completed")
        val AuthenticationFailed = EventKey("security.authentication.failed")
        val BiometricPromptFailed = EventKey("security.biometric_prompt.failed")
        val VaultInitialized = EventKey("security.vault.initialized")
        val VaultUnlocked = EventKey("security.vault.unlocked")
        val VerificationTagCreated = EventKey("security.verification_tag.created")
        val EnvelopeRekeyed = EventKey("security.envelope.rekeyed")
        val VaultOperationFailed = EventKey("security.vault.operation_failed")
        val VaultDeletionStarted = EventKey("security.vault_deletion.started")
        val VaultDeleted = EventKey("security.vault.deleted")
        val VaultLockStarted = EventKey("security.vault_lock.started")
        val VaultLocked = EventKey("security.vault.locked")
    }

    object Autofill {
        val Error = EventKey("autofill.error.reported")
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
        val ActionIgnored = EventKey("autofill.action.ignored")
    }

    object Backup {
        val Error = EventKey("backup.error.reported")
        val ResourceCleanupFailed = EventKey("backup.resource_cleanup.failed")
    }

    object Vault {
        val Error = EventKey("vault.error.reported")
        val EntryCreateStarted = EventKey("vault.entry.create_started")
        val EntryCreateCompleted = EventKey("vault.entry.create_completed")
        val EntryCreateFailed = EventKey("vault.entry.create_failed")
    }

    val All: Set<EventKey> = setOf(
        Application.Error,
        Application.PreviousProcessExit,
        Application.TraceResumeUnavailable,
        Application.BackgroundLockSkipped,
        Application.UnhandledException,
        Application.NativeLibraryLoadFailed,
        Application.PackageIconUnavailable,
        Application.ResourceCleanupFailed,
        Application.DeviceLockCompleted,
        Application.DiagnosticsExportFailed,
        Application.ScannedOtpInvalid,
        Application.ScannerFailed,
        Application.LogOverflow,
        Database.Error,
        Database.SessionTransition,
        Database.SessionTransitionFailed,
        Database.Opened,
        Database.OpenFailed,
        Database.ResetFailed,
        Security.Error,
        Security.LockStarted,
        Security.LockCompleted,
        Security.UnlockStarted,
        Security.UnlockCompleted,
        Security.AuthenticationFailed,
        Security.BiometricPromptFailed,
        Security.VaultInitialized,
        Security.VaultUnlocked,
        Security.VerificationTagCreated,
        Security.EnvelopeRekeyed,
        Security.VaultOperationFailed,
        Security.VaultDeletionStarted,
        Security.VaultDeleted,
        Security.VaultLockStarted,
        Security.VaultLocked,
        Autofill.Error,
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
        Autofill.ActionIgnored,
        Backup.Error,
        Backup.ResourceCleanupFailed,
        Vault.Error,
        Vault.EntryCreateStarted,
        Vault.EntryCreateCompleted,
        Vault.EntryCreateFailed,
    )
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
    val DatabaseOpen = OperationCode("database_open")
    val DatabaseReset = OperationCode("database_reset")
    val ResourceCleanup = OperationCode("resource_cleanup")
    val PackageIconLoad = OperationCode("package_icon_load")
    val BackupRestore = OperationCode("backup_restore")
    val VaultOtpImport = OperationCode("vault_otp_import")
    val Scanner = OperationCode("scanner")
    val VaultKeyManagement = OperationCode("vault_key_management")
    val VaultEntryCreate = OperationCode("vault_entry_create")
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
    val AuthenticationPurpose = LogFieldKey("authentication_purpose")
    val ExitStatus = LogFieldKey("exit_status")
    val ExitReason = LogFieldKey("exit_reason")
    val Importance = LogFieldKey("importance")
    val PssKb = LogFieldKey("pss_kb")
    val RssKb = LogFieldKey("rss_kb")
    val Source = LogFieldKey("source")
    val ErrorLayer = LogFieldKey("error_layer")
    val Severity = LogFieldKey("severity")
    val Recoverable = LogFieldKey("recoverable")
    val Operation = LogFieldKey("operation")
    val Locked = LogFieldKey("locked")
    val ClearTask = LogFieldKey("clear_task")
    val DroppedDebugCount = LogFieldKey("dropped_debug_count")
    val DroppedInfoCount = LogFieldKey("dropped_info_count")
    val DroppedWarnCount = LogFieldKey("dropped_warn_count")
    val DroppedErrorCount = LogFieldKey("dropped_error_count")
    val DroppedFatalCount = LogFieldKey("dropped_fatal_count")
    val EntryType = LogFieldKey("entry_type")
}
