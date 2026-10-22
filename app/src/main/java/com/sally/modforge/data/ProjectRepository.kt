package com.sally.modforge.data

import android.content.Context
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.json.JSONObject

class ProjectRepository(context: Context) {
    private val root = File(context.filesDir, "modforge/projects").apply { mkdirs() }

    fun listProjects(): List<ModProject> = root.listFiles()
        .orEmpty()
        .filter { File(it, "project.json").isFile }
        .mapNotNull { folder -> runCatching { readMetadata(folder) }.getOrNull() }
        .sortedByDescending { it.updatedAt }

    fun createProject(draft: ModProject): ModProject {
        val id = UUID.randomUUID().toString()
        val project = draft.copy(id = id, createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis())
        val folder = projectFolder(id).apply { mkdirs() }
        ModTemplateGenerator.generate(folder, project)
        writeMetadata(project)
        return project
    }

    fun updateProject(project: ModProject): ModProject {
        val updated = project.copy(updatedAt = System.currentTimeMillis())
        writeMetadata(updated)
        return updated
    }

    fun deleteProject(id: String) {
        projectFolder(id).deleteRecursively()
    }

    fun listFiles(projectId: String): List<String> {
        val folder = projectFolder(projectId)
        if (!folder.isDirectory) return emptyList()
        return folder.walkTopDown()
            .filter { it.isFile }
            .filterNot { it.relativeTo(folder).invariantSeparatorsPath == "project.json" }
            .filterNot { file -> file.relativeTo(folder).path.split(File.separator).any { it.startsWith(".") && it in setOf(".git") } }
            .filterNot { file -> file.relativeTo(folder).path.split(File.separator).any { it in setOf("build", ".gradle") } }
            .map { it.relativeTo(folder).invariantSeparatorsPath }
            .sorted()
            .toList()
    }

    fun readFile(projectId: String, relativePath: String): String {
        val target = resolveSafe(projectId, relativePath) ?: return ""
        return if (target.isFile) target.readText() else ""
    }

    fun writeFile(projectId: String, relativePath: String, content: String) {
        val target = resolveSafe(projectId, relativePath) ?: return
        target.parentFile?.mkdirs()
        target.writeText(content)
        runCatching {
            val project = readMetadata(projectFolder(projectId))
            writeMetadata(project.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun exportZip(projectId: String, output: File): File {
        val folder = projectFolder(projectId)
        ZipOutputStream(output.outputStream().buffered()).use { zip ->
            folder.walkTopDown().filter { it.isFile }.forEach { file ->
                val relative = file.relativeTo(folder).invariantSeparatorsPath
                if (relative == "project.json" || relative.startsWith(".git/") || relative.startsWith("build/") || relative.startsWith(".gradle/")) return@forEach
                zip.putNextEntry(ZipEntry(relative))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        return output
    }

    fun projectDirectory(id: String): File = projectFolder(id)

    private fun projectFolder(id: String): File = File(root, id)

    private fun resolveSafe(projectId: String, relativePath: String): File? {
        if (relativePath.isBlank() || relativePath.startsWith("/") || relativePath.contains("\\")) return null
        val base = projectFolder(projectId).canonicalFile
        val target = File(base, relativePath).canonicalFile
        return target.takeIf { it.path.startsWith(base.path + File.separator) }
    }

    private fun writeMetadata(project: ModProject) {
        val folder = projectFolder(project.id)
        folder.mkdirs()
        File(folder, "project.json").writeText(project.toJson().toString(2))
    }

    private fun readMetadata(folder: File): ModProject = ModProject.fromJson(JSONObject(File(folder, "project.json").readText()))
}
