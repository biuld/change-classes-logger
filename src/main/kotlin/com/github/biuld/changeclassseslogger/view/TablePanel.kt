package com.github.biuld.changeclassseslogger.view

import com.github.biuld.changeclassseslogger.model.ClassFileInfo
import com.intellij.openapi.vcs.FileStatus
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import java.awt.Component
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

class TablePanel(
    private val onFileSelected: (ClassFileInfo, MouseEvent) -> Unit
) : BasePanel() {
    private val panel = JBScrollPane()
    private val table: JBTable
    private val tableModel: DefaultTableModel
    private var files: List<ClassFileInfo> = emptyList()
    private val mouseListener: MouseAdapter

    init {
        tableModel = DefaultTableModel().apply {
            addColumn("File Name")
        }
        mouseListener = createTableMouseListener()
        table = JBTable(tableModel).apply {
            setShowGrid(false)
            intercellSpacing = JBUI.emptySize()
            setDefaultEditor(Object::class.java, null)
            addMouseListener(mouseListener)
            setDefaultRenderer(Object::class.java, VcsChangedRenderer())
            rowHeight = JBUI.scale(24) // 增加行高以更好地显示标记
        }
        panel.viewport.view = table
        panel.border = JBUI.Borders.customLine(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground())
    }

    fun updateFiles(newFiles: List<ClassFileInfo>) {
        files = newFiles
        tableModel.rowCount = 0
        files.forEach { file ->
            tableModel.addRow(arrayOf(file))
        }
    }

    private fun createTableMouseListener(): MouseAdapter {
        return object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (e.isPopupTrigger) {
                    handlePopup(e)
                }
            }

            override fun mouseReleased(e: MouseEvent) {
                if (e.isPopupTrigger) {
                    handlePopup(e)
                }
            }
        }
    }

    private fun handlePopup(e: MouseEvent) {
        val row = table.rowAtPoint(e.point)
        if (row in files.indices) {
            table.setRowSelectionInterval(row, row)
            onFileSelected(files[row], e)
        }
    }

    override fun getContent(): JComponent = panel

    override fun dispose() {
        table.removeMouseListener(mouseListener)
        tableModel.rowCount = 0
    }

    private inner class VcsChangedRenderer : DefaultTableCellRenderer() {
        private val defaultBorder = JBUI.Borders.empty(0, 4)

        override fun getTableCellRendererComponent(
            table: JTable?,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): Component {
            val file = value as? ClassFileInfo
            val component = super.getTableCellRendererComponent(table, file?.qualifiedName, isSelected, hasFocus, row, column)
            
            val status = file?.fileStatus ?: FileStatus.NOT_CHANGED
            val statusColor = when (status) {
                FileStatus.MODIFIED -> FileStatus.MODIFIED.color
                FileStatus.ADDED -> FileStatus.ADDED.color
                else -> null
            }

            border = if (statusColor != null) {
                JBUI.Borders.compound(
                    JBUI.Borders.customLine(statusColor, 0, 4, 0, 0),
                    JBUI.Borders.empty(0, 4)
                )
            } else {
                defaultBorder
            }
            
            return component
        }
    }
} 