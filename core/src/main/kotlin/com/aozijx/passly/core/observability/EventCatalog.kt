package com.aozijx.passly.core.observability

private val EVENT_PATTERN = Regex("^[a-z][a-z0-9]*(?:\\.[a-z][a-z0-9_]*)+$")
private val FIELD_PATTERN = Regex("^[a-z][a-z0-9_]{0,63}$")
private val CODE_PATTERN = Regex("^[A-Z][A-Z0-9_]{2,63}$")
private val ENUM_PATTERN = Regex("^[A-Z][A-Z0-9_]{1,63}$")
private val OPERATION_PATTERN = Regex("^[a-z][a-z0-9_]{2,126}$")

@JvmInline
value class EventKey(
    val value: String,
) {
    init {
        require(value.length in 3..96 && EVENT_PATTERN.matches(value)) { "Invalid event key" }
    }
}

class LogFieldKey private constructor(
    val value: String,
    internal val kind: LogFieldKind?,
) {
    constructor(value: String) : this(value, null)

    init {
        require(FIELD_PATTERN.matches(value)) { "Invalid log field key" }
    }

    override fun equals(other: Any?): Boolean = other is LogFieldKey && value == other.value

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value

    companion object {
        internal fun count(value: String) = typed(value, LogFieldKind.COUNT)

        internal fun duration(value: String) = typed(value, LogFieldKind.DURATION)

        internal fun ratio(value: String) = typed(value, LogFieldKind.RATIO)

        internal fun boolean(value: String) = typed(value, LogFieldKind.BOOLEAN)

        internal fun enum(value: String) = typed(value, LogFieldKind.ENUM)

        internal fun errorCode(value: String) = typed(value, LogFieldKind.ERROR_CODE)

        internal fun reason(value: String) = typed(value, LogFieldKind.REASON)

        internal fun operation(value: String) = typed(value, LogFieldKind.OPERATION)

        internal fun targetPackage(value: String) = typed(value, LogFieldKind.HASHED_TARGET_PACKAGE)

        internal fun browserPackage(value: String) = typed(value, LogFieldKind.HASHED_BROWSER_PACKAGE)

        private fun typed(
            value: String,
            kind: LogFieldKind,
        ) = LogFieldKey(value, kind)
    }
}

internal enum class LogFieldKind(
    val identifierNamespace: IdentifierNamespace? = null,
) {
    COUNT,
    DURATION,
    RATIO,
    BOOLEAN,
    ENUM,
    ERROR_CODE,
    REASON,
    OPERATION,
    HASHED_TARGET_PACKAGE(IdentifierNamespace.TARGET_PACKAGE),
    HASHED_BROWSER_PACKAGE(IdentifierNamespace.BROWSER_PACKAGE),
    PREHASHED_IDENTIFIER,
}

@JvmInline
value class ErrorCode(
    val value: String,
) {
    init {
        require(CODE_PATTERN.matches(value)) { "Invalid error code" }
    }
}

@JvmInline
value class ReasonCode(
    val value: String,
) {
    init {
        require(CODE_PATTERN.matches(value)) { "Invalid reason code" }
    }
}

@JvmInline
value class OperationCode(
    val value: String,
) {
    init {
        require(OPERATION_PATTERN.matches(value)) { "Invalid operation code" }
    }
}

