package com.aozijx.passly.data.model.payload.lookup

import kotlinx.serialization.Serializable

@Serializable
data class LookupPayload(
    val entryId: String,
    val searchText: String = ""
)