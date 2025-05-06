package com.github.biuld.changeclassseslogger.service

interface NotificationService {
    fun showInfo(message: String, title: String = "Info")
    fun showError(message: String, title: String = "Error")
} 