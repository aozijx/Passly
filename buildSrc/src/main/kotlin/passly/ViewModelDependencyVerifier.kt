package passly

internal object ViewModelDependencyVerifier {
    private const val ANDROIDX_VIEW_MODEL = "androidx.lifecycle.ViewModel"
    private val packagePattern = Regex("""(?m)^\s*package\s+([A-Za-z_][A-Za-z0-9_.]*)""")
    private val importPattern =
        Regex("""(?m)^\s*import\s+([A-Za-z_][A-Za-z0-9_.*]*(?:\.[A-Za-z_][A-Za-z0-9_]*)*)(?:\s+as\s+(`?[A-Za-z_][A-Za-z0-9_]*`?))?""")
    private val classPattern = Regex("""\bclass\s+(`[^`\r\n]+`|[A-Za-z_][A-Za-z0-9_]*)""")
    private val typeAliasPattern =
        Regex("""\btypealias\s+(`[^`\r\n]+`|[A-Za-z_][A-Za-z0-9_]*)(?:\s*<([^>\r\n]+)>)?\s*=\s*([^\r\n;]+)""")
    private val identifierPattern = Regex("""(?<![A-Za-z0-9_])(`[^`\r\n]+`|[A-Za-z_][A-Za-z0-9_]*)(?![A-Za-z0-9_])""")
    private val qualifiedIdentifierPattern =
        Regex("""(?:`[^`\r\n]+`|[A-Za-z_][A-Za-z0-9_]*)(?:\s*\.\s*(?:`[^`\r\n]+`|[A-Za-z_][A-Za-z0-9_]*))+""")
    private val typeNameAtStartPattern =
        Regex("""^\s*((?:`[^`\r\n]+`|[A-Za-z_][A-Za-z0-9_]*)(?:\s*\.\s*(?:`[^`\r\n]+`|[A-Za-z_][A-Za-z0-9_]*))*)""")
    private val functionBlockPattern = Regex("""\bfun\s+[A-Za-z_][A-Za-z0-9_]*\s*\(([^)]*)\)[^{\r\n]*\{""")
    private val expressionBodyFunctionPattern = Regex("""\bfun\s+[A-Za-z_][A-Za-z0-9_]*\s*\(([^)]*)\)[^{}\r\n]*=\s*[^;\r\n]+""")
    private val parameterNamePattern =
        Regex("""^\s*(?:(?:crossinline|noinline|vararg|val|var)\s+)*(`[^`\r\n]+`|[A-Za-z_][A-Za-z0-9_]*)\s*:""")
    private val localValuePattern = Regex("""\b(?:val|var)\s+(`[^`\r\n]+`|[A-Za-z_][A-Za-z0-9_]*)\b""")
    private val declarationStartPattern =
        Regex(
            listOf(
                """^(?:(?:public|private|protected|internal|inline|tailrec|operator|infix|suspend|external|expect|actual|""",
                """open|final|abstract|data|sealed|annotation|enum|inner|value|const|lateinit)\s+)*""",
                """(?:class|interface|object|typealias|fun|val|var)\b""",
            ).joinToString(separator = ""),
        )
    private val escapedIdentifierPattern = Regex("""`([A-Za-z_][A-Za-z0-9_]*)`""")
    private val dotWhitespacePattern = Regex("""\s*\.\s*""")
    private val viewModelAnnotationPattern = Regex("""@(?:[A-Za-z_][A-Za-z0-9_]*\.)*HiltViewModel\b""")

    fun verify(sources: List<EditorSource>): List<SourceBoundaryViolation> {
        val parsedSources = sources.map(::parseSource)
        val viewModelSymbols = resolveViewModelSymbols(parsedSources)
        if (viewModelSymbols.viewModelClassNames.isEmpty()) return emptyList()
        return parsedSources.flatMap { source ->
            val ownViewModels =
                source.classes.mapNotNull { declaration ->
                    declaration.qualifiedName
                        .takeIf(viewModelSymbols.viewModelClassNames::contains)
                        ?.let {
                            ViewModelDeclaration(
                                source.source,
                                declaration.qualifiedName,
                                declaration.declarationRange,
                                declaration.supertypeClassifierRanges,
                            )
                        }
                }
            if (ownViewModels.isEmpty()) return@flatMap emptyList()
            findReferences(source, ownViewModels, viewModelSymbols)
        }
    }

