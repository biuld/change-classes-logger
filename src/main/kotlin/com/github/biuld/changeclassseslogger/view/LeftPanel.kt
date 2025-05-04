package com.github.biuld.changeclassseslogger.view

import com.intellij.ui.components.JBBox
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent

class LeftPanel(
    private val onRefresh: () -> Unit,
) : BasePanel() {
    private val panel = JBPanel<Nothing>()
    private val refreshBtn = JButton("Refresh")

    init {
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)

        refreshBtn.maximumSize = JBUI.size(Int.MAX_VALUE, 40)
        refreshBtn.alignmentX = 0.5f
        refreshBtn.addActionListener { onRefresh() }
        panel.add(refreshBtn)

        panel.add(JBBox.createVerticalGlue())
    }

    override fun getContent(): JComponent = panel

    override fun dispose() {
        refreshBtn.removeActionListener(refreshBtn.actionListeners.firstOrNull())
    }
} 