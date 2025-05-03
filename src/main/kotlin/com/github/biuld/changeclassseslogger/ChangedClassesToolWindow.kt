package com.github.biuld.changeclassseslogger

import com.intellij.debugger.DebuggerManagerEx
import com.intellij.debugger.ui.HotSwapUI
import com.intellij.ide.DataManager
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupFactory.ActionSelectionAid
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.*
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import com.intellij.xdebugger.impl.hotswap.HotSwapSessionManager
import com.intellij.xdebugger.impl.hotswap.HotSwapVisibleStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.DefaultTableModel

class ChangedClassesToolWindow(private val project: Project) : Disposable {
    private val mainPanel = OnePixelSplitter(false, 0.05f)
    private val leftPanel = JBPanel<Nothing>().apply { 
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = JBUI.Borders.empty(4)
    }
    private val rightPanel = JBPanel<Nothing>(BorderLayout()).apply {
        border = JBUI.Borders.empty(4)
    }
    private val table: JBTable
    private val tableModel: DefaultTableModel
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val logger = Logger.getInstance(ChangedClassesToolWindow::class.java)
    private val countLabel = JBLabel()
    private val searchField = JBTextField()
    private var currentFiles: List<VirtualFile> = emptyList()
    private var filteredFiles: List<VirtualFile> = emptyList()

    init {
        // 左侧只放刷新按钮
        leftPanel.add(createRefreshButtonPanel())

        // 右侧：上为搜索框，中为表格，下为计数
        rightPanel.add(createSearchPanel(), BorderLayout.NORTH)
        table = createTable()
        tableModel = table.model as DefaultTableModel
        rightPanel.add(createTablePanel(), BorderLayout.CENTER)
        rightPanel.add(createCountLabelPanel(), BorderLayout.SOUTH)

        mainPanel.firstComponent = leftPanel
        mainPanel.secondComponent = rightPanel
        mainPanel.setResizeEnabled(false)
    }

    private fun createRefreshButtonPanel(): JBPanel<Nothing> {
        val panel = JBPanel<Nothing>()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        val refreshBtn = JButton("Refresh")
        refreshBtn.maximumSize = JBUI.size(Int.MAX_VALUE, 40)
        refreshBtn.alignmentX = 0.5f
        refreshBtn.addActionListener { refreshChangedClasses() }
        panel.add(refreshBtn)
        panel.add(JBBox.createVerticalGlue())
        return panel
    }