@JvmInline
value class EnumName(
    val value: String,
) {
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
        val DeviceLockSkipped = EventKey("application.device_lock.skipped")
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
        val LockFailed = EventKey("security.lock.failed")
        val LockCancelled = EventKey("security.lock.cancelled")
        val AuthenticationStarted = EventKey("security.authentication.started")
        val AuthenticationCompleted = EventKey("security.authentication.completed")
        val AuthenticationRejected = EventKey("security.authentication.rejected")
        val AuthenticationCancelled = EventKey("security.authentication.cancelled")
        val AuthenticationFailed = EventKey("security.authentication.failed")
        val BiometricPromptFailed = EventKey("security.biometric_prompt.failed")
        val BiometricRotationCleanupDeferred = EventKey("security.biometric_rotation.cleanup_deferred")
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
        val FieldsClassified = EventKey("autofill.fields_classified")
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
        val HostAbandoned = EventKey("autofill.host.abandoned")
        val RequestCancelled = EventKey("autofill.request.cancelled")
        val PresentationSelected = EventKey("autofill.presentation.selected")
        val FallbackStarted = EventKey("autofill.accessibility.started")
        val FallbackUnavailable = EventKey("autofill.accessibility.unavailable")
        val FallbackDelivered = EventKey("autofill.accessibility.delivered")
        val FallbackFailed = EventKey("autofill.accessibility.failed")

        object Legacy {
            val FillRequestReceived = EventKey("autofill.legacy.fill_request_received")
            val StructureParsed = EventKey("autofill.legacy.structure_parsed")
            val ResponseBuilt = EventKey("autofill.legacy.response_built")
            val SaveRequestReceived = EventKey("autofill.legacy.save_request_received")
        }
    }

    object Backup {
        val Error = EventKey("backup.error.reported")
        val ResourceCleanupFailed = EventKey("backup.resource_cleanup.failed")
        val Phase = EventKey("backup.operation.phase")
        val ExportStarted = EventKey("backup.export.started")
        val ExportCompleted = EventKey("backup.export.completed")
        val ImportStarted = EventKey("backup.import.started")
        val ImportCompleted = EventKey("backup.import.completed")
    }

    object Vault {
        val Error = EventKey("vault.error.reported")
        val EntryCreateStarted = EventKey("vault.entry.create.started")
        val EntryCreateCompleted = EventKey("vault.entry.create.completed")
        val EntryCreateFailed = EventKey("vault.entry.create.failed")
        val EntryUpdateStarted = EventKey("vault.entry.update.started")
        val EntryUpdateCompleted = EventKey("vault.entry.update.completed")
        val EntryUpdateFailed = EventKey("vault.entry.update.failed")
    }

    val All: Set<EventKey> =
        setOf(
            Application.Error,
            Application.PreviousProcessExit,
            Application.TraceResumeUnavailable,
            Application.BackgroundLockSkipped,
            Application.UnhandledException,
            Application.NativeLibraryLoadFailed,
            Application.PackageIconUnavailable,
            Application.ResourceCleanupFailed,
            Application.DeviceLockCompleted,
            Application.DeviceLockSkipped,
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
            Security.LockFailed,
            Security.LockCancelled,
            Security.AuthenticationStarted,
            Security.AuthenticationCompleted,
            Security.AuthenticationRejected,
            Security.AuthenticationCancelled,
            Security.AuthenticationFailed,
            Security.BiometricPromptFailed,
            Security.BiometricRotationCleanupDeferred,
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
            Autofill.Legacy.FillRequestReceived,
            Autofill.Legacy.StructureParsed,
            Autofill.FieldsClassified,
            Autofill.Legacy.ResponseBuilt,
            Autofill.Legacy.SaveRequestReceived,
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
            Autofill.HostAbandoned,
            Autofill.RequestCancelled,
            Autofill.PresentationSelected,
            Autofill.FallbackStarted,
            Autofill.FallbackUnavailable,
            Autofill.FallbackDelivered,
            Autofill.FallbackFailed,
            Backup.Error,
            Backup.ResourceCleanupFailed,
            Backup.Phase,
            Backup.ExportStarted,
            Backup.ExportCompleted,
            Backup.ImportStarted,
            Backup.ImportCompleted,
            Vault.Error,
            Vault.EntryCreateStarted,
            Vault.EntryCreateCompleted,
            Vault.EntryCreateFailed,
            Vault.EntryUpdateStarted,
            Vault.EntryUpdateCompleted,
            Vault.EntryUpdateFailed,
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
    val AutofillAccessibilityRequest = OperationCode("autofill_accessibility_request")
    val AutofillSessionClose = OperationCode("autofill_session_close")
    val DiagnosticsExport = OperationCode("diagnostics_export")
    val DatabaseOpen = OperationCode("database_open")
    val DatabaseReset = OperationCode("database_reset")
    val ResourceCleanup = OperationCode("resource_cleanup")
    val PackageIconLoad = OperationCode("package_icon_load")
    val BackupRestore = OperationCode("backup_restore")
    val BackupExport = OperationCode("backup_export")
    val BackupImport = OperationCode("backup_import")
    val VaultOtpImport = OperationCode("vault_otp_import")
    val Scanner = OperationCode("scanner")
    val VaultKeyManagement = OperationCode("vault_key_management")
    val AutofillInboxKeyProvision = OperationCode("autofill_inbox_key_provision")
    val VaultEntryCreate = OperationCode("vault_entry_create")
    val VaultEntryUpdate = OperationCode("vault_entry_update")
    val VaultQrExport = OperationCode("vault_qr_export")
}