    private fun parseSource(source: EditorSource): ParsedSource {
        val code = SourceBoundaryVerifier.maskCommentsAndStrings(source.content)
        val packageName =
            packagePattern
                .find(code)
                ?.groupValues
                ?.get(1)
                .orEmpty()
        val imports =
            importPattern
                .findAll(code)
                .mapNotNull { match ->
                    val qualifiedName = normalizeQualifiedName(match.groupValues[1])
                    if (qualifiedName.endsWith(".*")) {
                        ImportedName(qualifiedName, qualifiedName.removeSuffix(".*"), wildcard = true)
                    } else {
                        val alias = match.groupValues[2].takeIf(String::isNotBlank)?.let(::normalizeIdentifier)
                        ImportedName(alias ?: qualifiedName.substringAfterLast('.'), qualifiedName, wildcard = false)
                    }
                }.toList()
        val declarations = mutableListOf<ClassDeclaration>()
        classPattern.findAll(code).forEach { match ->
            classDeclaration(code, packageName, imports, match, declarations)?.let(declarations::add)
        }
        val typeAliases =
            typeAliasPattern
                .findAll(code)
                .map { match ->
                    val typeParameters = parseTypeParameters(match.groupValues[2])
                    val rightHandSide = match.groupValues[3]
                    TypeAliasDeclaration(
                        packageName = packageName,
                        imports = imports,
                        qualifiedName =
                            listOf(
                                packageName,
                                normalizeIdentifier(match.groupValues[1]),
                            ).filter(String::isNotBlank).joinToString("."),
                        targetClassifierName =
                            typeNameAtStartPattern
                                .find(rightHandSide)
                                ?.groupValues
                                ?.get(1)
                                ?.let(::normalizeQualifiedName)
                                ?.takeUnless(typeParameters::contains),
                        targetTypeNames = extractTypeReferences(rightHandSide, typeParameters),
                    )
                }.toList()
        return ParsedSource(source, code, packageName, imports, declarations, typeAliases)
    }

    private fun classDeclaration(
        code: String,
        packageName: String,
        imports: List<ImportedName>,
        match: MatchResult,
        enclosingDeclarations: List<ClassDeclaration>,
    ): ClassDeclaration? {
        val name = normalizeIdentifier(match.groupValues[1])
        val headerStart = match.range.last + 1
        val bodyStart = findClassBodyStart(code, headerStart)
        val declarationEnd =
            if (bodyStart == null) {
                findHeaderEnd(code, headerStart)
            } else {
                findMatchingBrace(code, bodyStart) ?: return null
            }
        val headerEnd = bodyStart ?: declarationEnd
        val header = code.substring(match.range.first, headerEnd)
        val declarationContext = annotationContext(code, match.range.first)
        val enclosingClass =
            enclosingDeclarations
                .filter { match.range.first in it.declarationRange }
                .minByOrNull { it.declarationRange.last - it.declarationRange.first }
        val qualifiedName =
            listOfNotNull(
                enclosingClass?.qualifiedName ?: packageName.takeIf(String::isNotBlank),
                name,
            ).joinToString(".")
        return ClassDeclaration(
            name = name,
            qualifiedName = qualifiedName,
            packageName = packageName,
            imports = imports,
            declarationRange = match.range.first..declarationEnd.coerceAtMost(code.lastIndex),
            isHiltViewModel = viewModelAnnotationPattern.containsMatchIn(declarationContext),
            supertypeNames = superclassNames(header),
            supertypeClassifierRanges = superclassClassifierRanges(header, match.range.first),
        )
    }

    private fun parseTypeParameters(parameters: String): Set<String> =
        parameters
            .split(',')
            .mapNotNull { parameter ->
                Regex("""(?:in|out|reified)\s+""")
                    .replace(parameter.trim(), "")
                    .substringBefore(':')
                    .trim()
                    .takeIf { it.matches(Regex("""`[^`]+`|[A-Za-z_][A-Za-z0-9_]*""")) }
                    ?.let(::normalizeIdentifier)
            }.toSet()

    private fun extractTypeReferences(
        typeText: String,
        typeParameters: Set<String>,
    ): List<String> {
        val qualifiedMatches = qualifiedIdentifierPattern.findAll(typeText).toList()
        val references = qualifiedMatches.map { normalizeQualifiedName(it.value) }.toMutableList()
        identifierPattern
            .findAll(typeText)
            .filterNot { token -> qualifiedMatches.any { token.range.first in it.range } }
            .map { normalizeIdentifier(it.value) }
            .filterNot(typeParameters::contains)
            .toCollection(references)
        return references.distinct()
    }

