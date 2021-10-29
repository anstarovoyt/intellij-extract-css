package extract.css.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.XmlRecursiveElementWalkingVisitor
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.css.CssFileType
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlFile

class ExtractCSSAction : AnAction() {
    
    override fun actionPerformed(e: AnActionEvent) {
        val xmlFile = e.getData(CommonDataKeys.PSI_FILE) as? XmlFile ?: return
        val project = e.project ?: return
        val classNames = mutableListOf<String>()
        xmlFile.acceptChildren(object : XmlRecursiveElementWalkingVisitor() {
            override fun visitXmlAttribute(attribute: XmlAttribute) {
                if (attribute.name == "class") {
                    classNames.addAll(attribute.value?.split(" ")?.filter(String::isNotBlank) ?: emptyList())
                }
            }
        })

        val newContent = classNames.joinToString("\n\n") { ".$it {}" }

        WriteCommandAction
            .writeCommandAction(project)
            .withName("Create CSS File")
            .run<Exception> {
                val newName = "${FileUtil.getNameWithoutExtension(xmlFile.name)}.css"
                val newFile = PsiFileFactory.getInstance(project)
                    .createFileFromText(newName, CssFileType.INSTANCE, newContent)

                val directory = xmlFile.parent ?: return@run
                val actualFile = directory.add(newFile) as? PsiFile ?: return@run
                CodeStyleManager.getInstance(project).reformat(actualFile)
                actualFile.navigate(true)
            }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = 
            e.getData(CommonDataKeys.PSI_FILE) is XmlFile && e.project != null
    }
}