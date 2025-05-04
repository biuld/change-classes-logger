package com.github.biuld.changeclassseslogger.view

import com.intellij.openapi.Disposable
import javax.swing.JComponent

abstract class BasePanel : Disposable {
    abstract fun getContent(): JComponent
    override fun dispose() {}
} 