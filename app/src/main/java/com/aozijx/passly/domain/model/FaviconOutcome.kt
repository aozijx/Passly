package com.aozijx.passly.domain.model

enum class FaviconResult {
    SUCCESS,
    NETWORK_ERROR,
    DECODE_ERROR,
    SAVE_ERROR,
    EMPTY_INPUT
}

data class FaviconOutcome(
    val result: FaviconResult,
    val filePath: String? = null
)
