package com.aozijx.passly.feature.autofill.attack

import android.os.Bundle
import android.service.autofill.AutofillService
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import com.aozijx.passly.feature.autofill.legacy.service.parser.AutofillStructureParser

class CaptureAutofillService : AutofillService() {
    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: android.os.CancellationSignal,
        callback: FillCallback,
    ) {
        val parsed = AutofillStructureParser.parse(request.fillContexts)
        getSharedPreferences(CAPTURE_PREFERENCES, MODE_PRIVATE)
            .edit()
            .putBoolean(CAPTURE_RECEIVED, true)
            .putString(CAPTURE_PACKAGE, parsed.packageName)
            .putString(CAPTURE_DOMAIN, parsed.webDomain)
            .putBoolean(CAPTURE_AMBIGUOUS, parsed.ambiguousWebOrigin)
            .putInt(CAPTURE_FIELD_COUNT, parsed.editableFields.size)
            .commit()
        callback.onSuccess(FillResponse.Builder().setClientState(Bundle()).build())
    }

    override fun onSaveRequest(
        request: SaveRequest,
        callback: SaveCallback,
    ) {
        callback.onSuccess()
    }
}
