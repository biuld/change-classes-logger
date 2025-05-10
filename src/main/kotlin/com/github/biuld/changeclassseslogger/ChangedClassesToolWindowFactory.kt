package com.github.biuld.changeclassseslogger

import com.github.biuld.changeclassseslogger.controller.ChangedClassesController
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class ChangedClassesToolWindowFactory : ToolWindowFactory, Disposable {
    private var controller: ChangedClassesController? = null

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        controller = project.getService(ChangedClassesController::class.java)
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(controller?.getContent(), "", false)
        toolWindow.contentManager.addContent(content)
    }

    override fun dispose() {
        controller?.dispose()
        controller = null
    }
} 