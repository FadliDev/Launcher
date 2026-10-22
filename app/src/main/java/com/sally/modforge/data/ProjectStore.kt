package com.sally.modforge.data

import android.content.Context
import com.sally.modforge.model.Project
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** Semua data project ada di filesDir/projects/<id>/{project.json, repo/, artifacts/}. Save = lokal saja. */
class ProjectStore(ctx: Context) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }
    private val root = File(ctx.filesDir, "projects").apply { mkdirs() }
    private val skip = setOf(".git", ".gradle", "build", "run")

    fun dir(p: Project) = File(root, p.id)
    fun repoDir(p: Project) = File(dir(p), "repo")

    fun list(): List<Project> = root.listFiles().orEmpty().mapNotNull {
        runCatching { json.decodeFromString<Project>(File(it, "project.json").readText()) }.getOrNull()
    }.sortedByDescending { it.createdAt }

    fun save(p: Project) {
        dir(p).mkdirs()
        File(dir(p), "project.json").writeText(json.encodeToString(p))
    }

    fun delete(p: Project) { dir(p).deleteRecursively() }

    private fun walk(p: Project): List<Pair<String, File>> {
        val base = repoDir(p)
        return base.walkTopDown().onEnter { it.name !in skip }
            .filter { it.isFile }
            .map { it.relativeTo(base).invariantSeparatorsPath to it }
            .sortedBy { it.first }.toList()
    }

    fun files(p: Project): List<String> = walk(p).map { it.first }
    fun readAll(p: Project): Map<String, ByteArray> = walk(p).associate { it.first to it.second.readBytes() }
    fun read(p: Project, rel: String): String = File(repoDir(p), rel).readText()
    fun write(p: Project, rel: String, text: String) {
        File(repoDir(p), rel).apply { parentFile?.mkdirs(); writeText(text) }
    }
}
