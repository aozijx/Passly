package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import org.junit.Assert.assertEquals
import org.junit.Test

class RelatedPasswordUiMapperTest {
    @Test
    fun candidateUsernameProjectsToExplicitVisibilityState() {
        assertEquals(
            RelatedPasswordCandidateUsernameUiState.Hidden,
            candidate(" \t").toCandidateUiModel().username,
        )
        assertEquals(
            RelatedPasswordCandidateUsernameUiState.Visible("person@example.test"),
            candidate("person@example.test").toCandidateUiModel().username,
        )
    }

    private fun candidate(username: String) =
        EntryListItem(
            identity =
                EntryIdentity(
                    id = EntryId("candidate"),
                    type = EntryType.LOGIN,
                    timestamps = EntryTimestamps(1L),
                ),
            profile = EntryProfile(title = "Candidate", username = username),
        )
}
