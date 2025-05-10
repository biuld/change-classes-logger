package com.github.biuld.changeclassseslogger.service

import com.github.biuld.changeclassseslogger.model.ClassFileInfo

interface FileScannerService {
    suspend fun scanFiles(): List<ClassFileInfo>
}