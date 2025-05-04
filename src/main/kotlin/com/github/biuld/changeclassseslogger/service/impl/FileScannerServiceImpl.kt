package com.github.biuld.changeclassseslogger.service.impl

import com.github.biuld.changeclassseslogger.model.ClassFileInfo
import com.github.biuld.changeclassseslogger.service.FileScannerService
import com.github.biuld.changeclassseslogger.state.ChangedClassesState
import com.intellij.debugger.impl.DebuggerSession
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.openapi.util.io.toCanonicalPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.walk

class FileScannerServiceImpl(private val project: Project) :
    FileScannerService {
    private val logger = Logger.getInstance(this::class.java)
    private val CLASS_EXTENSION: String = ".class"
    private val state = project.getService(ChangedClassesState::class.java)

    override suspend fun scanFiles(session: DebuggerSession): List<ClassFileInfo> = withContext(Dispatchers.IO) {
        logger.info("开始扫描项目文件")
        state.startScan()
        try {
            val classpath = OrderEnumerator.orderEntries(project).classes().roots
                .filter { it.path.startsWith(project.basePath!!) }
                .distinctBy { it.path }
                .map { it.path }

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

                            ClassFileInfo(canonicalPath, qualifiedName)
                        }
                    logger.info("扫描 $rootPath，共${fs.size}个文件")
                    fs
                }
            }.awaitAll()
                .flatten()
                .distinctBy { it.path }

            files
        } catch (e: Exception) {
            logger.error("扫描文件时发生异常", e)
            throw e
        } finally {
            state.stopScan()
        }
    }

    private fun calculateMd5(path: Path, md5Digest: MessageDigest): String {
        md5Digest.reset()
        return md5Digest.digest(Files.readAllBytes(path)).joinToString("") { "%02x".format(it) }
    }
} 