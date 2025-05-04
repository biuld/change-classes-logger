package com.github.biuld.changeclassseslogger.service

interface NotificationService {
    fun showInfo(message: String, title: String = "提示")
    fun showError(message: String, title: String = "错误")
} 