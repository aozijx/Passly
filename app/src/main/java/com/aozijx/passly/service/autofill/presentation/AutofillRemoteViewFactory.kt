package com.aozijx.passly.service.autofill.presentation

import android.content.Context
import android.widget.RemoteViews
import com.aozijx.passly.R
import com.aozijx.passly.domain.model.core.VaultEntry

object AutofillRemoteViewFactory {

    /**
     * 创建 SYSTEM_INLINE 模式下每条候选账号的展示行（显示在键盘候选条 / 下拉菜单中）。
     */
    fun createDatasetItem(
        context: Context,
        entry: VaultEntry,
        subtitle: String,
        badge: String
    ): RemoteViews {
        return RemoteViews(context.packageName, R.layout.autofill_dataset_item).apply {
            setTextViewText(R.id.item_title, entry.title)
            setTextViewText(R.id.item_subtitle, subtitle)
            setTextViewText(R.id.item_badge, badge)
        }
    }

    /**
     * 创建提示解锁的触发条。
     */
    fun createUnlockTrigger(context: Context): RemoteViews {
        return RemoteViews(context.packageName, R.layout.autofill_dataset_item).apply {
            setTextViewText(R.id.item_title, context.getString(R.string.vault_locked_title))
            setTextViewText(R.id.item_subtitle, context.getString(R.string.vault_locked_subtitle))
            setTextViewText(R.id.item_badge, context.getString(R.string.verify))
        }
    }

    /**
     * 创建 BOTTOM_SHEET 模式下唯一一条"踏板"入口。
     * 用户点击后由 AutofillAuthActivity 弹出半屏候选列表。
     *
     * @param candidateCount 找到的匹配账号数量，用于生成副标题文案
     */
    fun createBottomSheetTrigger(
        context: Context,
        candidateCount: Int
    ): RemoteViews {
        val subtitle = context.resources.getQuantityString(
            R.plurals.autofill_trigger_count,
            candidateCount,
            candidateCount
        )
        return RemoteViews(context.packageName, R.layout.autofill_bottom_sheet_trigger).apply {
            setTextViewText(R.id.trigger_subtitle, subtitle)
        }
    }

    fun createSaveDescription(
        context: Context,
        appLabel: String,
        iconBitmap: android.graphics.Bitmap
    ): RemoteViews {
        val saveTitle = context.getString(R.string.autofill_save_prompt_title, appLabel)
        val saveDescription = context.getString(R.string.autofill_save_prompt_description, appLabel)
        return RemoteViews(context.packageName, R.layout.autofill_save_description).apply {
            setImageViewBitmap(R.id.save_icon, iconBitmap)
            setTextViewText(R.id.save_title, saveTitle)
            setTextViewText(R.id.save_description, saveDescription)
        }
    }
}