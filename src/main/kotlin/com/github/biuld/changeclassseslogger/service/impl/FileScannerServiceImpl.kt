package com.github.biuld.changeclassseslogger.service.impl

import com.github.biuld.changeclassseslogger.model.ClassFileInfo
import com.github.biuld.changeclassseslogger.service.FileScannerService
import com.github.biuld.changeclassseslogger.state.ChangedClassesState
import com.intellij.debugger.impl.DebuggerSession
import com.intellij.openapi.application.readAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.openapi.util.io.toCanonicalPath
import com.intellij.openapi.vcs.FileStatus
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.nio.file.Path
import kotlin.io.path.walk

class FileScannerServiceImpl(private val project: Project) :
    FileScannerService {
    private val logger = Logger.getInstance(this::class.java)
    private val CLASS_EXTENSION: String = ".class"
    private val SOURCE_FILE_EXTENSIONS = setOf(".java", ".kt", ".scala")
    private val state = project.getService(ChangedClassesState::class.java)
    private val changeListManager = ChangeListManager.getInstance(project)
    private val psiManager = PsiManager.getInstance(project)

    override suspend fun scanFiles(session: DebuggerSession): List<ClassFileInfo> = withContext(Dispatchers.IO) {
        logger.info("Starting to scan project files")
        state.startScan()
        try {
            val classpath = OrderEnumerator.orderEntries(project).classes().roots
                .filter { it.path.startsWith(project.basePath!!) }
                .distinctBy { it.path }
                .map { it.path }

            // 获取所有修改的类名及其状态
            val modifiedClasses = getModifiedClasses()

            val files = classpath.map { rootPath ->
                async {
                    val fs = Path.of(rootPath).walk().toList()
                        .filter { it.toCanonicalPath().endsWith(CLASS_EXTENSION) }
                        .filter { p ->
                            state.getSessionTimestamp(session)?.let { p.toFile().lastModified() > it } ?: true
                        }
                        .map { it ->
                            val canonicalPath = it.toCanonicalPath()
                            val qualifiedName = canonicalPath.substring(
                                rootPath.length + 1,
                                canonicalPath.length - CLASS_EXTENSION.length
                            ).replace("/", ".")

                            val cf = ClassFileInfo(
                                path = canonicalPath,
                                qualifiedName = qualifiedName,
                                fileStatus = modifiedClasses[qualifiedName] ?: FileStatus.NOT_CHANGED
                            )
                            cf
                        }
                    logger.info("Scanned $rootPath, total ${fs.size} files")
                    fs
                }
            }.awaitAll()
                .flatten()
                .distinctBy { it.path }

            files
        } catch (e: Exception) {
            logger.error("Error occurred while scanning files", e)
            throw e
        } finally {
            state.stopScan()
        }
    }

    private fun getAllClasses(psiClass: PsiClass): List<PsiClass> {
        return listOf(psiClass) + psiClass.innerClasses.flatMap { getAllClasses(it) }
    }

    private suspend fun getModifiedClasses(): Map<String, FileStatus> {
        return changeListManager.allChanges.flatMap { change ->
            val vf = change.virtualFile

            if (vf == null || SOURCE_FILE_EXTENSIONS.none { vf.path.endsWith(it) })
                return@flatMap emptyList()

            val psiFile = readAction {
                psiManager.findFile(vf)
            }

            if (psiFile == null) {
                return@flatMap emptyList()
            }

            readAction {
                psiFile.children
                    .filterIsInstance<PsiClass>()
                    .flatMap { getAllClasses(it) }
                    .filter { it.qualifiedName != null }
                    .map { it.qualifiedName!! to change.fileStatus }
            }
        }.toMap()
    }
} 