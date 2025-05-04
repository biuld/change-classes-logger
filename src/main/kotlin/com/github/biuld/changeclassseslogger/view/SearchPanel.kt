package com.github.biuld.changeclassseslogger.view

import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class SearchPanel(private val onSearch: (String) -> Unit) : BasePanel() {
    private val panel = JBPanel<Nothing>(BorderLayout())
    private val searchField = JBTextField()
    private val documentListener = object : DocumentListener {
        override fun insertUpdate(e: DocumentEvent?) = onSearch(searchField.text)
        override fun removeUpdate(e: DocumentEvent?) = onSearch(searchField.text)
        override fun changedUpdate(e: DocumentEvent?) = onSearch(searchField.text)
    }

    init {
        searchField.putClientProperty("JTextField.placeholderText", "请输入文件名")
        searchField.maximumSize = JBUI.size(Int.MAX_VALUE, searchField.preferredSize.height)
        searchField.document.addDocumentListener(documentListener)
        panel.add(searchField, BorderLayout.CENTER)
    }

    override fun getContent(): JComponent = panel

    override fun dispose() {
        searchField.document.removeDocumentListener(documentListener)
    }
} 