package com.github.biuld.changeclassseslogger.service

import com.github.biuld.changeclassseslogger.model.ClassFileInfo

interface HotSwapService {
    fun reloadFile(f: ClassFileInfo)
}