package com.github.biuld.changeclassseslogger.model

import com.intellij.openapi.vcs.FileStatus

data class ClassFileInfo(
    val path: String,
    val qualifiedName: String,
    val timestamp: Long,
    val fileStatus: FileStatus = FileStatus.NOT_CHANGED,
)