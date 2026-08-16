package com.aozijx.passly.feature.autofill.framework.service.builder

import android.content.Context
import android.view.View
import android.widget.RemoteViews
import com.aozijx.passly.R
import com.aozijx.passly.core.autofill.model.ResolvedCandidate

object AutofillRemoteViewFactory {

    fun createDatasetItem(
        context: Context,
        candidate: ResolvedCandidate,
        badge: String
    ): RemoteViews {
        val subtitle = candidate.username.trim()
        return RemoteViews(context.packageName, R.layout.autofill_dataset_item).apply {
            setTextViewText(R.id.item_title, candidate.displayName)
            setTextViewText(R.id.item_subtitle, subtitle)
            setViewVisibility(
                R.id.item_subtitle,
                if (subtitle.isBlank()) View.GONE else View.VISIBLE
            )
            setTextViewText(R.id.item_badge, badge)
            setViewVisibility(
                R.id.item_badge,
                if (badge.isBlank()) View.GONE else View.VISIBLE
            )
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
