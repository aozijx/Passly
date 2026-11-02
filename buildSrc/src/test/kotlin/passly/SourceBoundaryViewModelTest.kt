package passly

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SourceBoundaryViewModelTest {
    @Test
    fun `view models cannot inject or expose another view model in type signatures`() {
        val sources =
            listOf(
                source(
                    path = "app/src/main/java/other/OtherViewModel.kt",
                    content =
                        """
                        package other
                        @HiltViewModel
                        class OtherViewModel : ViewModel()
                        """.trimIndent(),
                ),
                source(
                    path = "app/src/main/java/source/SourceViewModel.kt",
                    content =
                        """
                        package source
                        import other.OtherViewModel
                        @HiltViewModel
                        class SourceViewModel(
                            private val dependency: OtherViewModel,
                        ) : ViewModel() {
                            val cached: Map<String, List<OtherViewModel?>>? = null
                            fun accept(value: OtherViewModel) = Unit
                            fun returnValue(): List<OtherViewModel> = emptyList()
                        }
                        """.trimIndent(),
                ),
            )

        val violations = SourceBoundaryVerifier.verify(sources, SourceBoundaryPolicy.generalRules)

        assertEquals(4, violations.count { it.ruleId == "VIEW_MODEL_DEPENDENCY" })
        assertTrue(violations.all { it.path.endsWith("SourceViewModel.kt") })
    }

    @Test
    fun `view models cannot reference another view model through same package alias fqn or template code`() {
        val sources =
            listOf(
                source(
                    path = "app/src/main/java/sample/OtherViewModel.kt",
                    content =
                        """
                        package sample
                        class OtherViewModel : androidx.lifecycle.ViewModel()
                        """.trimIndent(),
                ),
                source(
                    path = "app/src/main/java/sample/SourceViewModel.kt",
                    content =
                        """
                        package sample
                        import sample.OtherViewModel as ImportedViewModel
                        @HiltViewModel
                        class SourceViewModel : ViewModel() {
                            val samePackage: OtherViewModel? = null
                            val alias: ImportedViewModel? = null
                            val aliasCall = ImportedViewModel.create()
                            val qualified: sample.OtherViewModel? = null
                            val template = "${'$'}{sample.OtherViewModel::class}"
                            val rawText = "sample.OtherViewModel"
                            val rawStringText = ${"\"\"\""}sample.OtherViewModel${"\"\"\""}
                            val nestedTemplate = "${'$'}{run { "${'$'}{sample.OtherViewModel::class}" }}"
                            val escaped = "\${'$'}OtherViewModel"
                            // OtherViewModel is only a comment here.
                        }
                        """.trimIndent(),
                ),
            )

        val violations = SourceBoundaryVerifier.verify(sources, SourceBoundaryPolicy.generalRules)

        assertEquals(6, violations.count { it.ruleId == "VIEW_MODEL_DEPENDENCY" })
    }

    @Test
    fun `wildcard lifecycle import and aliased superclass identify view model declarations`() {
        val sources =
            listOf(
                source(
                    path = "app/src/main/java/sample/OtherViewModel.kt",
                    content =
                        """
                        package sample
                        import androidx.lifecycle.*
                        class OtherViewModel : ViewModel()
                        """.trimIndent(),
                ),
                source(
                    path = "app/src/main/java/sample/SourceViewModel.kt",
                    content =
                        """
                        package sample
                        import androidx.lifecycle.ViewModel as LifecycleModel
                        class SourceViewModel(private val other: OtherViewModel) : LifecycleModel() {
                            fun read(): OtherViewModel = other
                        }
                        """.trimIndent(),
                ),
            )

        val violations = SourceBoundaryVerifier.verify(sources, SourceBoundaryPolicy.generalRules)

        assertEquals(2, violations.count { it.ruleId == "VIEW_MODEL_DEPENDENCY" })
    }

    @Test
    fun `route activity and view models using ordinary types remain allowed`() {
        val sources =
            listOf(
                source(
                    path = "app/src/main/java/sample/OtherViewModel.kt",
                    content =
                        """
                        package sample
                        @HiltViewModel
                        class OtherViewModel : ViewModel()
                        class NotAViewModel
                        """.trimIndent(),
                ),
                source(
                    path = "app/src/main/java/sample/SourceViewModel.kt",
                    content =
                        """
                        package sample
                        @HiltViewModel
                        class SourceViewModel : ViewModel() {
                            val ordinary: NotAViewModel? = null
                            val message = "OtherViewModel"
                        }
                        """.trimIndent(),
                ),
                source(
                    path = "app/src/main/java/sample/SampleRoute.kt",
                    content =
                        """
                        package sample
                        class SampleRoute(val first: SourceViewModel, val second: OtherViewModel)
                        """.trimIndent(),
                ),
                source(
                    path = "app/src/main/java/sample/SampleActivity.kt",
                    content =
                        """
                        package sample
                        class SampleActivity(val viewModel: OtherViewModel)
                        """.trimIndent(),
                ),
            )

        val violations = SourceBoundaryVerifier.verify(sources, SourceBoundaryPolicy.generalRules)
        assertTrue(violations.none { it.ruleId == "VIEW_MODEL_DEPENDENCY" }, violations.toString())
    }

    @Test
    fun `imports and references outside view model declaration ranges are allowed`() {
        val sources =
            listOf(
                source(
                    path = "app/src/main/java/sample/OtherViewModel.kt",
                    content =
                        """
                        package sample
                        class OtherViewModel : androidx.lifecycle.ViewModel()
                        """.trimIndent(),
                ),
                source(
                    path = "app/src/main/java/sample/SourceViewModel.kt",
                    content =
                        """
                        package sample
                        import sample.OtherViewModel as Other
                        class SourceViewModel : androidx.lifecycle.ViewModel() {
                            fun harmless() = Unit
                        }
                        class Helper(val dependency: Other)
                        """.trimIndent(),
                ),
            )

        val violations = SourceBoundaryVerifier.verify(sources, SourceBoundaryPolicy.generalRules)
        assertTrue(violations.none { it.ruleId == "VIEW_MODEL_DEPENDENCY" }, violations.toString())
    }

    @Test
    fun `local imported and fully qualified typealiases to view models are forbidden`() {
        val sources =
            listOf(
                source(
                    path = "app/src/main/java/target/TargetViewModel.kt",
                    content =
                        """
                        package target
                        class TargetViewModel : androidx.lifecycle.ViewModel()
                        typealias LocalAlias = TargetViewModel
                        """.trimIndent(),
                ),
                source(
                    path = "app/src/main/java/aliases/ImportedAlias.kt",
                    content =
                        """
                        package aliases
                        import target.TargetViewModel as ImportedTarget
                        typealias ImportedAlias = ImportedTarget
                        typealias ChainedAlias = ImportedAlias
                        """.trimIndent(),
                ),
                source(
                    path = "app/src/main/java/aliases/QualifiedAlias.kt",
                    content =
                        """
                        package aliases
                        typealias QualifiedAlias = target.TargetViewModel
                        """.trimIndent(),
                ),
                source(
                    path = "app/src/main/java/source/SourceViewModel.kt",
                    content =
                        """
                        package source
                        import target.LocalAlias
                        import aliases.ImportedAlias
                        @HiltViewModel
                        class SourceViewModel : androidx.lifecycle.ViewModel() {
                            val local: LocalAlias? = null
                            val imported: ImportedAlias? = null
                            val qualified: aliases.QualifiedAlias? = null
                            val chained: aliases.ChainedAlias? = null
                        }
                        """.trimIndent(),
                ),
            )

        val violations = SourceBoundaryVerifier.verify(sources, SourceBoundaryPolicy.generalRules)

        assertEquals(4, violations.count { it.ruleId == "VIEW_MODEL_DEPENDENCY" })
    }

    @Test
    fun `transitive custom view model bases identify referenced targets`() {
        val sources =
            listOf(
                source(
                    path = "app/src/main/java/target/TargetViewModel.kt",
                    content =
                        """
                        package target
                        import androidx.lifecycle.ViewModel
                        abstract class SharedViewModelBase : ViewModel()
                        class TargetViewModel : SharedViewModelBase()
                        """.trimIndent(),
                ),
                source(
                    path = "app/src/main/java/source/SourceViewModel.kt",
                    content =
                        """
                        package source
                        import androidx.lifecycle.ViewModel
                        import target.TargetViewModel
                        @HiltViewModel
                        class SourceViewModel(private val target: TargetViewModel) : ViewModel()
                        """.trimIndent(),
                ),
            )

        val violations = SourceBoundaryVerifier.verify(sources, SourceBoundaryPolicy.generalRules)

        assertEquals(1, violations.count { it.ruleId == "VIEW_MODEL_DEPENDENCY" })
    }

    @Test
    fun `same file view models cannot reference each other`() {
        val sources =
            listOf(
                source(
                    path = "app/src/main/java/sample/ViewModels.kt",
                    content =
                        """
                        package sample
                        class FirstViewModel : androidx.lifecycle.ViewModel() {
                            val second: SecondViewModel? = null
                        }
                        class SecondViewModel : androidx.lifecycle.ViewModel()
                        """.trimIndent(),
                ),
            )

        val violations = SourceBoundaryVerifier.verify(sources, SourceBoundaryPolicy.generalRules)

        assertEquals(1, violations.count { it.ruleId == "VIEW_MODEL_DEPENDENCY" })
    }

    @Test
    fun `generic arguments in a superclass are not treated as superclasses`() {
        val sources =
            listOf(
                source(
                    path = "app/src/main/java/sample/ViewModels.kt",
                    content =
                        """
                        package sample
                        abstract class Holder<T>
                        class OrdinaryType : Holder<PeerViewModel>()
                        """.trimIndent(),
                ),
                source(
                    path = "app/src/main/java/sample/PeerViewModel.kt",
                    content =
                        """
                        package sample
                        class PeerViewModel : androidx.lifecycle.ViewModel()
                        """.trimIndent(),
                ),
                source(
                    path = "app/src/main/java/sample/SourceViewModel.kt",
                    content =
                        """
                        package sample
                        class SourceViewModel : androidx.lifecycle.ViewModel() {
                            val ordinary: OrdinaryType? = null
                        }
                        """.trimIndent(),
                ),
            )

        val violations = SourceBoundaryVerifier.verify(sources, SourceBoundaryPolicy.generalRules)

        assertTrue(violations.none { it.ruleId == "VIEW_MODEL_DEPENDENCY" }, violations.toString())
    }

    @Test
    fun `typealias to the AndroidX root identifies derived view models`() {
        val sources =
            listOf(
                source(
                    path = "app/src/main/java/sample/ViewModels.kt",
                    content =
                        """
                        package sample
                        typealias LifecycleBase = androidx.lifecycle.ViewModel
                        class DerivedViewModel : LifecycleBase()
                        """.trimIndent(),
                ),
                source(
                    path = "app/src/main/java/sample/SourceViewModel.kt",
                    content =
                        """
                        package sample
                        class SourceViewModel : androidx.lifecycle.ViewModel() {
                            val target: DerivedViewModel? = null
                        }
                        """.trimIndent(),
                ),
            )

        val violations = SourceBoundaryVerifier.verify(sources, SourceBoundaryPolicy.generalRules)

        assertEquals(1, violations.count { it.ruleId == "VIEW_MODEL_DEPENDENCY" })
    }

    @Test
    fun `typealias to the AndroidX root is forbidden in a view model signature`() {
        val sources =
            listOf(
                source(
                    path = "app/src/main/java/sample/SourceViewModel.kt",
                    content =
                        """
                        package sample
                        typealias LifecycleBase = androidx.lifecycle.ViewModel
                        class SourceViewModel : androidx.lifecycle.ViewModel() {
                            val base: LifecycleBase? = null
                        }
                        """.trimIndent(),
                ),
            )

        val violations = SourceBoundaryVerifier.verify(sources, SourceBoundaryPolicy.generalRules)

        assertEquals(1, violations.count { it.ruleId == "VIEW_MODEL_DEPENDENCY" })
    }

    @Test
    fun `fully qualified member calls resolve the longest known type prefix`() {
        val sources =
            listOf(
                source(
                    path = "app/src/main/java/p/Peer.kt",
                    content =
                        """
                        package p
                        class Peer : androidx.lifecycle.ViewModel() {
                            companion object { fun create() = Peer() }
                        }
                        """.trimIndent(),
                ),
                source(
                    path = "app/src/main/java/q/OtherPeer.kt",
                    content =
                        """
                        package q
                        class Peer { companion object { fun create() = Peer() } }
                        """.trimIndent(),
                ),
                source(
                    path = "app/src/main/java/p/SourceViewModel.kt",
                    content =
                        """
                        package p
                        class SourceViewModel : androidx.lifecycle.ViewModel() {
                            val forbidden = p.Peer.create()
                            val ordinary = q.Peer.create()
                        }
                        """.trimIndent(),
                ),
            )

        val violations = SourceBoundaryVerifier.verify(sources, SourceBoundaryPolicy.generalRules)

        assertEquals(1, violations.count { it.ruleId == "VIEW_MODEL_DEPENDENCY" })
    }

    @Test
    fun `bodyless view model does not absorb the following top level function`() {
        val sources =
            listOf(
                source(
                    path = "app/src/main/java/sample/OtherViewModel.kt",
                    content =
                        """
                        package sample
                        class OtherViewModel : androidx.lifecycle.ViewModel()
                        """.trimIndent(),
                ),
                source(
                    path = "app/src/main/java/sample/BodylessViewModel.kt",
                    content =
                        """
                        package sample
                        class BodylessViewModel : androidx.lifecycle.ViewModel()
                        fun helper(): OtherViewModel {
                            return OtherViewModel()
                        }
                        """.trimIndent(),
                ),
            )

        val violations = SourceBoundaryVerifier.verify(sources, SourceBoundaryPolicy.generalRules)

        assertTrue(violations.none { it.ruleId == "VIEW_MODEL_DEPENDENCY" }, violations.toString())
    }

    @Test
    fun `multiline superclass declaration still identifies a view model`() {
        val sources =
            listOf(
                source(
                    path = "app/src/main/java/sample/ViewModels.kt",
                    content =
                        """
                        package sample
                        class PeerViewModel : androidx.lifecycle.ViewModel()
                        """.trimIndent(),
                ),
                source(
                    path = "app/src/main/java/sample/SourceViewModel.kt",
                    content =
                        """
                        package sample
                        class SourceViewModel(
                            private val peer: PeerViewModel,
                        )
                            : androidx.lifecycle.ViewModel() {
                        }
                        """.trimIndent(),
                ),
            )

        val violations = SourceBoundaryVerifier.verify(sources, SourceBoundaryPolicy.generalRules)

        assertEquals(1, violations.count { it.ruleId == "VIEW_MODEL_DEPENDENCY" })
    }

    @Test
    fun `typealias whose generic right side contains a view model is forbidden`() {
        val sources =
            listOf(
                source(
                    path = "app/src/main/java/sample/ViewModels.kt",
                    content =
                        """
                        package sample
                        class PeerViewModel : androidx.lifecycle.ViewModel()
                        """.trimIndent(),
                ),
                source(
                    path = "app/src/main/java/sample/SourceViewModel.kt",
                    content =
                        """
                        package sample
                        typealias Peers = List<PeerViewModel>
                        class SourceViewModel : androidx.lifecycle.ViewModel() {
                            val peers: Peers = emptyList()
                        }
                        """.trimIndent(),
                ),
            )

        val violations = SourceBoundaryVerifier.verify(sources, SourceBoundaryPolicy.generalRules)

        assertEquals(1, violations.count { it.ruleId == "VIEW_MODEL_DEPENDENCY" })
    }

    @Test
    fun `nested view model declarations retain their outer qualified name`() {
        val sources =
            listOf(
                source(
                    path = "app/src/main/java/p/Outer.kt",
                    content =
                        """
                        package p
                        class Outer {
                            class Peer : androidx.lifecycle.ViewModel()
                        }
                        class SourceViewModel : androidx.lifecycle.ViewModel() {
                            val target: p.Outer.Peer? = null
                        }
                        """.trimIndent(),
                ),
            )

        val violations = SourceBoundaryVerifier.verify(sources, SourceBoundaryPolicy.generalRules)

        assertEquals(1, violations.count { it.ruleId == "VIEW_MODEL_DEPENDENCY" })
    }

    @Test
    fun `simple string templates scan executable view model references`() {
        val sources =
            listOf(
                source(
                    path = "app/src/main/java/sample/ViewModels.kt",
                    content =
                        """
                        package sample
                        val Peer = sample.Peer()
                        class SourceViewModel : androidx.lifecycle.ViewModel() {
                            val message = "${'$'}Peer"
                        }
                        """.trimIndent(),
                ),
                source(
                    path = "app/src/main/java/sample/Peer.kt",
                    content =
                        """
                        package sample
                        class Peer : androidx.lifecycle.ViewModel()
                        """.trimIndent(),
                ),
            )

        val violations = SourceBoundaryVerifier.verify(sources, SourceBoundaryPolicy.generalRules)

        assertEquals(1, violations.count { it.ruleId == "VIEW_MODEL_DEPENDENCY" })
    }

    @Test
    fun `typealiases preserve exact view model type and generic contained dependencies`() {
        val sources =
            listOf(
                source(
                    path = "app/src/main/java/sample/ViewModels.kt",
                    content =
                        """
                        package sample
                        class Source : androidx.lifecycle.ViewModel()
                        class Peer : androidx.lifecycle.ViewModel()
                        class Pair<A, B>
                        open class Box<T>
                        typealias Both = Pair<Source, Peer>
                        typealias Wrapped<T> = Pair<T, Peer>
                        typealias PeerBox = Box<Peer>
                        class UiBox : PeerBox()
                        class Third : androidx.lifecycle.ViewModel() {
                            val both: Both? = null
                            val wrapped: Wrapped<String>? = null
                            val box: PeerBox? = null
                        }
                        """.trimIndent(),
                ),
                source(
                    path = "app/src/main/java/sample/UiConsumer.kt",
                    content =
                        """
                        package sample
                        class UiConsumer : androidx.lifecycle.ViewModel() {
                            val box: UiBox? = null
                        }
                        """.trimIndent(),
                ),
            )

        val violations = SourceBoundaryVerifier.verify(sources, SourceBoundaryPolicy.generalRules)

        assertEquals(4, violations.count { it.ruleId == "VIEW_MODEL_DEPENDENCY" }, violations.toString())
        assertEquals(2, violations.count { it.evidence.startsWith("val both:") })
        assertEquals(1, violations.count { it.evidence.startsWith("val wrapped:") })
        assertEquals(1, violations.count { it.evidence.startsWith("val box:") })
        assertTrue(violations.none { it.path.endsWith("UiConsumer.kt") }, violations.toString())
    }

    @Test
    fun `class body on the line after the header is scanned`() {
        val sources =
            listOf(
                source(
                    path = "app/src/main/java/sample/Peer.kt",
                    content =
                        """
                        package sample
                        class Peer : androidx.lifecycle.ViewModel()
                        """.trimIndent(),
                ),
                source(
                    path = "app/src/main/java/sample/SourceViewModel.kt",
                    content =
                        """
                        package sample
                        class SourceViewModel : androidx.lifecycle.ViewModel()
                        {
                            val peer: Peer? = null
                        }
                        """.trimIndent(),
                ),
            )

        val violations = SourceBoundaryVerifier.verify(sources, SourceBoundaryPolicy.generalRules)

        assertEquals(1, violations.count { it.ruleId == "VIEW_MODEL_DEPENDENCY" })
    }

    @Test
    fun `same package nested type name resolves through its outer class`() {
        val sources =
            listOf(
                source(
                    path = "app/src/main/java/sample/Outer.kt",
                    content =
                        """
                        package sample
                        class Outer {
                            class Peer : androidx.lifecycle.ViewModel()
                        }
                        class SourceViewModel : androidx.lifecycle.ViewModel() {
                            val peer: Outer.Peer? = null
                        }
                        """.trimIndent(),
                ),
            )

        val violations = SourceBoundaryVerifier.verify(sources, SourceBoundaryPolicy.generalRules)

        assertEquals(1, violations.count { it.ruleId == "VIEW_MODEL_DEPENDENCY" })
    }

    @Test
    fun `local and parameter values shadow matching view model type names`() {
        val sources =
            listOf(
                source(
                    path = "app/src/main/java/sample/Peer.kt",
                    content =
                        """
                        package sample
                        class Peer : androidx.lifecycle.ViewModel()
                        """.trimIndent(),
                ),
                source(
                    path = "app/src/main/java/sample/SourceViewModel.kt",
                    content =
                        """
                        package sample
                        class SourceViewModel : androidx.lifecycle.ViewModel() {
                            fun render(Peer: String): String {
                                val local = Peer
                                return "${'$'}Peer"
                            }
                        }
                        """.trimIndent(),
                ),
            )

        val violations = SourceBoundaryVerifier.verify(sources, SourceBoundaryPolicy.generalRules)

        assertTrue(violations.none { it.ruleId == "VIEW_MODEL_DEPENDENCY" }, violations.toString())
    }

    @Test
    fun `expression body function parameter shadows matching view model type name`() {
        val sources =
            listOf(
                source(
                    path = "app/src/main/java/sample/Peer.kt",
                    content =
                        """
                        package sample
                        class Peer : androidx.lifecycle.ViewModel()
                        """.trimIndent(),
                ),
                source(
                    path = "app/src/main/java/sample/SourceViewModel.kt",
                    content =
                        """
                        package sample
                        class SourceViewModel : androidx.lifecycle.ViewModel() {
                            fun text(Peer: String) = Peer.length
                        }
                        """.trimIndent(),
                ),
            )

        val violations = SourceBoundaryVerifier.verify(sources, SourceBoundaryPolicy.generalRules)

        assertTrue(violations.none { it.ruleId == "VIEW_MODEL_DEPENDENCY" }, violations.toString())
    }

    @Test
    fun `expression body shadow does not hide a view model return type`() {
        val sources =
            listOf(
                source(
                    path = "app/src/main/java/sample/Peer.kt",
                    content =
                        """
                        package sample
                        class Peer : androidx.lifecycle.ViewModel()
                        """.trimIndent(),
                ),
                source(
                    path = "app/src/main/java/sample/SourceViewModel.kt",
                    content =
                        """
                        package sample
                        class SourceViewModel : androidx.lifecycle.ViewModel() {
                            fun text(Peer: String): Peer? = null
                        }
                        """.trimIndent(),
                ),
            )

        val violations = SourceBoundaryVerifier.verify(sources, SourceBoundaryPolicy.generalRules)

        assertEquals(1, violations.count { it.ruleId == "VIEW_MODEL_DEPENDENCY" })
    }

    @Test
    fun `expression body parameter does not shadow later block function`() {
        val sources =
            listOf(
                source(
                    path = "app/src/main/java/sample/Peer.kt",
                    content =
                        """
                        package sample
                        class Peer : androidx.lifecycle.ViewModel() {
                            companion object { operator fun invoke() = Peer() }
                        }
                        """.trimIndent(),
                ),
                source(
                    path = "app/src/main/java/sample/SourceViewModel.kt",
                    content =
                        """
                        package sample
                        class SourceViewModel : androidx.lifecycle.ViewModel() {
                            fun text(Peer: String) = Peer.length
                            fun newPeer() {
                                Peer()
                            }
                        }
                        """.trimIndent(),
                ),
            )

        val violations = SourceBoundaryVerifier.verify(sources, SourceBoundaryPolicy.generalRules)

        assertEquals(1, violations.count { it.ruleId == "VIEW_MODEL_DEPENDENCY" }, violations.toString())
        assertTrue(violations.single().evidence.contains("Peer()"))
    }

    @Test
    fun `supertype alias skips its exact base but reports generic view model arguments`() {
        val sources =
            listOf(
                source(
                    path = "app/src/main/java/sample/ViewModels.kt",
                    content =
                        """
                        package sample
                        class Peer : androidx.lifecycle.ViewModel()
                        open class Base<T> : androidx.lifecycle.ViewModel()
                        typealias PeerBase = Base<Peer>
                        class SourceViewModel : PeerBase()
                        """.trimIndent(),
                ),
            )

        val violations = SourceBoundaryVerifier.verify(sources, SourceBoundaryPolicy.generalRules)

        assertEquals(1, violations.count { it.ruleId == "VIEW_MODEL_DEPENDENCY" }, violations.toString())
        assertTrue(violations.single().evidence.contains("PeerBase"))
        assertTrue(violations.single().message.contains("sample.Peer"))
    }

    @Test
    fun `view model generic supertype arguments are scanned without rejecting base inheritance`() {
        val sources =
            listOf(
                source(
                    path = "app/src/main/java/sample/ViewModels.kt",
                    content =
                        """
                        package sample
                        class Peer : androidx.lifecycle.ViewModel()
                        open class Base<T> : androidx.lifecycle.ViewModel()
                        class SourceViewModel : Base<Peer>()
                        """.trimIndent(),
                ),
            )

        val violations = SourceBoundaryVerifier.verify(sources, SourceBoundaryPolicy.generalRules)

        assertEquals(1, violations.count { it.ruleId == "VIEW_MODEL_DEPENDENCY" })
    }

    @Test
    fun `where bound to a view model does not make a generic host a view model`() {
        val sources =
            listOf(
                source(
                    path = "app/src/main/java/sample/ViewModels.kt",
                    content =
                        """
                        package sample
                        class Peer : androidx.lifecycle.ViewModel()
                        class Host<T> where T : Peer
                        class SourceViewModel : androidx.lifecycle.ViewModel() {
                            val host: Host<*>? = null
                        }
                        class Route(val host: Host<Peer>)
                        """.trimIndent(),
                ),
            )

        val violations = SourceBoundaryVerifier.verify(sources, SourceBoundaryPolicy.generalRules)

        assertTrue(violations.none { it.ruleId == "VIEW_MODEL_DEPENDENCY" })
    }

    private fun source(
        path: String,
        content: String,
    ) = EditorSource(path, content)
}
