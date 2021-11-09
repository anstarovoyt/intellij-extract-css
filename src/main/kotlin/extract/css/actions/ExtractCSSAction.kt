package extract.css.actions

import com.intellij.ide.scratch.ScratchRootType
import com.intellij.lang.Language
import com.intellij.lang.css.CSSLanguage
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.XmlRecursiveElementWalkingVisitor
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlFile
import java.awt.datatransfer.StringSelection

class ExtractCSSAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val xmlFile = e.getData(CommonDataKeys.PSI_FILE) as? XmlFile ?: return
        val project = e.project ?: return

        val state: ExtractState = getInstance(project).state
        val classNames = collectClassNames(xmlFile)
        val newContent = generateContent(state, classNames)

        generateFileAndApply(project, state, xmlFile, newContent)
    }

    private fun collectClassNames(xmlFile: XmlFile): MutableList<String> {
        val classNames: MutableList<String> = mutableListOf()
        xmlFile.acceptChildren(object : XmlRecursiveElementWalkingVisitor() {
            override fun visitXmlAttribute(attribute: XmlAttribute) {
                val name = attribute.name
                if (name == "class" || name == "className") {
                    classNames.addAll(attribute.value?.split(" ")?.filter(String::isNotBlank) ?: emptyList())
                }
            }
        })
        return classNames
    }

    private fun generateFileAndApply(project: Project, state: ExtractState, xmlFile: XmlFile, newContent: String) {
        WriteCommandAction
            .writeCommandAction(project)
            .withName("Create File")
            .run<Exception> {
                val language = Language.findLanguageByID(state.language) ?: CSSLanguage.INSTANCE
                val newFile = createNewFile(language, xmlFile, project, newContent) ?: return@run
                when (state.targetValue()) {
                    Target.NEW_FILE -> {
                        val directory = xmlFile.parent ?: return@run
                        val actualFile = directory.add(newFile) as? PsiFile ?: return@run
                        actualFile.navigate(true)
                    }
                    Target.SCRATCH_FILE -> {
                        val scratchFile = ScratchRootType.getInstance()
                            .createScratchFile(project, newFile.name, language, newFile.text) ?: return@run
                        val psiFile = PsiManager.getInstance(project).findFile(scratchFile)
                        psiFile?.navigate(false)
                    }
                    Target.CLIPBOARD -> {
                        CopyPasteManager.getInstance().setContents(StringSelection(newFile.text))
                    }
                }

            }
    }

    private fun createNewFile(language: Language, xmlFile: XmlFile, project: Project, newContent: String): PsiFile? {
        val defaultExtension = language.associatedFileType?.defaultExtension ?: "css"
        val newName = "${FileUtil.getNameWithoutExtension(xmlFile.name)}.$defaultExtension"
        val newFile =
            PsiFileFactory.getInstance(project).createFileFromText(newName, language, newContent) ?: return null
        return CodeStyleManager.getInstance(project).reformat(newFile) as? PsiFile
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible =
            e.getData(CommonDataKeys.PSI_FILE) is XmlFile && e.project != null
    }
}

class BEMBlock(val name: String) {
    val modifiers: MutableSet<String> = mutableSetOf()
    val elements: MutableMap<String, BEMElement> = mutableMapOf()

    fun isEmpty() = modifiers.isEmpty() && elements.isEmpty()
}

class BEMElement(val name: String) {
    val modifiers: MutableSet<String> = mutableSetOf()
    fun isEmpty() = modifiers.isEmpty()
}

fun generateContent(state: ExtractState, classNames: List<String>): String {
    return when (state.languageValue()) {
        TargetLanguage.CSS -> generateSimple(classNames, true)
        TargetLanguage.LESS -> generateBEM(state, classNames, true)
        TargetLanguage.SCSS -> generateBEM(state, classNames, true)
        TargetLanguage.STYLUS -> generateBEM(state, classNames, false)
        TargetLanguage.SASS -> generateBEM(state, classNames, false)
    }
}

private fun generateBEM(state: ExtractState, classNames: List<String>, braces: Boolean): String {
    return if (state.bem) generateBEMNesting(state, classNames, braces) else generateSimple(classNames, braces)
}

private fun generateSimple(classNames: List<String>, braces: Boolean) =
    classNames.joinToString("\n") { ".$it${if (braces) " {}" else ""}" }

private fun generateBEMNesting(state: ExtractState, classNames: List<String>, braces: Boolean): String {
    val blocks = prepare(state, classNames)
    val builder = StringBuilder()
    var first = true

    for (block in blocks) {
        when {
            first -> first = false
            else -> builder.append("\n")
        }

        appendOpenBlock(builder, block, braces)
        for ((_, element) in block.elements) {
            builder.append("\n")
            appendComment(builder, state, block.name, element.name, null)
            appendOpenElement(builder, state, braces, element)
            for (modifier in element.modifiers) {
                builder.append("\n")
                appendComment(builder, state, block.name, element.name, modifier)
                appendModifier(builder, state, braces, modifier, false)
            }

            appendCloseElement(element, braces, builder)
        }
        for (modifier in block.modifiers) {
            builder.append("\n")
            appendComment(builder, state, block.name, null, modifier)
            appendModifier(builder, state, braces, modifier, true)
        }

        appendCloseBlock(block, braces, builder)
    }

    return builder.toString()
}

