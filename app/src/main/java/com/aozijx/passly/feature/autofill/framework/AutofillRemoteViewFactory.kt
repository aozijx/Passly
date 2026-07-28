package com.aozijx.passly.feature.autofill.framework

import android.content.Context
import android.widget.RemoteViews
import com.aozijx.passly.R
import com.aozijx.passly.core.autofill.model.ResolvedCandidate

object AutofillRemoteViewFactory {

    fun createDatasetItem(
        context: Context,
        candidate: ResolvedCandidate,
        subtitle: String,
        badge: String
    ): RemoteViews {
        return RemoteViews(context.packageName, R.layout.autofill_dataset_item).apply {
            setTextViewText(R.id.item_title, candidate.displayName)
            setTextViewText(R.id.item_subtitle, subtitle)
            setTextViewText(R.id.item_badge, badge)
        }
    }

    fun createUnlockTrigger(context: Context): RemoteViews {
        return RemoteViews(context.packageName, R.layout.autofill_dataset_item).apply {
            setTextViewText(R.id.item_title, context.getString(R.string.vault_locked_title))
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
        return RemoteViews(context.packageName, R.layout.autofill_dataset_item).apply {
            setTextViewText(
                R.id.item_title,
                context.getString(
                    R.string.autofill_prompt_title,
                    context.getString(R.string.app_name)
                )
            )
            setTextViewText(R.id.item_subtitle, subtitle)
            setTextViewText(R.id.item_badge, context.getString(R.string.select))
        }
    }
}
