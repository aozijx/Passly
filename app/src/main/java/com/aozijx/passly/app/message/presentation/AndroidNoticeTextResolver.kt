package com.aozijx.passly.app.message.presentation

import android.content.Context
import com.aozijx.passly.R
import com.aozijx.passly.app.message.model.AppNotice
import com.aozijx.passly.app.message.model.NoticeCode
import com.aozijx.passly.app.message.model.ArgumentKey
import com.aozijx.passly.app.message.model.ArgumentValue
import com.aozijx.passly.app.message.contract.NoticeCodeRegistry
import com.aozijx.passly.app.message.contract.NoticeTextResolver
import com.aozijx.passly.app.message.contract.ResolvedNotice
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidNoticeTextResolver @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val codeRegistry: NoticeCodeRegistry
) : NoticeTextResolver {
    override fun resolve(notice: AppNotice): ResolvedNotice {
        val policy = codeRegistry.policyFor(notice.code)
        return ResolvedNotice(
            eventId = notice.eventId,
            title = context.getString(R.string.app_name),
            text = notice.resolveText(),
            level = policy.level
        )
    }

    private fun AppNotice.resolveText(): String {
        if (code != NoticeCode.BACKUP_IMPORT_COMPLETED || arguments.isEmpty()) {
            return context.getString(code.messageResource())
        }
        return context.getString(
            R.string.backup_import_success_summary,
            count(ArgumentKey.IMPORTED_ITEM_COUNT),
            count(ArgumentKey.EXISTING_ITEM_COUNT),
            count(ArgumentKey.SKIPPED_ITEM_COUNT),
            count(ArgumentKey.IGNORED_FIELD_COUNT),
            count(ArgumentKey.PRUNED_LINK_COUNT),
            count(ArgumentKey.PRUNED_RESOURCE_COUNT),
        )
    }

    private fun AppNotice.count(key: ArgumentKey): Long =
        (arguments[key] as? ArgumentValue.Count)?.value ?: 0L

    private fun NoticeCode.messageResource(): Int = when (this) {
        NoticeCode.CLIPBOARD_CLEARED -> R.string.notice_clipboard_cleared
        NoticeCode.CLIPBOARD_CLEAR_FAILED -> R.string.notice_clipboard_clear_failed
        NoticeCode.APP_LOCKED -> R.string.notice_app_locked
        NoticeCode.APP_CLOSE_REMINDER -> R.string.notice_app_close_reminder
        NoticeCode.BACKUP_EXPORT_COMPLETED -> R.string.backup_export_success
        NoticeCode.BACKUP_EXPORT_FAILED -> R.string.backup_error_unknown
        NoticeCode.BACKUP_IMPORT_COMPLETED -> R.string.backup_import_success
        NoticeCode.BACKUP_IMPORT_FAILED -> R.string.backup_error_unknown
        NoticeCode.BACKUP_DIRECTORY_CHECK_COMPLETED ->
            R.string.backup_directory_permission_ok

        NoticeCode.BACKUP_DIRECTORY_CHECK_FAILED ->
            R.string.backup_directory_permission_failed
        NoticeCode.SECURITY_KEY_INVALIDATED -> R.string.notice_security_key_invalidated
        NoticeCode.SECURITY_RECOVERY_REQUIRED -> R.string.notice_security_recovery_required
        NoticeCode.SECURITY_ACTION_FAILED -> R.string.notice_security_action_failed
        NoticeCode.DATABASE_INDEX_REBUILD_COMPLETED ->
            R.string.notice_database_index_rebuild_completed
        NoticeCode.DATABASE_INDEX_REBUILD_FAILED ->
            R.string.notice_database_index_rebuild_failed
        NoticeCode.DATABASE_OPERATION_FAILED -> R.string.notice_database_operation_failed
        NoticeCode.NOTIFICATION_PERMISSION_DENIED ->
            R.string.notification_permission_denied_message
    }
}
