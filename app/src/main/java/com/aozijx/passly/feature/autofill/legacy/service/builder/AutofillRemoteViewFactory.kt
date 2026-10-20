package com.aozijx.passly.feature.autofill.legacy.service.builder

import android.content.Context
import android.widget.RemoteViews
import com.aozijx.passly.R

object AutofillRemoteViewFactory {

    fun render(
        context: Context,
        item: AutofillRemoteItem,
    ): RemoteViews = when (item) {
        is AutofillRemoteItem.SingleLine ->
            RemoteViews(context.packageName, R.layout.autofill_dataset_item_single).apply {
                setTextViewText(R.id.item_title, item.title)
            }

        is AutofillRemoteItem.DoubleLine ->
            RemoteViews(context.packageName, R.layout.autofill_dataset_item_double).apply {
                setTextViewText(R.id.item_title, item.title)
                setTextViewText(R.id.item_subtitle, item.subtitle)
            }
    }
}
