package com.aozijx.passly.service.autofill.presentation

import android.content.Context
import android.widget.RemoteViews
import com.aozijx.passly.R
import com.aozijx.passly.core.platform.PackageUtils
import com.aozijx.passly.domain.model.VaultEntry

object AutofillRemoteViewFactory {
    fun createDatasetItem(
        context: Context,
        entry: VaultEntry,
        subtitle: String,
        badge: String
    ): RemoteViews {
        return RemoteViews(context.packageName, R.layout.autofill_dataset_item).apply {
            setTextViewText(R.id.title, entry.title)
            setTextViewText(R.id.username, subtitle)
            setTextViewText(R.id.badge, badge)
            entry.associatedAppPackage?.let { appPkg ->
                PackageUtils.getAppIconDrawable(context, appPkg)?.let { icon ->
                    setImageViewBitmap(R.id.icon, PackageUtils.drawableToBitmap(icon))
                }
            }
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

