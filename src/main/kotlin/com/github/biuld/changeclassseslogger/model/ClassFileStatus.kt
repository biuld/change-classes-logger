package com.github.biuld.changeclassseslogger.model

import com.intellij.openapi.vcs.FileStatus
import com.intellij.ui.JBColor
import java.awt.Color

enum class ClassFileStatus(val color: Color?) {
    MODIFIED(FileStatus.MODIFIED.color),
    ADDED(FileStatus.ADDED.color),
    // Custom color for newer than debug session (using a purple color)
    NEWER_THAN_DEBUG_SESSION(JBColor(
        Color(147, 112, 219),  // Medium purple for light theme
        Color(186, 104, 200)   // Light purple for dark theme
    )),
    NOT_CHANGED(FileStatus.NOT_CHANGED.color);

    companion object {
        fun fromVcsStatus(status: FileStatus): ClassFileStatus {
            return when (status) {
                FileStatus.MODIFIED -> MODIFIED
                FileStatus.ADDED -> ADDED
                else -> NOT_CHANGED
            }
        }
    }
} 