    private fun resolveViewModelSymbols(sources: List<ParsedSource>): ViewModelSymbols {
        val classes = sources.flatMap(ParsedSource::classes)
        val aliases = sources.flatMap(ParsedSource::typeAliases)
        val declarationsByName =
            (classes.map(ClassDeclaration::qualifiedName) + aliases.map(TypeAliasDeclaration::qualifiedName)).toSet()
        val viewModelClassNames = mutableSetOf<String>()
        val exactViewModelTypeNames = mutableSetOf(ANDROIDX_VIEW_MODEL)
        val dependencyOwners = mutableMapOf<String, MutableSet<String>>()
        dependencyOwners[ANDROIDX_VIEW_MODEL] = mutableSetOf(ANDROIDX_VIEW_MODEL)

        classes.filter(ClassDeclaration::isHiltViewModel).forEach { declaration ->
            viewModelClassNames += declaration.qualifiedName
        }
        classes.forEach { declaration ->
            if (declaration.supertypeNames.any { supertype ->
                    resolveReferenceNames(declaration.packageName, declaration.imports, supertype, declarationsByName)
                        .contains(ANDROIDX_VIEW_MODEL)
                }
            ) {
                viewModelClassNames += declaration.qualifiedName
            }
        }

        var changed: Boolean
        do {
            changed = false
            viewModelClassNames.forEach { name ->
                if (exactViewModelTypeNames.add(name)) changed = true
                if (dependencyOwners.getOrPut(name, ::mutableSetOf).add(name)) changed = true
            }

            aliases.forEach { alias ->
                val targets =
                    alias.targetTypeNames.flatMap { target ->
                        resolveReferenceNames(alias.packageName, alias.imports, target, declarationsByName)
                    }
                val containedOwners = targets.flatMap { dependencyOwners[it].orEmpty() }.toSet()
                if (dependencyOwners.getOrPut(alias.qualifiedName, ::mutableSetOf).addAll(containedOwners)) {
                    changed = true
                }

                val exactTarget = alias.targetClassifierName
                if (exactTarget != null) {
                    val resolvedTargets = resolveReferenceNames(alias.packageName, alias.imports, exactTarget, declarationsByName)
                    if (resolvedTargets.any(exactViewModelTypeNames::contains) && exactViewModelTypeNames.add(alias.qualifiedName)) {
                        changed = true
                    }
                }
            }

            classes.forEach { declaration ->
                val isDerivedViewModel =
                    declaration.supertypeNames.any { supertype ->
                        resolveReferenceNames(declaration.packageName, declaration.imports, supertype, declarationsByName)
                            .any(exactViewModelTypeNames::contains)
                    }
                if (isDerivedViewModel && viewModelClassNames.add(declaration.qualifiedName)) {
                    changed = true
                }
            }
        } while (changed)

        val exactBaseOwnerByTypeName = exactBaseOwnerByTypeName(aliases, exactViewModelTypeNames, viewModelClassNames)
        return ViewModelSymbols(
            dependencyOwnersByQualifiedName = dependencyOwners.mapValues { it.value.toSet() },
            exactViewModelTypeNames = exactViewModelTypeNames,
            viewModelClassNames = viewModelClassNames,
            exactBaseOwnerByTypeName = exactBaseOwnerByTypeName,
            declarationNames = declarationsByName,
        )
    }

    private fun exactBaseOwnerByTypeName(
        aliases: List<TypeAliasDeclaration>,
        exactViewModelTypeNames: Set<String>,
        viewModelClassNames: Set<String>,
    ): Map<String, String> {
        val owners = mutableMapOf(ANDROIDX_VIEW_MODEL to ANDROIDX_VIEW_MODEL)
        viewModelClassNames.forEach { owners[it] = it }
        var changed: Boolean
        do {
            changed = false
            aliases.forEach { alias ->
                if (alias.qualifiedName !in exactViewModelTypeNames || alias.targetClassifierName == null) return@forEach
                val targetOwner =
                    resolveReferenceNames(alias.packageName, alias.imports, alias.targetClassifierName, exactViewModelTypeNames)
                        .firstNotNullOfOrNull(owners::get)
                if (targetOwner != null && owners[alias.qualifiedName] != targetOwner) {
                    owners[alias.qualifiedName] = targetOwner
                    changed = true
                }
            }
        } while (changed)
        return owners
    }

