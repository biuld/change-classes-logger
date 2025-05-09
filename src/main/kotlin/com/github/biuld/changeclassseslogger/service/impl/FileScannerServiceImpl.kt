package com.github.biuld.changeclassseslogger.service.impl

import com.github.biuld.changeclassseslogger.model.ClassFileInfo
import com.github.biuld.changeclassseslogger.model.ClassFileStatus
import com.github.biuld.changeclassseslogger.service.FileScannerService
import com.github.biuld.changeclassseslogger.state.ChangedClassesState
import com.intellij.debugger.DebuggerManagerEx
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.openapi.util.io.toCanonicalPath
import com.intellij.openapi.vcs.FileStatus
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.psi.PsiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jetbrains.uast.UFile
import org.jetbrains.uast.toUElement
import java.nio.file.Path
import kotlin.io.path.walk

@Service(Service.Level.PROJECT)
class FileScannerServiceImpl(private val project: Project) : FileScannerService {
    private val logger = Logger.getInstance(this::class.java)
    private val CLASS_EXTENSION: String = ".class"
    private val SOURCE_FILE_EXTENSIONS = setOf(".java", ".kt", ".scala", ".groovy")
    private val state = project.getService(ChangedClassesState::class.java)
    private val changeListManager = ChangeListManager.getInstance(project)
    private val psiManager = PsiManager.getInstance(project)

    override suspend fun scanFiles(): List<ClassFileInfo> = withContext(Dispatchers.IO) {
        logger.info("Starting to scan project files")
        state.startScan()
        try {
            val classpath = OrderEnumerator.orderEntries(project).classes().roots
                .filter { it.path.startsWith(project.basePath!!) }
                .distinctBy { it.path }
                .map { it.path }

            val modifiedSourceFiles = getModifiedSourceFiles()

            val files = classpath.map { rootPath ->
                async {
                    val fs = Path.of(rootPath).walk().toList()
                        .filter { it.toCanonicalPath().endsWith(CLASS_EXTENSION) }
                        .map { it ->
                            val canonicalPath = it.toCanonicalPath()
                            val qualifiedName = canonicalPath.substring(
                                rootPath.length + 1,
                                canonicalPath.length - CLASS_EXTENSION.length
                            ).replace("/", ".")

                            val classTimestamp = it.toFile().lastModified()
                            val fileStatus = determineFileStatus(qualifiedName, classTimestamp, modifiedSourceFiles)

                            ClassFileInfo(
                                path = canonicalPath,
                                qualifiedName = qualifiedName,
                                timestamp = classTimestamp,
                                fileStatus = fileStatus
                            )
                        }
                    logger.info("Scanned $rootPath, total ${fs.size} files")
                    fs
                }
            }.awaitAll()
                .flatten()
                .distinctBy { it.path }
                .sortedWith(
                    compareByDescending<ClassFileInfo> { it.fileStatus != ClassFileStatus.NOT_CHANGED }
                        .thenBy { it.qualifiedName }
                )

            files
        } catch (e: Exception) {
            logger.error("Error occurred while scanning files", e)
            throw e
        } finally {
            state.stopScan()
        }
    }

    private fun determineFileStatus(
        qualifiedName: String,
        classTimestamp: Long,
        modifiedSourceFiles: Map<String, Pair<FileStatus, Long>>
    ): ClassFileStatus {
        // First check if the file is newer than debug session
        val debuggerManager = DebuggerManagerEx.getInstanceEx(project)
        val session = debuggerManager.context.debuggerSession
        if (session != null && session.isAttached) {
            val sessionTimestamp = state.getSessionTimestamp(session)
            if (sessionTimestamp != null && classTimestamp > sessionTimestamp) {
                return ClassFileStatus.NEWER_THAN_DEBUG_SESSION
            }
        }

        // If not newer than debug session, check VCS status
        return modifiedSourceFiles.entries
            .find { (sourceQualifiedName, _) -> qualifiedName.startsWith(sourceQualifiedName) }
            ?.let { (_, statusAndTimestamp) ->
                val (status, sourceTimestamp) = statusAndTimestamp
                if (classTimestamp > sourceTimestamp) ClassFileStatus.fromVcsStatus(status) else ClassFileStatus.NOT_CHANGED
            } ?: ClassFileStatus.NOT_CHANGED
    }

    private suspend fun getModifiedSourceFiles(): Map<String, Pair<FileStatus, Long>> {
        return changeListManager.allChanges.flatMap { change ->
            val vf = change.virtualFile

            if (vf == null) {
                return@flatMap emptyList()
            }

            if (SOURCE_FILE_EXTENSIONS.none { vf.path.endsWith(it) }) {
                return@flatMap emptyList()
            }

            val psiFile = readAction {
                psiManager.findFile(vf)
            }

            if (psiFile == null) {
                return@flatMap emptyList()
            }

            readAction {
                val uFile = psiFile.toUElement(UFile::class.java)
                if (uFile == null) {
                    return@readAction emptyList()
                }

                uFile.classes
                    .filter { it.qualifiedName != null }
                    .map { uClass ->
                        uClass.qualifiedName!! to (change.fileStatus to vf.timeStamp)
                    }
            }
        }.toMap()
    }
} 