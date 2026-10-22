package com.sally.modforge.data

import android.content.Context
import com.sally.modforge.core.FabricTemplateGenerator
import org.json.JSONObject
import java.io.File

class LocalProjectStore(context: Context) {
    private val root = File(context.filesDir, "projects").apply { mkdirs() }

    @Synchronized
    fun loadProjects(): List<ModProject> = root.listFiles()
        .orEmpty()
        .asSequence()
        .filter { it.isDirectory }
        .mapNotNull { dir ->
            runCatching { File(dir, "project.json").readText().let { ModProject.fromJson(JSONObject(it)) } }.getOrNull()
        }
        .sortedByDescending { it.createdAt }
        .toList()

    @Synchronized
    fun createProject(project: ModProject): ModProject {
        val dir = projectDir(project.id)
        if (!dir.exists() && !dir.mkdirs()) error("Tidak bisa membuat folder project")
        FabricTemplateGenerator.generate(project, dir)
        saveProject(project)
        return project
    }

    @Synchronized
    fun saveProject(project: ModProject) {
        val dir = projectDir(project.id)
        if (!dir.exists() && !dir.mkdirs()) error("Tidak bisa membuat folder project")
        File(dir, "project.json").writeText(project.toJson().toString(2))
    }

    @Synchronized
    fun deleteProject(id: String) {
        projectDir(id).deleteRecursively()
    }

    fun projectDir(id: String): File = File(root, id)

    fun readSourceFiles(project: ModProject): Map<String, ByteArray> {
        val base = projectDir(project.id)
        if (!base.exists()) return emptyMap()
        val result = linkedMapOf<String, ByteArray>()
        base.walkTopDown().filter { it.isFile }.forEach { file ->
            val relative = file.relativeTo(base).invariantSeparatorsPath
            if (relative == "project.json" || relative.startsWith(".modforge/") || relative.startsWith(".git/")) return@forEach
            result[relative] = file.readBytes()
        }
        return result
    }

    fun listSourceFiles(project: ModProject): List<String> = readSourceFiles(project).keys.sorted()

    fun readText(project: ModProject, path: String): String {
        require(isSafeRelativePath(path)) { "Invalid file path" }
        return File(projectDir(project.id), path).readText()
    }

    fun writeText(project: ModProject, path: String, content: String) {
        require(isSafeRelativePath(path)) { "Invalid file path" }
        val target = File(projectDir(project.id), path)
        target.parentFile?.mkdirs()
        target.writeText(content)
    }

    private fun isSafeRelativePath(path: String): Boolean =
        path.isNotBlank() && !path.startsWith('/') && path.split('/').none { it == ".." || it.isBlank() }
}
