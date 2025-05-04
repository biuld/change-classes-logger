package com.github.biuld.changeclassseslogger.listener

import com.github.biuld.changeclassseslogger.state.ChangedClassesState
import com.intellij.debugger.impl.DebuggerManagerListener
import com.intellij.debugger.impl.DebuggerSession
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project

class DebuggerSessionListener(project: Project) : DebuggerManagerListener {
    private val logger = Logger.getInstance(this::class.java)
    private val state = project.getService(ChangedClassesState::class.java)

    override fun sessionCreated(session: DebuggerSession) {
        val timestamp = System.currentTimeMillis()
        logger.info("${session.sessionName} created at $timestamp")
        state.setSessionTimestamp(session, timestamp)
    }

    override fun sessionRemoved(session: DebuggerSession) {
        logger.info("${session.sessionName} removed")
        state.removeSessionTimestamp(session)
    }
}