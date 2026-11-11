package passly

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SourceBoundaryReferenceScannerTest {
    @Test
    fun presentationSharedRejectsFullyQualifiedDomainReferencesWithoutImports() {
        val source =
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/presentation/shared/components/Card.kt",
                content = "val entry: com.aozijx.passly.domain.entry.model.Entry? = null",
            )

        val violation =
            SourceBoundaryVerifier
                .verify(
                    listOf(source),
                    SourceBoundaryPolicy.layerRules,
                ).single()

        assertEquals("LAYER_PRESENTATION_SHARED", violation.ruleId)
        assertEquals(source.content, violation.evidence)
    }

    @Test
    fun fullyQualifiedReferencesHonorExactAllowlistAndIgnoreCommentsAndStrings() {
        val allowed =
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/presentation/shared/components/Card.kt",
                content =
                    """
                    // com.aozijx.passly.domain.entry.model.Entry
                    val documentation = "com.aozijx.passly.domain.entry.model.Entry"
                    /* com.aozijx.passly.domain.entry.model.Entry */
                    val type: com.aozijx.passly.domain.entry.model.EntryType =
                        com.aozijx.passly.domain.entry.model.EntryType.LOGIN
                    """.trimIndent(),
            )
        val sibling =
            allowed.copy(
                content = "val metadata: com.aozijx.passly.domain.entry.model.EntryTypeMetadata",
            )

        assertEquals(
            emptyList(),
            SourceBoundaryVerifier.verify(listOf(allowed), SourceBoundaryPolicy.layerRules),
        )
        assertEquals(
            "LAYER_PRESENTATION_SHARED",
            SourceBoundaryVerifier.verify(listOf(sibling), SourceBoundaryPolicy.layerRules).single().ruleId,
        )
    }

    @Test
    fun fullyQualifiedReferencesInsideStringTemplatesAreScannedButTextAndEscapedDollarsAreIgnored() {
        val source =
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/presentation/ui/Template.kt",
                content =
                    listOf(
                        "val regular = \"\${com.aozijx.passly.domain.entry.model.Entry::class.simpleName}\"",
                        "val nested = \"\${run { val type = com.aozijx.passly.domain.entry.model.Entry::class; type.simpleName }}\"",
                        "val nestedTemplate = \"\${run { val label = \\\"\${com.aozijx.passly.domain.entry.model.Entry::class.simpleName}\\\"; label }}\"",
                        "val raw = \"\"\"\${com.aozijx.passly.domain.entry.model.Entry::class.simpleName}\"\"\"",
                        "val escaped = \"\\\$com.aozijx.passly.domain.entry.model.Entry\"",
                        "val text = \"com.aozijx.passly.domain.entry.model.Entry\"",
                        "// com.aozijx.passly.domain.entry.model.Entry",
                    ).joinToString("\n"),
            )

        val violations = SourceBoundaryVerifier.verify(listOf(source), SourceBoundaryPolicy.generalRules)

        assertEquals(4, violations.size)
        assertTrue(violations.all { it.ruleId == "LAYER_PRESENTATION_UI" })
        assertEquals(
            source.content
                .lineSequence()
                .take(4)
                .toList(),
            violations.map { it.evidence },
        )
    }

    @Test
    fun fullyQualifiedReferencesContinueAcrossLinesAndReportTheirStartingLine() {
        val source =
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/feature/sync_engine2/presentation/Route.kt",
                content = "val repository = com.aozijx.passly\n    .data.repository.EntryRepositoryImpl",
            )

        val violation = SourceBoundaryVerifier.verify(listOf(source), SourceBoundaryPolicy.layerRules).single()

        assertEquals("LAYER_FEATURE_PRESENTATION", violation.ruleId)
        assertEquals("val repository = com.aozijx.passly", violation.evidence)
    }

    @Test
    fun escapedImportSegmentsAreNormalizedBeforeBoundaryChecks() {
        val forbiddenImports =
            listOf(
                "com.aozijx.passly.`data`.repository.Repository",
                "com.aozijx.passly.`data`.repository.Repository as Store",
                "com.aozijx.passly.`data`.repository.*",
            )
        forbiddenImports.forEach { imported ->
            val violation =
                SourceBoundaryVerifier
                    .verify(
                        listOf(uiSource("import $imported")),
                        SourceBoundaryPolicy.generalRules,
                    ).single()

            assertEquals("LAYER_PRESENTATION_UI", violation.ruleId, imported)
        }

        val allowed = uiSource("import com.aozijx.passly.core.`ui`.theme.PasslyTheme")
        assertEquals(emptyList(), SourceBoundaryVerifier.verify(listOf(allowed), SourceBoundaryPolicy.generalRules))
    }

    @Test
    fun crossFeatureImportsRequireAnExactReviewedOwner() {
        assertTrue(
            SourceBoundaryPolicy.featureIntegrationAllowances.all { allowance ->
                allowance.sourcePathContains.isNotBlank() &&
                    allowance.targetFeature.isNotBlank() &&
                    allowance.allowedImportPrefixes.isNotEmpty() &&
                    allowance.owner.isNotBlank() &&
                    allowance.rationale.isNotBlank()
            },
        )
        val businessDependency =
            EditorSource(
                path = "feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/core/SaveAutofillCredential.kt",
                content = "import com.aozijx.passly.feature.vault.entry.CreateEntryUseCase",
            )
        val violation =
            SourceBoundaryVerifier
                .verify(
                    listOf(businessDependency),
                    SourceBoundaryPolicy.generalRules,
                ).single()
        assertEquals("FEATURE_CROSS_FEATURE", violation.ruleId)
        assertEquals("feature.autofill", violation.owner)

        val qualifiedBusinessDependency =
            businessDependency.copy(
                content = "val writer: com.aozijx.passly.feature.`vault`.entry.CreateEntryUseCase",
            )
        assertEquals(
            "FEATURE_CROSS_FEATURE",
            SourceBoundaryVerifier
                .verify(
                    listOf(qualifiedBusinessDependency),
                    SourceBoundaryPolicy.generalRules,
                ).single()
                .ruleId,
        )

        val reviewedRouteIntegration =
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/feature/vault/presentation/editor/otp/AddOtpEditorRoute.kt",
                content = "import com.aozijx.passly.feature.scanner.presentation.integration.ScannerOverlay",
            )
        assertEquals(
            emptyList(),
            SourceBoundaryVerifier.verify(listOf(reviewedRouteIntegration), SourceBoundaryPolicy.generalRules),
        )

        val unregisteredRouteIntegration =
            businessDependency.copy(
                content = "import com.aozijx.passly.feature.scanner.presentation.route.ScannerRoute",
            )
        assertEquals(
            "FEATURE_CROSS_FEATURE",
            SourceBoundaryVerifier
                .verify(
                    listOf(unregisteredRouteIntegration),
                    SourceBoundaryPolicy.generalRules,
                ).single { it.ruleId == "FEATURE_CROSS_FEATURE" }
                .ruleId,
        )

        val cardStyleSettingsIntegration =
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/feature/settings/presentation/ui/appearance/InterfaceScreen.kt",
                content = "import com.aozijx.passly.feature.vault.presentation.list.ui.component.cardstyle.CardStyleRegistry",
            )
        assertEquals(
            emptyList(),
            SourceBoundaryVerifier.verify(listOf(cardStyleSettingsIntegration), SourceBoundaryPolicy.generalRules),
        )
        val unrelatedVaultDependency =
            cardStyleSettingsIntegration.copy(
                content = "import com.aozijx.passly.feature.vault.presentation.detail.DetailRoute",
            )
        assertEquals(
            "FEATURE_CROSS_FEATURE",
            SourceBoundaryVerifier
                .verify(listOf(unrelatedVaultDependency), SourceBoundaryPolicy.generalRules)
                .single()
                .ruleId,
        )
    }
}
