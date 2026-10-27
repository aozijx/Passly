package com.aozijx.passly.core.ui.autofill

import android.app.Activity
import android.view.View
import android.view.autofill.AutofillManager
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentDataType
import androidx.compose.ui.semantics.contentDataType
import androidx.compose.ui.semantics.semantics

/** Excludes an editable Compose node without clearing its accessibility or editing semantics. */
fun Modifier.excludeFromAutofill(): Modifier = semantics {
    contentDataType = ContentDataType.None
}

/** Excludes this app's native view subtree and cancels only this Activity's fill/save session. */
fun Activity.excludeFromAutofill() {
    window.decorView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
    getSystemService(AutofillManager::class.java)?.cancel()
}
