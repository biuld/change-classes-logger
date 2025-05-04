package com.github.biuld.changeclassseslogger.view

import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JComponent

class RightPanel(
    private val searchPanel: JComponent,
    private val tablePanel: JComponent,
    private val countPanel: JComponent
) : BasePanel() {
    private val panel = JBPanel<Nothing>(BorderLayout()).apply {
        border = JBUI.Borders.empty(4)
    }

    init {
        panel.add(searchPanel, BorderLayout.NORTH)
        panel.add(tablePanel, BorderLayout.CENTER)
        panel.add(countPanel, BorderLayout.SOUTH)
    }

    override fun getContent() = panel

    override fun dispose() {
        panel.removeAll()
    }
} 