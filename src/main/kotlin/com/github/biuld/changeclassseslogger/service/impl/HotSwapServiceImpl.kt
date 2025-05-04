package com.github.biuld.changeclassseslogger.service.impl

import com.github.biuld.changeclassseslogger.model.ClassFileInfo
import com.github.biuld.changeclassseslogger.service.HotSwapService
import com.github.biuld.changeclassseslogger.service.NotificationService
import com.intellij.debugger.DebuggerManagerEx
import com.intellij.debugger.impl.DebuggerSession
import com.intellij.debugger.impl.HotSwapFile
import com.intellij.debugger.impl.HotSwapManager
import com.intellij.debugger.ui.HotSwapProgressImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import java.io.File

class HotSwapServiceImpl(
    private val project: Project,
    private val notificationService: NotificationService,
) : HotSwapService {
    private val logger = Logger.getInstance(this::class.java)

    override fun reloadFile(f: ClassFileInfo) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val debuggerManager = DebuggerManagerEx.getInstanceEx(project)
            val session = debuggerManager.context.debuggerSession

            if (session == null || !session.isAttached) {
                notificationService.showError("cannot find debugger session")
                return@executeOnPooledThread
            }

            // 获取当前选中的文件
            val hotswapFile = HotSwapFile(File(f.path))

            ApplicationManager.getApplication().invokeLater({
                val progress = HotSwapProgressImpl(project)
                progress.setSessionForActions(session)
                // 创建当前会话的修改类映射
                val modifiedClasses = mapOf(session to mapOf(f.qualifiedName to hotswapFile))

                logger.info("HotSwapping session:[${session.sessionName}], state:[${session.state}]")

                // 执行热重载
                ApplicationManager.getApplication().executeOnPooledThread {
                    reloadModifiedClasses(modifiedClasses, progress)
                }
            }, ModalityState.nonModal())
        }
    }

    private fun reloadModifiedClasses(
        modifiedClasses: Map<DebuggerSession, Map<String, HotSwapFile>>,
        progress: HotSwapProgressImpl
    ) {
        ProgressManager.getInstance().runProcess({
            HotSwapManager.reloadModifiedClasses(modifiedClasses, progress)
            progress.finished()
            notificationService.showInfo("HotSwapped")
        }, progress.progressIndicator)
    }
}