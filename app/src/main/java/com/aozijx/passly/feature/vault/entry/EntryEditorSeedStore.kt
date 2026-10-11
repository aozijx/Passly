package com.aozijx.passly.feature.vault.entry

import com.aozijx.passly.feature.vault.wifi.WifiQrPayload
import java.util.UUID
import javax.inject.Inject

@JvmInline
value class EditorSeedToken(val value: String) {
    init {
        require(value.isNotBlank()) { "Editor seed token cannot be blank" }
    }

    companion object {
        fun new(): EditorSeedToken = EditorSeedToken(UUID.randomUUID().toString())

        fun parse(value: String): EditorSeedTokenParseResult =
            if (value.isBlank()) EditorSeedTokenParseResult.Invalid
            else EditorSeedTokenParseResult.Valid(EditorSeedToken(value))
    }
}

sealed interface EditorSeedTokenParseResult {
    data class Valid(val token: EditorSeedToken) : EditorSeedTokenParseResult
    data object Invalid : EditorSeedTokenParseResult
}

sealed interface EntryEditorSeed {
    data class Wifi(val payload: WifiQrPayload) : EntryEditorSeed
}

sealed interface EntryEditorSeedConsumeResult {
    data class Consumed(val seed: EntryEditorSeed) : EntryEditorSeedConsumeResult
    data object Missing : EntryEditorSeedConsumeResult
    data object AccessDenied : EntryEditorSeedConsumeResult
}

class EntryEditorSeedStore @Inject constructor() {
    private sealed interface Slot {
        data object Empty : Slot
        data class Stored(
            val token: EditorSeedToken,
            val seed: EntryEditorSeed,
        ) : Slot
    }

    private var slot: Slot = Slot.Empty

    @Synchronized
    fun stage(seed: EntryEditorSeed): EditorSeedToken {
        val token = EditorSeedToken.new()
        slot = Slot.Stored(token, seed)
        return token
    }

    @Synchronized
    fun consume(token: EditorSeedToken): EntryEditorSeedConsumeResult {
        val current = slot
        if (current !is Slot.Stored || current.token != token) {
            return EntryEditorSeedConsumeResult.Missing
        }
        slot = Slot.Empty
        return EntryEditorSeedConsumeResult.Consumed(current.seed)
    }

    @Synchronized
    fun clear() {
        slot = Slot.Empty
    }
}
