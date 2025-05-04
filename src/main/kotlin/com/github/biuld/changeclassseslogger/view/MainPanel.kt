package com.github.biuld.changeclassseslogger.view

import com.intellij.ui.OnePixelSplitter
import com.intellij.util.ui.JBUI
import javax.swing.JComponent

class MainPanel(
    private val leftPanel: JComponent,
    private val rightPanel: JComponent
) : BasePanel() {
    private val panel = OnePixelSplitter(false, 0.05f)

    init {
        panel.firstComponent = leftPanel
        panel.secondComponent = rightPanel
        panel.setResizeEnabled(false)
    }

    override fun getContent(): JComponent {
        return panel
    }

    override fun dispose() {
        panel.firstComponent = null
        panel.secondComponent = null
    }
} 