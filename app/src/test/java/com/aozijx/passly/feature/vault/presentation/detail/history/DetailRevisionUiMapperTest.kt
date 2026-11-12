package com.aozijx.passly.feature.vault.presentation.detail.history

import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.history.EntryRevisionDifference
import com.aozijx.passly.domain.entry.model.history.EntryRevisionId
import com.aozijx.passly.domain.entry.model.history.EntryRevisionMetadata
import com.aozijx.passly.domain.entry.model.history.EntryRevisionSummary
import com.aozijx.passly.domain.entry.model.history.RevisionChange
import com.aozijx.passly.domain.entry.model.history.RevisionDifferenceKind
import com.aozijx.passly.domain.entry.model.history.RevisionFieldId
import com.aozijx.passly.domain.entry.model.history.RevisionFieldValue
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.vault.presentation.common.labelRes
import com.aozijx.passly.feature.vault.presentation.detail.DetailActivityHistoryPhase
import com.aozijx.passly.feature.vault.presentation.detail.DetailActivityHistoryState
import com.aozijx.passly.feature.vault.presentation.detail.DetailHistoryFilter
import com.aozijx.passly.feature.vault.presentation.detail.DetailRevisionRestorePhase
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHistoryAffectedFieldsUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHistoryDescriptionUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHistoryIcon
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHistoryItemUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHistoryListUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHistoryRevisionReference
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHistorySheetUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRevisionComparisonContentUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRevisionComparisonUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRevisionDifferenceContentUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRevisionFieldLabelUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRevisionRevealActionUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRevisionValueUiModel
import org.junit.Assert.assertEquals
import org.junit.Test

class DetailRevisionUiMapperTest {
    @Test
    fun `latest revision annotates its activity but is not rendered as an extra history row`() {
        val revisions =
            listOf(
                summary("latest", version = 3, changedFields = setOf(RevisionFieldId.Title)),
                summary("previous", version = 2, changedFields = setOf(RevisionFieldId.Username)),
            )

        val historyUi =
            listPage(
                revisions = revisions,
                activities = listOf(pendingActivity(version = 3), pendingActivity(version = 2, id = "activity:previous")),
            )

        val currentActivity = historyUi.items.first { it.id == "activity:update" }
        val previousActivity = historyUi.items.first { it.id == "activity:previous" }
        assertEquals(
            DetailHistoryAffectedFieldsUiState.Labels(
                listOf(DetailRevisionFieldLabelUiState.Resource(FieldKey.TITLE.labelRes())),
            ),
            currentActivity.affectedFields,
        )
        assertEquals(DetailHistoryRevisionReference.Unlinked, currentActivity.revision)
        assertEquals(
            DetailHistoryAffectedFieldsUiState.Labels(
                listOf(DetailRevisionFieldLabelUiState.Resource(FieldKey.USERNAME.labelRes())),
            ),
            previousActivity.affectedFields,
        )
        assertEquals(DetailHistoryRevisionReference.Linked(2, "previous"), previousActivity.revision)
        assertEquals(2, historyUi.items.size)
    }

    @Test
    fun `latest revision without a matching activity is omitted from standalone history rows`() {
        val revisions =
            listOf(
                summary("latest", version = 3, changedFields = emptySet()),
                summary("previous", version = 2, changedFields = emptySet()),
            )

        val historyUi = listPage(revisions, activities = emptyList())

        assertEquals(listOf("revision:previous"), historyUi.items.map { it.id })
    }

    @Test
    fun `unchanged fields map to explicit no changes content`() {
        val comparison =
            DetailRevisionState
                .Open(
                    entryId = EntryId("entry"),
                    revisions = DetailRevisionListState.Ready(emptyList()),
                    page =
                        DetailRevisionPage.Comparison(
                            revisionId = "revision",
                            phase =
                                DetailRevisionComparisonPhase.Ready(
                                    listOf(
                                        EntryRevisionDifference(
                                            field = RevisionFieldId.Title,
                                            kind = RevisionDifferenceKind.UNCHANGED,
                                            before = RevisionFieldValue.Text("same"),
                                            after = RevisionFieldValue.Text("same"),
                                        ),
                                    ),
                                    DetailRevisionRestorePhase.Idle,
                                ),
                        ),
                ).toHistorySheetUiModel(
                    filter = DetailHistoryFilter.ALL,
                    activityHistory =
                        DetailActivityHistoryState.Content(emptyList(), DetailActivityHistoryPhase.Ready),
                    activityItems = emptyList(),
                    reveals = emptyMap(),
                ) as DetailHistorySheetUiModel.ComparisonPage

        assertEquals(
            DetailRevisionComparisonContentUiState.NoChanges,
            (comparison.content as DetailRevisionComparisonUiState.Ready).content,
        )
    }

    @Test
    fun `sensitive differences map to explicit hidden and revealed content and reveal action`() {
        val difference =
            EntryRevisionDifference(
                field = RevisionFieldId.Sensitive(SensitiveFieldKey.PASSWORD),
                kind = RevisionDifferenceKind.CHANGED,
                before = null,
                after = null,
            )

        val hidden = differenceUiModel(difference)

        assertEquals(DetailRevisionDifferenceContentUiState.ProtectedHidden, hidden.content)
        assertEquals(
            DetailRevisionRevealActionUiState.Available(SensitiveFieldKey.PASSWORD),
            hidden.revealAction,
        )

        val revealed =
            differenceUiModel(
                difference,
                mapOf(SensitiveFieldKey.PASSWORD to TestSensitiveValue("old-password")),
            )

        assertEquals(
            "old-password",
            (revealed.content as DetailRevisionDifferenceContentUiState.ProtectedRevealed).value.useChars(::String),
        )
    }