object LogFieldKeys {
    val RetryCount = LogFieldKey.count("retry_count")
    val ElapsedMs = LogFieldKey.duration("elapsed_ms")
    val TargetPackage = LogFieldKey.targetPackage("target_package")
    val BrowserPackage = LogFieldKey.browserPackage("browser_package")
    val FromState = LogFieldKey.enum("from_state")
    val ToState = LogFieldKey.enum("to_state")
    val LockType = LogFieldKey.enum("lock_type")
    val Reason = LogFieldKey.reason("reason")
    val Owner = LogFieldKey.enum("owner")
    val TimeoutMs = LogFieldKey.duration("timeout_ms")
    val ActiveLeaseCount = LogFieldKey.count("active_lease_count")
    val LockReason = LogFieldKey.reason("lock_reason")
    val Trigger = LogFieldKey.enum("trigger")
    val Result = LogFieldKey.enum("result")
    val AuthenticationMethod = LogFieldKey.enum("authentication_method")
    val AuthenticationPurpose = LogFieldKey.enum("authentication_purpose")
    val ExitStatus = LogFieldKey.count("exit_status")
    val ExitReason = LogFieldKey.enum("exit_reason")
    val Importance = LogFieldKey.enum("importance")
    val PssKb = LogFieldKey.count("pss_kb")
    val RssKb = LogFieldKey.count("rss_kb")
    val Source = LogFieldKey.enum("source")
    val ErrorLayer = LogFieldKey.enum("error_layer")
    val Severity = LogFieldKey.enum("severity")
    val Recoverable = LogFieldKey.boolean("recoverable")
    val Operation = LogFieldKey.operation("operation")
    val ClearTask = LogFieldKey.boolean("clear_task")
    val DroppedDebugCount = LogFieldKey.count("dropped_debug_count")
    val DroppedInfoCount = LogFieldKey.count("dropped_info_count")
    val DroppedWarnCount = LogFieldKey.count("dropped_warn_count")
    val DroppedErrorCount = LogFieldKey.count("dropped_error_count")
    val DroppedFatalCount = LogFieldKey.count("dropped_fatal_count")
    val EntryType = LogFieldKey.enum("entry_type")
    val Presentation = LogFieldKey.enum("presentation")
    val InlineAvailable = LogFieldKey.boolean("inline_available")

    object Backup {
        val Stage = LogFieldKey.enum("stage")
        val FailureReason = LogFieldKey.reason("failure_reason")
        val EntryCount = LogFieldKey.count("entry_count")
        val ResourceCount = LogFieldKey.count("resource_count")
    }

    object Autofill {
        val FieldCount = LogFieldKey.count("editable_field_count")
        val UsernameRoleCount = LogFieldKey.count("username_role_count")
        val PasswordRoleCount = LogFieldKey.count("password_role_count")
        val OtpRoleCount = LogFieldKey.count("otp_role_count")
        val HasCredentials = LogFieldKey.boolean("has_credentials")
        val FailureKind = LogFieldKey.enum("failure_kind")

        object Legacy {
            val FillContextCount = LogFieldKey.count("fill_context_count")
            val RequestFlags = LogFieldKey.count("request_flags")
            val Stage = LogFieldKey.enum("stage")
            val WindowCount = LogFieldKey.count("window_count")
            val VisitedNodeCount = LogFieldKey.count("visited_node_count")
            val TraversalTruncated = LogFieldKey.boolean("traversal_truncated")
            val HintedFieldCount = LogFieldKey.count("hinted_field_count")
            val FocusedFieldCount = LogFieldKey.count("focused_field_count")
            val PasswordInputTypeFieldCount = LogFieldKey.count("password_input_type_field_count")
            val HasPackage = LogFieldKey.boolean("has_package")
            val HasWebDomain = LogFieldKey.boolean("has_web_domain")
            val ResponsePresent = LogFieldKey.boolean("response_present")
            val DatasetCount = LogFieldKey.count("dataset_count")
            val SaveInfoPresent = LogFieldKey.boolean("save_info_present")
            val InlineRequestAvailable = LogFieldKey.boolean("inline_request_available")
        }
    }
}
