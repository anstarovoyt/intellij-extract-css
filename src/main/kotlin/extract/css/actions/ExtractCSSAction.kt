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
    return if (state.bem) generateNesting(state, classNames, braces) else generateSimple(classNames, braces)
}

private fun generateSimple(classNames: List<String>, braces: Boolean) =
    classNames.joinToString("\n") { ".$it${if (braces) " {}" else ""}" }

private fun generatePlainIndentBased(classNames: List<String>) =
    classNames.joinToString("\n") { ".$it" }

private fun generateNesting(state: ExtractState, classNames: List<String>, braces: Boolean): String {
    val blocks = prepare(state, classNames)

    val builder = StringBuilder()

    val lineCommentLanguages =
        mutableSetOf(TargetLanguage.SCSS, TargetLanguage.SASS, TargetLanguage.STYLUS, TargetLanguage.LESS)

    var first = true
    val openBrace = if (braces) " {" else ""
    val closeBrace = if (braces) "}" else ""
    val commentStart = if (lineCommentLanguages.contains(state.languageValue())) "// " else "/* "
    val commentEnd = if (lineCommentLanguages.contains(state.languageValue())) "" else " */"

    for (block in blocks) {
        when {
            first -> first = false
            else -> builder.append("\n")
        }
        val prefixClass = "."
        builder.append(prefixClass).append(block.name).append(openBrace)
        for ((_, element) in block.elements) {
            builder.append("\n")
            val elementOffset = " "
            if (state.bemComments) {
                builder.append(elementOffset)
                    .append(commentStart)
                    .append(prefixClass)
                    .append(block.name)
                    .append(state.bemElementPrefix)
                    .append(element.name)
                    .append(commentEnd)
                    .append("\n")
            }
            builder.append(elementOffset).append("&").append(state.bemElementPrefix).append(element.name)
                .append(openBrace)
            for (modifier in element.modifiers) {
                builder.append("\n")
                val modifierOffset = "  "
                if (state.bemComments) {
                    builder.append(modifierOffset)
                        .append(commentStart)
                        .append(prefixClass)
                        .append(block.name)
                        .append(state.bemElementPrefix)
                        .append(element.name)
                        .append(state.bemModifierPrefix)
                        .append(modifier)
                        .append(commentEnd)
                        .append("\n")
                }

                builder.append(modifierOffset).append("&").append(state.bemModifierPrefix).append(modifier)
                    .append(openBrace)
                    .append(closeBrace)
            }

            if (!element.isEmpty() && braces) builder.append("\n ").append(closeBrace) else builder.append(closeBrace)
        }
        for (modifier in block.modifiers) {
            val topLevelModifierOffset = " "
            builder.append("\n")
            if (state.bemComments) {
                builder.append(topLevelModifierOffset)
                    .append(commentStart)
                    .append(prefixClass)
                    .append(block.name)
                    .append(state.bemModifierPrefix)
                    .append(modifier)
                    .append(commentEnd)
                    .append("\n")
            }
            builder.append(topLevelModifierOffset).append("&").append(state.bemModifierPrefix).append(modifier)
                .append(openBrace)
                .append(closeBrace)
        }

        if (!block.isEmpty() && braces) builder.append("\n").append(closeBrace) else builder.append(closeBrace)
    }

    return builder.toString()
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

class ExtractCSSAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val xmlFile = e.getData(CommonDataKeys.PSI_FILE) as? XmlFile ?: return
        val project = e.project ?: return

        val state: ExtractState = getInstance(project).state
        val classNames: MutableList<String> = mutableListOf()
        xmlFile.acceptChildren(object : XmlRecursiveElementWalkingVisitor() {
            override fun visitXmlAttribute(attribute: XmlAttribute) {
                val name = attribute.name
                if (name == "class" || name == "className") {
                    classNames.addAll(attribute.value?.split(" ")?.filter(String::isNotBlank) ?: emptyList())
                }
            }
        })

        val newContent = generateContent(state, classNames)

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