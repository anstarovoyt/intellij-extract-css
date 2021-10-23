package com.github.anstarovoyt.intellijextractcss.services

import com.intellij.openapi.project.Project
import com.github.anstarovoyt.intellijextractcss.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
