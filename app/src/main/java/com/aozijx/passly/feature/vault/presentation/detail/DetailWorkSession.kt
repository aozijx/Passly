package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.policy.EntryEdit
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.domain.sensitive.SensitiveValue
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

/** Owns the identity of one detail visit and publishes completed edits to that visit. */
internal class DetailWorkSession(
    private val hasAccess: () -> Boolean,
    private val editEntry: suspend (EntryId, EntryEdit) -> AppResult<Entry>,
    private val present: suspend (Entry) -> DetailEntryPresentation,
    private val onMutation: (DetailMutation) -> Unit,
    private val onReveal: (FieldKey, SensitiveValue) -> Unit,
) {
    sealed interface Scope {
        data object Closed : Scope
        class Active internal constructor(val entryId: EntryId) : Scope
    }

    private var scope: Scope = Scope.Closed

    fun open(entryId: EntryId): Scope.Active = Scope.Active(entryId).also { scope = it }

    fun close() {
        scope = Scope.Closed
    }

    fun current(entryId: EntryId): Scope.Active? =
        (scope as? Scope.Active)?.takeIf { it.entryId == entryId }

    fun isCurrent(token: Scope.Active): Boolean = scope === token && hasAccess()

    suspend fun <K> publishReveals(
        token: Scope.Active,
        values: List<Pair<K, SensitiveValue>>,
        publish: (K, SensitiveValue) -> Unit,
    ) {
        try {
            for ((key, value) in values) {
                if (isCurrent(token) && currentCoroutineContext().isActive) publish(key, value) else value.wipe()
            }
        } catch (failure: Throwable) {
            values.forEach { it.second.wipe() }
            throw failure
        }
    }

    suspend fun persist(token: Scope.Active, edit: EntryEdit, completion: DetailEditCompletion) {
        if (!isCurrent(token) || !currentCoroutineContext().isActive) return
        val result = editEntry(token.entryId, edit)
        if (!isCurrent(token) || !currentCoroutineContext().isActive) return
        when (result) {
            is AppResult.Success -> {
                val presentation = present(result.data)
                if (!isCurrent(token) || !currentCoroutineContext().isActive) return
                onMutation(DetailMutation.EntryPresented(presentation, completion != DetailEditCompletion.Title))
                if (!isCurrent(token) || !currentCoroutineContext().isActive) return
                if (completion is DetailEditCompletion.Field && edit is EntryEdit.SetFieldValue) {
                    onReveal(edit.fieldKey, OwnedChars.fromString(edit.value))
                }
                if (isCurrent(token) && currentCoroutineContext().isActive) onMutation(DetailMutation.SaveSucceeded(completion))
            }
            is AppResult.Failure -> onMutation(DetailMutation.SaveFailed(completion, result.error.code))
        }
    }
}
