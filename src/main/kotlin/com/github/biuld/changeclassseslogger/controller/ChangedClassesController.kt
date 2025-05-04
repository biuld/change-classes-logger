package com.github.biuld.changeclassseslogger.controller

import com.github.biuld.changeclassseslogger.model.ClassFileInfo
import com.github.biuld.changeclassseslogger.service.FileScannerService
import com.github.biuld.changeclassseslogger.service.HotSwapService
import com.github.biuld.changeclassseslogger.service.NotificationService
import com.github.biuld.changeclassseslogger.service.impl.FileScannerServiceImpl
import com.github.biuld.changeclassseslogger.service.impl.HotSwapServiceImpl
import com.github.biuld.changeclassseslogger.state.ChangedClassesState
import com.github.biuld.changeclassseslogger.view.*
import com.intellij.debugger.DebuggerManagerEx
import com.intellij.ide.DataManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupFactory.ActionSelectionAid
import com.intellij.openapi.vfs.LocalFileSystem
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.awt.event.MouseEvent

class ChangedClassesController(
    private val project: Project,
    private val notificationService: NotificationService
) : Disposable {

    private val logger = Logger.getInstance(this::class.java)
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val state = project.getService(ChangedClassesState::class.java)
    private val fileScanner: FileScannerService = FileScannerServiceImpl(project)
    private val hotSwapper: HotSwapService = HotSwapServiceImpl(project, notificationService)

    // 视图组件
    private val leftPanel = LeftPanel { refreshChangedClasses() }
    private val searchPanel = SearchPanel { state.filterFiles(it) }
    private val tablePanel = TablePanel { f, e -> showFilePopup(f, e) }
    private val countPanel = CountPanel()
    private val rightPanel = RightPanel(
        searchPanel.getContent(),
        tablePanel.getContent(),
        countPanel.getContent()
    )
    private val mainPanel = MainPanel(
        leftPanel.getContent(),
        rightPanel.getContent()
    )

    init {
        logger.info("初始化ChangedClassesController")
        coroutineScope.launch {
            state.filteredFiles.collectLatest { files ->
                logger.debug("更新文件列表，当前文件数量：${files.size}")
                tablePanel.updateFiles(files)
                countPanel.updateCount(files.size)
            }
        }
    }

    fun getContent() = mainPanel.getContent()

    private fun refreshChangedClasses() {
        val debuggerManager = DebuggerManagerEx.getInstanceEx(project)
        val session = debuggerManager.context.debuggerSession

        if (!state.isInitialized() || session == null || !session.isAttached) {
            notificationService.showError("Cannot find debugger session")
            return
        }

        coroutineScope.launch {
            try {
                val files = fileScanner.scanFiles(session)

                if (files.isEmpty()) {
                    notificationService.showInfo("没有变更")
                } else {
                    state.updateFiles(files)
                }
            } catch (e: Exception) {
                logger.error("刷新文件列表时发生异常", e)
                notificationService.showError("发生异常：${e.message}")
            }
        }
    }

    private fun showFilePopup(f: ClassFileInfo, e: MouseEvent) {
        logger.debug("显示文件${f.path}的弹出菜单")
        val actionGroup = DefaultActionGroup().apply {
            add(object : AnAction("打开文件") {
                override fun actionPerformed(event: AnActionEvent) {
                    val virtualFile = LocalFileSystem.getInstance().findFileByPath(f.path)
                    if (virtualFile != null) {
                        FileEditorManager.getInstance(project).openFile(virtualFile, true)
                    } else {
                        notificationService.showError("无法找到文件：${f.path}")
                    }
                }
            })
            add(object : AnAction("HotSwap") {
                override fun actionPerformed(event: AnActionEvent) {
                    hotSwapper.reloadFile(f)
                }
            })
        }
        val popup = JBPopupFactory.getInstance().createActionGroupPopup(
            "",
            actionGroup,
            DataManager.getInstance().getDataContext(mainPanel.getContent()),
            ActionSelectionAid.SPEEDSEARCH,
            true
        )
        popup.showInScreenCoordinates(e.component, e.locationOnScreen)
    }

    override fun dispose() {
        logger.info("清理ChangedClassesController资源")
        coroutineScope.cancel()
        leftPanel.dispose()
        searchPanel.dispose()
        tablePanel.dispose()
        countPanel.dispose()
        rightPanel.dispose()
        mainPanel.dispose()
    }
} 