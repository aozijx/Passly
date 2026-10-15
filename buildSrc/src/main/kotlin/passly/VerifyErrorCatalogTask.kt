package passly

import java.io.File
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction

abstract class VerifyErrorCatalogTask : DefaultTask() {
    @get:InputFile
    abstract val catalogFile: RegularFileProperty

    @get:InputFile
    abstract val markdownFile: RegularFileProperty

    @get:InputFiles
    abstract val stringResourceFiles: ConfigurableFileCollection

    @TaskAction
    fun verify() {
        val catalog = ErrorCatalogParser.parse(catalogFile.get().asFile)
        val expectedMarkdown = ErrorCatalogRenderer.render(catalog).markdown
        require(markdownFile.get().asFile.readText() == expectedMarkdown) {
            "docs/ERROR_CODES.md is stale; run generateErrorCodes"
        }
        val resourcesByFile = stringResourceFiles.files.associateWith(::readStringNames)
        catalog.errors.filter { it.exposure == "USER_ACTIONABLE" }.forEach { error ->
            val key = requireNotNull(error.messageKey)
            val missingFrom = resourcesByFile.filterValues { key !in it }.keys
            require(missingFrom.isEmpty()) {
                "Missing string resource $key in ${missingFrom.joinToString { it.path }}"
            }
        }
    }

    private fun readStringNames(file: File): Set<String> {
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "")
            setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")
        }
        val nodes = factory.newDocumentBuilder().parse(file).getElementsByTagName("string")
        return buildSet {
            repeat(nodes.length) { index ->
                nodes.item(index).attributes?.getNamedItem("name")?.nodeValue?.let(::add)
            }
        }
    }
}
