package passly

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiOwnershipPolicyTest {
    @Test
    fun presentationUiCannotDeclareAHiltModule() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/presentation/ui/settings/SettingsUiModule.kt",
            content = """
                import dagger.Module
                @Module
                abstract class SettingsUiModule
            """.trimIndent(),
        )

        assertEquals(
            "PRESENTATION_DI_MODULE",
            SourceBoundaryVerifier.verify(
                listOf(source),
                SourceBoundaryPolicy.generalRules,
            ).single().ruleId,
        )
    }

    @Test
    fun presentationCannotBindADataImplementation() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/presentation/feature/vault/VaultBindingModule.kt",
            content = """
                import com.aozijx.passly.data.repository.entry.RoomEntryQueryRepository
                import dagger.Binds
                import dagger.Module
                @Module
                abstract class VaultBindingModule {
                    @Binds abstract fun bind(impl: RoomEntryQueryRepository): EntryQueryRepository
                }
            """.trimIndent(),
        )

        assertTrue(
            SourceBoundaryVerifier.verify(
                listOf(source),
                SourceBoundaryPolicy.generalRules,
            ).any { it.ruleId == "PRESENTATION_DI_MODULE" },
        )
    }

    @Test
    fun appAdapterMayBindItsImplementationToAFeatureContract() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/app/database/recovery/DatabaseRecoveryAdapterModule.kt",
            content = """
                import com.aozijx.passly.feature.database.recovery.DatabaseRecoveryGateway
                import dagger.Binds
                import dagger.Module
                @Module
                abstract class DatabaseRecoveryAdapterModule {
                    @Binds
                    abstract fun bind(impl: DataDatabaseRecoveryGateway): DatabaseRecoveryGateway
                }
            """.trimIndent(),
        )

        assertEquals(
            emptyList(),
            SourceBoundaryVerifier.verify(listOf(source), SourceBoundaryPolicy.generalRules),
        )
    }
}
