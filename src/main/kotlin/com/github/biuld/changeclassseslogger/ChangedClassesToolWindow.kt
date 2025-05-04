package com.github.biuld.changeclassseslogger

import com.github.biuld.changeclassseslogger.controller.ChangedClassesController
import com.github.biuld.changeclassseslogger.service.NotificationService
import com.github.biuld.changeclassseslogger.service.impl.NotificationServiceImpl
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import javax.swing.JComponent

class ChangedClassesToolWindow(project: Project) : Disposable {
    private val notificationService: NotificationService = NotificationServiceImpl(project)
    private val controller = ChangedClassesController(project, notificationService)

    fun getContent(): JComponent {
        return controller.getContent()
    }

    override fun dispose() {
        controller.dispose()
    }
} 