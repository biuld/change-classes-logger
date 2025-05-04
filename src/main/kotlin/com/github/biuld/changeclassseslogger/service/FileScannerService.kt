package com.github.biuld.changeclassseslogger.service

import com.github.biuld.changeclassseslogger.model.ClassFileInfo
import com.intellij.debugger.impl.DebuggerSession

interface FileScannerService {
    suspend fun scanFiles(session: DebuggerSession): List<ClassFileInfo>
}