    private fun resolveReferenceNames(
        packageName: String,
        imports: List<ImportedName>,
        name: String,
        declarationNames: Set<String>,
    ): Set<String> {
        val normalizedName = normalizeQualifiedName(name)
        if (normalizedName.contains('.')) {
            val candidates = setOf(normalizedName, "$packageName.$normalizedName")
            return candidates.filterTo(mutableSetOf()) {
                it in declarationNames || it == "androidx.lifecycle.ViewModel"
            }
        }

        val explicitImports =
            imports
                .filterNot(ImportedName::wildcard)
                .filter { it.alias == normalizedName }
                .map(ImportedName::qualifiedName)
        if (explicitImports.isNotEmpty()) {
            return explicitImports.filterTo(mutableSetOf()) {
                it in declarationNames || it == "androidx.lifecycle.ViewModel"
            }
        }

        val samePackageName = "$packageName.$normalizedName"
        if (packageName.isNotBlank() && samePackageName in declarationNames) return setOf(samePackageName)

        return imports
            .filter(ImportedName::wildcard)
            .map { "${it.packageName}.$normalizedName" }
            .filterTo(mutableSetOf()) { it in declarationNames || it == "androidx.lifecycle.ViewModel" }
    }

    private fun superclassNames(header: String): List<String> {
        val colonIndex = findTopLevelColon(header)
        if (colonIndex < 0) return emptyList()
        val supertypes = header.substring(colonIndex + 1)
        return splitTopLevel(supertypes, ',').mapNotNull { specifier ->
            typeNameAtStartPattern
                .find(specifier)
                ?.groupValues
                ?.get(1)
                ?.let(::normalizeQualifiedName)
        }
    }

    private fun superclassClassifierRanges(
        header: String,
        absoluteStart: Int,
    ): List<IntRange> {
        val colonIndex = findTopLevelColon(header)
        if (colonIndex < 0) return emptyList()
        val supertypes = header.substring(colonIndex + 1)
        return splitTopLevel(supertypes, ',').mapNotNull { specifier ->
            typeNameAtStartPattern.find(specifier)?.range?.let { classifierRange ->
                val specifierStart = supertypes.indexOf(specifier)
                val start = absoluteStart + colonIndex + 1 + specifierStart + classifierRange.first
                start..(start + classifierRange.last - classifierRange.first)
            }
        }
    }

    private fun splitTopLevel(
        text: String,
        separator: Char,
    ): List<String> {
        val parts = mutableListOf<String>()
        var start = 0
        var parentheses = 0
        var brackets = 0
        var angles = 0
        text.forEachIndexed { index, character ->
            when (character) {
                '(' -> {
                    parentheses++
                }

                ')' -> {
                    parentheses = (parentheses - 1).coerceAtLeast(0)
                }

                '[' -> {
                    brackets++
                }

                ']' -> {
                    brackets = (brackets - 1).coerceAtLeast(0)
                }

                '<' -> {
                    if (parentheses == 0 && brackets == 0) angles++
                }

                '>' -> {
                    if (parentheses == 0 && brackets == 0 && angles > 0) angles--
                }

                separator -> {
                    if (parentheses == 0 && brackets == 0 && angles == 0) {
                        parts += text.substring(start, index)
                        start = index + 1
                    }
                }
            }
        }
        parts += text.substring(start)
        return parts
    }

    private fun findTopLevelColon(header: String): Int {
        val whereIndex = Regex("\\bwhere\\b").find(header)?.range?.first ?: Int.MAX_VALUE
        var parentheses = 0
        var brackets = 0
        var angles = 0
        header.forEachIndexed { index, character ->
            when (character) {
                '(' -> parentheses++
                ')' -> parentheses = (parentheses - 1).coerceAtLeast(0)
                '[' -> brackets++
                ']' -> brackets = (brackets - 1).coerceAtLeast(0)
                '<' -> if (parentheses == 0 && brackets == 0) angles++
                '>' -> if (parentheses == 0 && brackets == 0 && angles > 0) angles--
                ':' -> if (parentheses == 0 && brackets == 0 && angles == 0 && index < whereIndex) return index
            }
        }
        return -1
    }

