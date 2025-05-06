package com.github.biuld.changeclassseslogger.view

import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JComponent

class CountPanel : BasePanel() {
    private val panel = JBPanel<Nothing>(BorderLayout())
    private val countLabel = JBLabel()

    init {
        panel.add(countLabel, BorderLayout.WEST)
        panel.border = JBUI.Borders.emptyTop(4)
    }

    fun updateCount(count: Int) {
        countLabel.text = "Total $count changes"
    }

    override fun getContent(): JComponent = panel

    override fun dispose() {
        countLabel.text = ""
    }
} 