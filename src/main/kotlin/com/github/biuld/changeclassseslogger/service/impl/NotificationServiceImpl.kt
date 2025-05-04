package com.github.biuld.changeclassseslogger.service.impl

import com.github.biuld.changeclassseslogger.service.NotificationService
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import javax.swing.SwingUtilities
import com.intellij.openapi.diagnostic.Logger

class NotificationServiceImpl(private val project: Project) : NotificationService {

    override fun showInfo(message: String, title: String) {
        SwingUtilities.invokeLater {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("changed-classes-logger.notifications")
                .createNotification(
                    title,
                    message,
                    NotificationType.INFORMATION
                )
                .notify(project)
        }
    }

    override fun showError(message: String, title: String) {
        SwingUtilities.invokeLater {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("changed-classes-logger.notifications")
                .createNotification(
                    title,
                    message,
                    NotificationType.ERROR
                )
                .notify(project)
        }
    }
} 