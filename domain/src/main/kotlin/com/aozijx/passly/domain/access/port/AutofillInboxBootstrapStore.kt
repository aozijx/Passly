package com.aozijx.passly.domain.access.port

import com.aozijx.passly.domain.access.model.AutofillInboxKeyMaterial

interface AutofillInboxBootstrapStore {
    suspend fun saveAutofillInboxKeyMaterial(material: AutofillInboxKeyMaterial)

    suspend fun loadAutofillInboxKeyMaterial(): AutofillInboxKeyMaterial?

    suspend fun clearAutofillInboxKeyMaterial()
}