    private fun findClassBodyStart(
        code: String,
        start: Int,
    ): Int? {
        var index = start
        var parentheses = 0
        var brackets = 0
        var angles = 0
        while (index < code.length) {
            when (code[index]) {
                '(' -> {
                    parentheses++
                }

                ')' -> {
                    parentheses = (parentheses - 1).coerceAtLeast(0)
                }

                '[' -> {
                    brackets++
                }

                ']' -> {
                    brackets = (brackets - 1).coerceAtLeast(0)
                }

                '<' -> {
                    if (parentheses == 0 && brackets == 0) angles++
                }

                '>' -> {
                    if (parentheses == 0 && brackets == 0 && angles > 0) angles--
                }

                '{' -> {
                    if (parentheses == 0 && brackets == 0 && angles == 0) return index
                }

                '\n' -> {
                    if (parentheses == 0 && brackets == 0 && angles == 0) {
                        val nextLine = code.substring(index + 1).trimStart()
                        if (isDeclarationStart(nextLine) && !nextLine.startsWith(":")) return null
                        if (!continuesClassHeader(code, start, index, nextLine)) return null
                    }
                }

                ';' -> {
                    if (parentheses == 0 && brackets == 0 && angles == 0) return null
                }
            }
            index++
        }
        return null
    }

    private fun findHeaderEnd(
        code: String,
        start: Int,
    ): Int {
        var index = start
        var parentheses = 0
        var brackets = 0
        var angles = 0
        while (index < code.length) {
            when (code[index]) {
                '(' -> {
                    parentheses++
                }

                ')' -> {
                    parentheses = (parentheses - 1).coerceAtLeast(0)
                }

                '[' -> {
                    brackets++
                }

                ']' -> {
                    brackets = (brackets - 1).coerceAtLeast(0)
                }

                '<' -> {
                    if (parentheses == 0 && brackets == 0) angles++
                }

                '>' -> {
                    if (parentheses == 0 && brackets == 0 && angles > 0) angles--
                }

                '\n' -> {
                    if (parentheses == 0 && brackets == 0 && angles == 0) {
                        val nextLine = code.substring(index + 1).trimStart()
                        if (!continuesClassHeader(code, start, index, nextLine)) return index
                    }
                }
            }
            index++
        }
        return code.length
    }

    private fun continuesClassHeader(
        code: String,
        start: Int,
        newline: Int,
        nextLine: String,
    ): Boolean {
        val previous = code.substring(start, newline).trimEnd().lastOrNull()
        return nextLine.startsWith(":") || nextLine.startsWith("{") || previous == ':' || previous == ','
    }

    private fun isDeclarationStart(line: String): Boolean =
        line.startsWith("@") ||
            declarationStartPattern.containsMatchIn(line)

