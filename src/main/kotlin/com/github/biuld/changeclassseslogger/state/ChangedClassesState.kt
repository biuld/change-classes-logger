package com.github.biuld.changeclassseslogger.state

import com.github.biuld.changeclassseslogger.model.ClassFileInfo
import com.intellij.debugger.impl.DebuggerSession
import com.intellij.openapi.components.Service
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Service(Service.Level.PROJECT)
class ChangedClassesState {
    private val _currentFiles = MutableStateFlow<List<ClassFileInfo>>(emptyList())
    val currentFiles: StateFlow<List<ClassFileInfo>> = _currentFiles.asStateFlow()
    
    private val _filteredFiles = MutableStateFlow<List<ClassFileInfo>>(emptyList())
    val filteredFiles: StateFlow<List<ClassFileInfo>> = _filteredFiles.asStateFlow()
    
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    
    private val _sessionTimestamps = MutableStateFlow<Map<DebuggerSession, Long>>(emptyMap())
    val sessionTimestamps: StateFlow<Map<DebuggerSession, Long>> = _sessionTimestamps.asStateFlow()
    
    fun updateFiles(files: List<ClassFileInfo>) {
        _currentFiles.value = files
        _filteredFiles.value = files
    }
    
    fun filterFiles(keyword: String) {
        _filteredFiles.value = if (keyword.isEmpty()) {
            _currentFiles.value
        } else {
            _currentFiles.value.filter { it.qualifiedName.contains(keyword) }
        }
    }
    
    fun startScan() { _isScanning.value = true }
    fun stopScan() { _isScanning.value = false }
    
    fun setSessionTimestamp(session: DebuggerSession, timestamp: Long) {
        _sessionTimestamps.value = _sessionTimestamps.value + (session to timestamp)
    }
    
    fun removeSessionTimestamp(session: DebuggerSession) {
        _sessionTimestamps.value = _sessionTimestamps.value - session
    }
    
    fun getSessionTimestamp(session: DebuggerSession): Long? {
        return _sessionTimestamps.value[session]
    }
    
    fun getLatestSessionTimestamp(): Long {
        return _sessionTimestamps.value.values.maxOrNull() ?: 0L
    }
    
    fun isInitialized(): Boolean = _sessionTimestamps.value.isNotEmpty()
} 