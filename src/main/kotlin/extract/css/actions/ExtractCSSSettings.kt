package extract.css.actions

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.ui.PresentableEnum

enum class Target : PresentableEnum {
    NEW_FILE {
        override fun getPresentableText(): String = "New file"
    },
    SCRATCH_FILE {
        override fun getPresentableText(): String = "Scratch file"
    },
    CLIPBOARD {
        override fun getPresentableText(): String = "Clipboard"
    };
}

enum class TargetLanguage : PresentableEnum {
    CSS {
        override fun getPresentableText(): String = "CSS"
    },
    SASS {
        override fun getPresentableText(): String = "SASS"
    },    
    SCSS {
        override fun getPresentableText(): String = "SCSS"
    },
    LESS {
        override fun getPresentableText(): String = "LESS"
    },
    STYLUS {
        override fun getPresentableText(): String = "Stylus"
    }
}

val defaultLanguage = TargetLanguage.CSS
val defaultTarget = Target.SCRATCH_FILE

class ExtractState {
    var language: String = defaultLanguage.name
    var target: String = defaultTarget.name
    var bem: Boolean = false
    var bemComments: Boolean = false
    var bemElementPrefix: String = "__"
    var bemModifierPrefix: String = "_"
    
    fun languageValue() = TargetLanguage.valueOf(language)
    fun targetValue() = Target.valueOf(target)
}

fun getInstance(project: Project): ExtractCSSSettings {
    return project.getService(ExtractCSSSettings::class.java)
}

@State(name = "ExtractCSSSettings", storages = [Storage("extract-css.xml")])
class ExtractCSSSettings : PersistentStateComponent<ExtractState> {
    private var innerState = ExtractState()

    override fun getState(): ExtractState = innerState
    override fun loadState(state: ExtractState) {
        innerState = state
    }
}