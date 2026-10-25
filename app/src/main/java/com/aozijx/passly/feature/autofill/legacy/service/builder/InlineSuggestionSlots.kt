package com.aozijx.passly.feature.autofill.legacy.service.builder

internal object InlineSuggestionSlots {
    fun candidateCount(candidates: Int, maxSuggestions: Int, specCount: Int): Int =
        if (specCount == 0) 0 else candidates.coerceAtMost(maxSuggestions).coerceAtLeast(0)

    fun specIndex(suggestionIndex: Int, specCount: Int): Int {
        require(suggestionIndex >= 0 && specCount > 0)
        return suggestionIndex.coerceAtMost(specCount - 1)
    }
}
