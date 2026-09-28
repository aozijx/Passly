package com.aozijx.passly.data.repository.entry

import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.model.SensitiveRevisionAccessAction
import com.aozijx.passly.domain.access.port.AuthorizationPermitVerifier
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.history.EntryRevisionId
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import javax.inject.Inject

internal class RevisionPermitConsumer @Inject constructor(
    private val verifier: AuthorizationPermitVerifier,
) {
    fun consumeReveal(
        permit: AuthorizationPermit,
        entryId: EntryId,
        revisionId: EntryRevisionId,
        keys: Set<SensitiveFieldKey>,
    ): Boolean = verifier.consume(
        permit,
        AuthorizationScope.SensitiveRevision(
            entryId = entryId,
            revisionId = revisionId.value,
            fieldKeys = keys,
            action = SensitiveRevisionAccessAction.REVEAL,
        ),
    )

    fun consumeRestore(
        permit: AuthorizationPermit,
        entryId: EntryId,
        revisionId: EntryRevisionId,
        keys: Set<SensitiveFieldKey>,
    ): Boolean = verifier.consume(
        permit,
        AuthorizationScope.SensitiveRevision(
            entryId = entryId,
            revisionId = revisionId.value,
            fieldKeys = keys,
            action = SensitiveRevisionAccessAction.RESTORE,
        ),
    )
}
