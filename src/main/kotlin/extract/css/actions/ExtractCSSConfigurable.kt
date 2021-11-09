package extract.css.actions

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.PresentableEnumUtil
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel


class ExtractCSSConfigurable(private val project: Project) : Configurable {
    private lateinit var target: ComboBox<Target>
    private lateinit var targetLanguage: ComboBox<TargetLanguage>
    private lateinit var bemNesting: JCheckBox
    private lateinit var bemComments: JCheckBox
    private lateinit var bemElementSeparator: JBTextField
    private lateinit var bemModifierSeparator: JBTextField
    private lateinit var bemWrapper: JPanel

    override fun createComponent(): JComponent {
        target = PresentableEnumUtil.fill(ComboBox(JBUIScale.scale(280)), Target::class.java)
        targetLanguage = PresentableEnumUtil.fill(ComboBox(JBUIScale.scale(280)), TargetLanguage::class.java)
        bemNesting = JCheckBox("BEM Nesting")
        bemComments = JCheckBox("Use comments")
        bemElementSeparator = JBTextField(10)
        bemModifierSeparator = JBTextField(10)

        val mainBuilder = FormBuilder()
        mainBuilder.addLabeledComponent("Target:", target)
        mainBuilder.addLabeledComponent("Target language:", targetLanguage)

        mainBuilder.addComponent(bemNesting)

        val bemPanelBuilder = FormBuilder()
        bemPanelBuilder.addComponent(bemComments)
        bemPanelBuilder.addLabeledComponent("Element separator:", bemElementSeparator)
        bemPanelBuilder.addLabeledComponent("Modifier separator:", bemModifierSeparator)
        val bemPanel = bemPanelBuilder.panel
        bemPanel.border = JBUI.Borders.emptyLeft(25)
        
        bemWrapper = JPanel(BorderLayout())
        bemWrapper.add(bemPanel, BorderLayout.NORTH)
        mainBuilder.addComponent(bemWrapper)

        bemNesting.addChangeListener {
            updateVisibility()
        }

        val wrapper = JPanel(BorderLayout())
        wrapper.add(mainBuilder.panel, BorderLayout.NORTH)

        return wrapper
    }

    private fun updateVisibility() {
        UIUtil.setEnabled(bemWrapper, bemNesting.isSelected, true)
    }

    override fun isModified(): Boolean {
        val state = getInstance(project).state
        return target.selectedItem != Target.valueOf(state.target) ||
                targetLanguage.selectedItem != TargetLanguage.valueOf(state.language) ||
                bemNesting.isSelected != state.bem ||
                bemComments.isSelected != state.bemComments ||
                bemElementSeparator.text != state.bemElementPrefix ||
                bemModifierSeparator.text != state.bemElementModifierPrefix
    }

    override fun reset() {
        val state = getInstance(project).state
        target.selectedItem = Target.valueOf(state.target)
        targetLanguage.selectedItem = TargetLanguage.valueOf(state.language)
        bemNesting.isSelected = state.bem
        bemComments.isSelected = state.bemComments
        bemElementSeparator.text = state.bemElementPrefix
        bemModifierSeparator.text = state.bemElementModifierPrefix

        updateVisibility()
    }

    override fun apply() {
        val state = getInstance(project).state
        state.target = (target.selectedItem as? Target ?: defaultTarget).name
        state.language = (targetLanguage.selectedItem as? TargetLanguage ?: defaultLanguage).name
        state.bem = bemNesting.isSelected
        state.bemComments = bemComments.isSelected
        state.bemElementPrefix = bemElementSeparator.text
        state.bemElementModifierPrefix = bemModifierSeparator.text
    }

    override fun getDisplayName(): String = "ECSStractor"
}