    private fun annotationContext(
        code: String,
        declarationStart: Int,
    ): String {
        val prefix = code.substring(0, declarationStart)
        val lines = prefix.lineSequence().toList()
        val annotationLines = mutableListOf<String>()
        for (line in lines.asReversed()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("@")) {
                annotationLines += trimmed
            } else {
                break
            }
        }
        return annotationLines.asReversed().joinToString("\n")
    }

    private fun findMatchingBrace(
        code: String,
        start: Int,
    ): Int? {
        var depth = 0
        for (index in start until code.length) {
            when (code[index]) {
                '{' -> {
                    depth++
                }

                '}' -> {
                    depth--
                    if (depth == 0) return index
                }
            }
        }
        return null
    }

    private fun findReferences(
        source: ParsedSource,
        ownViewModels: List<ViewModelDeclaration>,
        symbols: ViewModelSymbols,
    ): List<SourceBoundaryViolation> {
        val violations = mutableListOf<SourceBoundaryViolation>()

        fun report(
            line: Int,
            target: String,
        ) {
            val rawLine =
                source.source.content
                    .lineSequence()
                    .toList()
                    .getOrNull(line)
                    .orEmpty()
                    .trim()
            val path = source.source.path.replace('\\', '/')
            violations +=
                SourceBoundaryViolation(
                    ruleId = SourceBoundaryPolicy.VIEW_MODEL_DEPENDENCY_RULE_ID,
                    owner = "ViewModel dependency",
                    path = path,
                    evidence = rawLine,
                    message =
                        "a ViewModel must not reference another ViewModel ($target); " +
                            "share behavior through an application/Domain contract",
                )
        }

        ownViewModels.forEach { ownViewModel ->
            val range = ownViewModel.declarationRange
            val body = source.code.substring(range.first, range.last + 1)
            val shadowedValues = shadowedValues(body)
            val qualifiedMatches = qualifiedIdentifierPattern.findAll(body).toList()
            qualifiedMatches.forEach { match ->
                val absoluteStart = range.first + match.range.first
                val inheritedBaseOwners =
                    inheritedBaseOwnersAt(
                        absoluteStart,
                        ownViewModel,
                        source,
                        symbols,
                    )
                val firstIdentifier = identifierPattern.find(match.value)
                if (firstIdentifier != null &&
                    isShadowedValue(
                        normalizeIdentifier(firstIdentifier.value),
                        match.range.first + firstIdentifier.range.first,
                        body,
                        shadowedValues,
                    )
                ) {
                    return@forEach
                }
                val name = normalizeQualifiedName(match.value)
                val knownPrefix =
                    longestKnownQualifiedPrefix(
                        source.packageName,
                        source.imports,
                        name,
                        symbols.declarationNames,
                    )
                if (knownPrefix != null) {
                    symbols.dependencyOwnersByQualifiedName[knownPrefix]
                        .orEmpty()
                        .filter { it != ownViewModel.qualifiedName && it !in inheritedBaseOwners }
                        .forEach { target ->
                            report(source.code.substring(0, range.first + match.range.first).count { it == '\n' }, target)
                        }
                }
            }
            identifierPattern.findAll(body).forEach { match ->
                val absoluteStart = range.first + match.range.first
                val inheritedBaseOwners =
                    inheritedBaseOwnersAt(
                        absoluteStart,
                        ownViewModel,
                        source,
                        symbols,
                    )
                if (qualifiedMatches.any { match.range.first in it.range }) return@forEach
                val identifier = normalizeIdentifier(match.value)
                if (isShadowedValue(identifier, match.range.first, body, shadowedValues)) return@forEach
                val targets =
                    resolveReferenceNames(source.packageName, source.imports, identifier, symbols.declarationNames)
                        .flatMap { symbols.dependencyOwnersByQualifiedName[it].orEmpty() }
                        .filter { it != ownViewModel.qualifiedName && it !in inheritedBaseOwners }
                        .distinct()
                targets.forEach { target ->
                    report(source.code.substring(0, range.first + match.range.first).count { it == '\n' }, target)
                }
            }
        }
        return violations.distinctBy { listOf(it.path, it.evidence, it.ruleId, it.message) }
    }

    private fun inheritedBaseOwnersAt(
        absoluteStart: Int,
        ownViewModel: ViewModelDeclaration,
        source: ParsedSource,
        symbols: ViewModelSymbols,
    ): Set<String> {
        val classifierRange = ownViewModel.supertypeClassifierRanges.firstOrNull { absoluteStart in it } ?: return emptySet()
        val classifierName = source.code.substring(classifierRange.first, classifierRange.last + 1).trim()
        return resolveReferenceNames(source.packageName, source.imports, classifierName, symbols.declarationNames)
            .mapNotNull(symbols.exactBaseOwnerByTypeName::get)
            .toSet()
    }

    private fun shadowedValues(body: String): List<ShadowedValue> =
        buildList {
            val blockFunctions = functionBlockPattern.findAll(body).toList()
            blockFunctions.forEach { function ->
                val openBrace = function.range.last
                val closeBrace = findMatchingBrace(body, openBrace) ?: return@forEach
                addAll(parameterShadows(body, function, closeBrace))
                localValuePattern
                    .findAll(body, function.range.last + 1)
                    .filter { it.range.first <= closeBrace }
                    .forEach { local ->
                        val name = normalizeIdentifier(local.groupValues[1])
                        val declarationOffset = local.groups[1]!!.range.first
                        add(ShadowedValue(name, declarationOffset..closeBrace, declarationOffset))
                    }
            }
            expressionBodyFunctionPattern
                .findAll(body)
                .filterNot { expression -> blockFunctions.any { expression.range.first in it.range } }
                .forEach { expression ->
                    addAll(parameterShadows(body, expression, expression.range.last))
                }
        }

    private fun parameterShadows(
        body: String,
        function: MatchResult,
        visibleUntil: Int,
    ): List<ShadowedValue> =
        buildList {
            val parameterText = function.groupValues[1]
            val parameterTextStart = function.groups[1]?.range?.first ?: return@buildList
            val functionRange = function.range.first..visibleUntil
            var segmentSearchStart = 0
            splitTopLevel(parameterText, ',').forEach { segment ->
                val segmentStart = parameterText.indexOf(segment, segmentSearchStart)
                segmentSearchStart = segmentStart + segment.length
                val parameter = parameterNamePattern.find(segment) ?: return@forEach
                val name = normalizeIdentifier(parameter.groupValues[1])
                val declarationOffset =
                    parameterTextStart + segmentStart + parameter.range.first +
                        parameter.groupValues[0].indexOf(parameter.groupValues[1])
                add(ShadowedValue(name, functionRange, declarationOffset))
            }
        }

    private fun isShadowedValue(
        name: String,
        relativeOffset: Int,
        body: String,
        shadowedValues: List<ShadowedValue>,
    ): Boolean =
        shadowedValues.any { shadow ->
            shadow.name == name && relativeOffset in shadow.visibleRange &&
                (relativeOffset == shadow.declarationOffset || !isTypePosition(body, relativeOffset))
        }

    private fun isTypePosition(
        body: String,
        identifierStart: Int,
    ): Boolean {
        val prefix = body.substring(0, identifierStart).trimEnd()
        if (prefix.isEmpty()) return false
        if (prefix.endsWith(":")) return true
        val previousWord = Regex("""([A-Za-z_][A-Za-z0-9_]*)$""").find(prefix)?.groupValues?.get(1)
        if (previousWord == "as" || previousWord == "is") return true
        val last = prefix.last()
        if (last == '<') return true
        if (last == ',') {
            var angleDepth = 0
            prefix.forEach { character ->
                when (character) {
                    '<' -> angleDepth++
                    '>' -> angleDepth = (angleDepth - 1).coerceAtLeast(0)
                }
            }
            return angleDepth > 0
        }
        return false
    }

    private fun longestKnownQualifiedPrefix(
        packageName: String,
        imports: List<ImportedName>,
        name: String,
        declarationNames: Set<String>,
    ): String? {
        val segments = name.split('.')
        for (end in segments.size downTo 1) {
            val candidate = segments.take(end).joinToString(".")
            val resolved = resolveReferenceNames(packageName, imports, candidate, declarationNames)
            resolved.firstOrNull { it in declarationNames || it == ANDROIDX_VIEW_MODEL }?.let { return it }
        }
        return null
    }

    private fun normalizeQualifiedName(name: String): String =
        escapedIdentifierPattern.replace(name) { it.groupValues[1] }.replace(dotWhitespacePattern, ".")

    private fun normalizeIdentifier(name: String): String = escapedIdentifierPattern.replace(name) { it.groupValues[1] }

    private data class ParsedSource(
        val source: EditorSource,
        val code: String,
        val packageName: String,
        val imports: List<ImportedName>,
        val classes: List<ClassDeclaration>,
        val typeAliases: List<TypeAliasDeclaration>,
    )

    private data class ViewModelSymbols(
        val dependencyOwnersByQualifiedName: Map<String, Set<String>>,
        val exactViewModelTypeNames: Set<String>,
        val viewModelClassNames: Set<String>,
        val exactBaseOwnerByTypeName: Map<String, String>,
        val declarationNames: Set<String>,
    )

    private data class ImportedName(
        val alias: String,
        val packageName: String,
        val wildcard: Boolean,
    ) {
        val qualifiedName: String
            get() = if (wildcard) "$packageName.*" else packageName
    }

    private data class ClassDeclaration(
        val name: String,
        val qualifiedName: String,
        val packageName: String,
        val imports: List<ImportedName>,
        val declarationRange: IntRange,
        val isHiltViewModel: Boolean,
        val supertypeNames: List<String>,
        val supertypeClassifierRanges: List<IntRange>,
    )

    private data class TypeAliasDeclaration(
        val packageName: String,
        val imports: List<ImportedName>,
        val qualifiedName: String,
        val targetClassifierName: String?,
        val targetTypeNames: List<String>,
    )

    private data class ViewModelDeclaration(
        val source: EditorSource,
        val qualifiedName: String,
        val declarationRange: IntRange,
        val supertypeClassifierRanges: List<IntRange>,
    )

    private data class ShadowedValue(
        val name: String,
        val visibleRange: IntRange,
        val declarationOffset: Int,
    )
}