private fun appendCloseBlock(block: BEMBlock, braces: Boolean, builder: StringBuilder) {
    val closeBrace = closeBrace(braces)
    if (!block.isEmpty() && braces) {
        builder.append("\n").append(closeBrace)
    } else {
        builder.append(closeBrace)
    }
}

private fun appendCloseElement(element: BEMElement, braces: Boolean, builder: StringBuilder) {
    val closeBrace = closeBrace(braces)
    if (!element.isEmpty() && braces) {
        builder.append("\n ").append(closeBrace)
    } else {
        builder.append(closeBrace)
    }
}

private fun appendModifier(
    builder: StringBuilder,
    state: ExtractState,
    braces: Boolean,
    modifier: String,
    topLevel: Boolean
) {
    builder
        .append(if (topLevel) " " else "  ")
        .append("&")
        .append(state.bemModifierPrefix)
        .append(modifier)
        .append(openBrace(braces))
        .append(closeBrace(braces))
}

private fun appendOpenElement(builder: StringBuilder, state: ExtractState, braces: Boolean, element: BEMElement) {
    builder
        .append(" ")
        .append("&")
        .append(state.bemElementPrefix)
        .append(element.name)
        .append(openBrace(braces))
}

private fun openBrace(braces: Boolean): String = if (braces) " {" else ""
private fun closeBrace(braces: Boolean): String = if (braces) "}" else ""

private fun appendOpenBlock(builder: StringBuilder, block: BEMBlock, braces: Boolean) {
    builder
        .append(".")
        .append(block.name)
        .append(openBrace(braces))
}

private fun appendComment(
    builder: StringBuilder,
    state: ExtractState,
    blockName: String,
    elementName: String?,
    modifierName: String?
) {
    if (!state.bemComments) return

    val lineCommentLanguages =
        mutableSetOf(TargetLanguage.SCSS, TargetLanguage.SASS, TargetLanguage.STYLUS, TargetLanguage.LESS)
    val commentStart = if (lineCommentLanguages.contains(state.languageValue())) "// " else "/* "
    val commentEnd = if (lineCommentLanguages.contains(state.languageValue())) "" else " */"

    val offset = if (elementName != null && modifierName != null) "  " else " "

    builder
        .append(offset)
        .append(commentStart)
        .append(".")
        .append(blockName)

    if (elementName != null) {
        builder
            .append(state.bemElementPrefix)
            .append(elementName)
    }
    if (modifierName != null) {
        builder
            .append(state.bemModifierPrefix)
            .append(modifierName)
    }

    builder
        .append(commentEnd)
        .append("\n")
}

private fun prepare(state: ExtractState, classNames: List<String>): List<BEMBlock> {
    val blocks = mutableMapOf<String, BEMBlock>()
    val elPrefix = state.bemElementPrefix
    val modPrefix = state.bemModifierPrefix

    for (className in classNames) {
        val indexOfElement = className.indexOf(elPrefix)
        if (hasElement(indexOfElement, className, elPrefix)) {
            val blockName = className.substring(0, indexOfElement)
            val elementWithModifier = className.substring(indexOfElement + elPrefix.length)
            val bemBlock = blocks.computeIfAbsent(blockName) { BEMBlock(it) }
            val indexOfModifier = elementWithModifier.indexOf(modPrefix)
            if (hasModifier(indexOfModifier, elementWithModifier, modPrefix)) {
                val elementName = elementWithModifier.substring(0, indexOfModifier)
                val modifierName = elementWithModifier.substring(indexOfModifier + modPrefix.length)
                val bemElement = bemBlock.elements.computeIfAbsent(elementName) { BEMElement(it) }
                bemElement.modifiers.add(modifierName)
            } else {
                bemBlock.elements.putIfAbsent(elementWithModifier, BEMElement(elementWithModifier))
            }
        } else {
            val indexOfModifier = className.indexOf(modPrefix)
            if (hasModifier(indexOfModifier, className, modPrefix)) {
                val blockName = className.substring(0, indexOfModifier)
                val modifierName = className.substring(indexOfModifier + modPrefix.length)
                val bemBlock = blocks.computeIfAbsent(blockName) { BEMBlock(it) }
                bemBlock.modifiers.add(modifierName)
            } else {
                blocks.putIfAbsent(className, BEMBlock(className))
            }
        }
    }

    return blocks.values.toList()
}

private fun hasElement(indexOfElement: Int, className: String, elPrefix: String) =
    indexOfElement > 0 && className.length > indexOfElement + elPrefix.length

private fun hasModifier(
    indexOfModifier: Int,
    elementWithModifier: String,
    modPrefix: String
) = indexOfModifier > 0 && elementWithModifier.length > indexOfModifier + modPrefix.length