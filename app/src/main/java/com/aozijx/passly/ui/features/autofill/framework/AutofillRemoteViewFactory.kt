package com.aozijx.passly.ui.features.autofill.framework

import android.content.Context
import android.graphics.Bitmap
import android.widget.RemoteViews
import com.aozijx.passly.R
import com.aozijx.passly.domain.model.VaultEntry

object AutofillRemoteViewFactory {

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

    fun createUnlockTrigger(context: Context): RemoteViews {
        return RemoteViews(context.packageName, R.layout.autofill_dataset_item).apply {
            setTextViewText(R.id.item_title, context.getString(R.string.vault_locked_title))
            setTextViewText(R.id.item_subtitle, context.getString(R.string.vault_locked_subtitle))
            setTextViewText(R.id.item_badge, context.getString(R.string.verify))
        }
    }

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
        iconBitmap: Bitmap
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