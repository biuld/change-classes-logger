package com.github.biuld.changeclassseslogger.model

data class ClassFileInfo(
    val path: String,
    val qualifiedName: String,
    val timestamp: Long,
    val fileStatus: ClassFileStatus
)