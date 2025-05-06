package com.github.biuld.changeclassseslogger.view

import com.github.biuld.changeclassseslogger.model.ClassFileInfo
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
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
        }
        panel.viewport.view = table
        panel.border = JBUI.Borders.customLine(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground())
    }

    fun updateFiles(newFiles: List<ClassFileInfo>) {
        files = newFiles
        tableModel.rowCount = 0
        files.forEach { file ->
            tableModel.addRow(arrayOf(file.qualifiedName))
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
} 