    @Test
    fun `public differences retain before and after values without protected actions`() {
        val difference =
            EntryRevisionDifference(
                field = RevisionFieldId.Title,
                kind = RevisionDifferenceKind.CHANGED,
                before = RevisionFieldValue.Text("before"),
                after = RevisionFieldValue.Text("after"),
            )

        val actual = differenceUiModel(difference)

        assertEquals(
            DetailRevisionDifferenceContentUiState.Values(
                before = DetailRevisionValueUiModel.Text("before"),
                after = DetailRevisionValueUiModel.Text("after"),
            ),
            actual.content,
        )
        assertEquals(DetailRevisionRevealActionUiState.Unavailable, actual.revealAction)
    }

    @Test
    fun `protected custom field labels and content stay explicit without a reveal action`() {
        val actual =
            differenceUiModel(
                EntryRevisionDifference(
                    field = RevisionFieldId.ProtectedCustomField(position = 2, name = "Recovery code"),
                    kind = RevisionDifferenceKind.CHANGED,
                    before = null,
                    after = null,
                ),
            )

        assertEquals(
            DetailRevisionFieldLabelUiState.Named(R.string.revision_custom_field_label, "Recovery code"),
            actual.label,
        )
        assertEquals(DetailRevisionDifferenceContentUiState.ProtectedHidden, actual.content)
        assertEquals(DetailRevisionRevealActionUiState.Unavailable, actual.revealAction)
    }

    @Test
    fun `added sensitive differences do not offer a reveal action`() {
        val actual =
            differenceUiModel(
                EntryRevisionDifference(
                    field = RevisionFieldId.Sensitive(SensitiveFieldKey.IDENTITY_NUMBER),
                    kind = RevisionDifferenceKind.ADDED,
                    before = null,
                    after = null,
                ),
            )

        assertEquals(
            DetailRevisionFieldLabelUiState.Resource(FieldKey.ID_NUMBER.labelRes()),
            actual.label,
        )
        assertEquals(R.string.revision_change_added, actual.changeLabelRes)
        assertEquals(DetailRevisionRevealActionUiState.Unavailable, actual.revealAction)
    }

    private fun differenceUiModel(
        difference: EntryRevisionDifference,
        reveals: Map<SensitiveFieldKey, SensitiveValue> = emptyMap(),
    ) = (
        DetailRevisionState
            .Open(
                entryId = EntryId("entry"),
                revisions = DetailRevisionListState.Ready(emptyList()),
                page =
                    DetailRevisionPage.Comparison(
                        revisionId = "revision",
                        phase = DetailRevisionComparisonPhase.Ready(listOf(difference), DetailRevisionRestorePhase.Idle),
                    ),
            ).toHistorySheetUiModel(
                filter = DetailHistoryFilter.ALL,
                activityHistory =
                    DetailActivityHistoryState.Content(emptyList(), DetailActivityHistoryPhase.Ready),
                activityItems = emptyList(),
                reveals = reveals,
            ) as DetailHistorySheetUiModel.ComparisonPage
    ).content
        .let { it as DetailRevisionComparisonUiState.Ready }
        .content
        .let { it as DetailRevisionComparisonContentUiState.Changes }
        .differences
        .single()

    private fun listPage(
        revisions: List<EntryRevisionSummary>,
        activities: List<DetailHistoryItemUiModel>,
    ): DetailHistoryListUiState {
        val historyUi =
            DetailRevisionState
                .Open(
                    entryId = EntryId("entry"),
                    revisions = DetailRevisionListState.Ready(revisions),
                    page = DetailRevisionPage.List,
                ).toHistorySheetUiModel(
                    filter = DetailHistoryFilter.ALL,
                    activityHistory =
                        DetailActivityHistoryState.Content(emptyList(), DetailActivityHistoryPhase.Ready),
                    activityItems = activities,
                    reveals = emptyMap(),
                ) as DetailHistorySheetUiModel.ListPage
        return historyUi.content
    }

    private fun summary(
        id: String,
        version: Int,
        changedFields: Set<RevisionFieldId>,
    ): EntryRevisionSummary =
        EntryRevisionSummary(
            metadata =
                EntryRevisionMetadata(
                    id = EntryRevisionId(id),
                    entryId = EntryId("entry"),
                    version = EntryVersion(version),
                    createdAtMs = version.toLong(),
                    change = RevisionChange.VALUE_CHANGED,
                ),
            changedFields = changedFields,
        )

    private fun pendingActivity(
        version: Int,
        id: String = "activity:update",
    ) = DetailHistoryItemUiModel(
        id = id,
        description = DetailHistoryDescriptionUiState.Label(R.string.vault_detail_activity_update),
        icon = DetailHistoryIcon.EDITED,
        filter = DetailHistoryFilter.CONTENT_CHANGES,
        affectedFields = DetailHistoryAffectedFieldsUiState.None,
        createdAtMs = version.toLong(),
        revision = DetailHistoryRevisionReference.Pending(version),
    )

    private class TestSensitiveValue(
        private val value: String,
    ) : SensitiveValue {
        override val isEmpty: Boolean get() = value.isEmpty()

        override fun toCharArray(): CharArray = value.toCharArray()

        override fun wipe() = Unit
    }
}