    private fun createSearchPanel(): JBPanel<Nothing> {
        val panel = JBPanel<Nothing>(BorderLayout())
        searchField.putClientProperty("JTextField.placeholderText", "请输入文件名")
        searchField.maximumSize = JBUI.size(Int.MAX_VALUE, searchField.preferredSize.height)
        searchField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = filterFilesAndUpdateTable()
            override fun removeUpdate(e: DocumentEvent?) = filterFilesAndUpdateTable()
            override fun changedUpdate(e: DocumentEvent?) = filterFilesAndUpdateTable()
        })
        panel.add(searchField, BorderLayout.CENTER)
        return panel
    }

    private fun createTablePanel(): JBScrollPane {
        val scrollPane = JBScrollPane(table)
        scrollPane.border = JBUI.Borders.customLine(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground())
        return scrollPane
    }

    private fun createCountLabelPanel(): JBPanel<Nothing> {
        val panel = JBPanel<Nothing>(BorderLayout())
        panel.add(countLabel, BorderLayout.WEST)
        panel.border = JBUI.Borders.empty(4, 0, 0, 0)
        return panel
    }

    private fun createTable(): JBTable {
        return JBTable(
            DefaultTableModel().apply {
                addColumn("文件名")
            }
        ).apply {
            setShowGrid(false)
            intercellSpacing = JBUI.emptySize()
            setDefaultEditor(Object::class.java, null)
            addMouseListener(createTableMouseListener())
        }
    }

    fun getContent(): JComponent {
        mainPanel.putClientProperty("ChangedClassesToolWindow", this)
        return mainPanel
    }

    fun updateChangedClasses(changes: Set<VirtualFile>) {
        logger.info("got ${changes.size} changes")
        SwingUtilities.invokeLater {
            currentFiles = changes.toList().sortedBy { it.path }
            filterFilesAndUpdateTable()
            countLabel.text = "共 ${changes.size} 个变更"
        }
    }

    private fun filterFilesAndUpdateTable() {
        if (currentFiles.isEmpty()) {
            logger.info("No files to filter, do nothing")
            return
        }
        val keyword = searchField.text.trim()
        filteredFiles = if (keyword.isEmpty()) {
            currentFiles
        } else {
            currentFiles.filter { it.path.contains(keyword, ignoreCase = true) }
        }
        tableModel.rowCount = 0
        filteredFiles.forEach { file ->
            tableModel.addRow(arrayOf(file.path))
        }
    }

    private fun refreshChangedClasses() {
        logger.info("refreshing, scope isActive:[${coroutineScope.isActive}]")
        coroutineScope.launch {
            try {
                val sessionManager = HotSwapSessionManager.getInstance(project)
                val state = withTimeout(3000) {
                    sessionManager.currentStatusFlow.first()
                }
                if (state == null) {
                    showInfo("未检测到可用的 HotSwap 状态。")
                    return@launch
                }
                val session = state.session
                val status = state.status
                when (status) {
                    HotSwapVisibleStatus.CHANGES_READY -> {
                        val changes = session.getChanges() as? Set<VirtualFile> ?: emptySet()
                        if (changes.isEmpty()) {
                            showInfo("没有检测到类的变更。")
                        }
                        updateChangedClasses(changes)
                    }
                    else -> {
                        showInfo("未检测到变更：HotSwap[${status.name}]")
                    }
                }
            } catch (_: TimeoutCancellationException) {
                showInfo("获取HotSwap状态超时")
            } catch (e: Exception) {
                showInfo("发生异常：${e.message}")
            }
        }
    }

    private fun createTableMouseListener(): MouseAdapter {
        return object : MouseAdapter() {
            private fun showPopup(e: MouseEvent) {
                val row = table.rowAtPoint(e.point)
                if (row in filteredFiles.indices) {
                    table.setRowSelectionInterval(row, row)
                    val actionGroup = DefaultActionGroup().apply {
                        add(object : AnAction("打开文件") {
                            override fun actionPerformed(event: AnActionEvent) {
                                val file = filteredFiles[row]
                                FileEditorManager.getInstance(project).openFile(file, true)
                            }
                        })
                        add(object : AnAction("HotSwap") {
                            override fun actionPerformed(event: AnActionEvent) {
                                val file = filteredFiles[row]

                                val debuggerManager = DebuggerManagerEx.getInstanceEx(project)
                                val sessions = debuggerManager.sessions

                                if (sessions.isEmpty()) {
                                    showInfo("cannot find any debugger session")
                                } else {
                                    val session = sessions.first()
                                    HotSwapUI.getInstance(project).compileAndReload(session, file)
                                }
                            }
                        })
                    }
                    val popup = JBPopupFactory.getInstance().createActionGroupPopup(
                        "",
                        actionGroup,
                        DataManager.getInstance().getDataContext(e.component),
                        ActionSelectionAid.SPEEDSEARCH,
                        true
                    )
                    popup.showInScreenCoordinates(e.component, e.locationOnScreen)
                }
            }
            override fun mousePressed(e: MouseEvent) {
                if (e.isPopupTrigger) {
                    showPopup(e)
                }
            }
            override fun mouseReleased(e: MouseEvent) {
                if (e.isPopupTrigger) {
                    showPopup(e)
                }
            }
        }
    }

    private fun showInfo(message: String, title: String = "提示") {
        logger.info("[$title] $message")
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

    override fun dispose() {
        coroutineScope.cancel()
    }
} 