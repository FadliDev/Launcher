package com.sally.modforge.model

import kotlinx.serialization.json.Json
import java.io.File

/**
 * Penyimpanan project di storage privat app: <root>/<id>/project.json.
 * Penulisan lewat file sementara lalu rename, supaya file tidak rusak saat app ditutup mendadak.
 * Pemanggil harus menjalankan operasi ini di Dispatchers.IO.
 */
class ProjectStore(private val root: File) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun list(): List<ProjectConfig> =
        root.listFiles { f -> f.isDirectory }
            ?.mapNotNull { read(File(it, FILE_NAME)) }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()

    fun get(id: String): ProjectConfig? = read(File(File(root, id), FILE_NAME))

    fun save(config: ProjectConfig) {
        val dir = File(root, config.id).apply { mkdirs() }
        val target = File(dir, FILE_NAME)
        val tmp = File(dir, "$FILE_NAME.tmp")
        tmp.writeText(json.encodeToString(config))
        if (!tmp.renameTo(target)) {
            target.delete()
            tmp.renameTo(target)
        }
    }

    fun delete(id: String): Boolean = File(root, id).deleteRecursively()

    private fun read(file: File): ProjectConfig? = runCatching {
        if (file.exists()) json.decodeFromString<ProjectConfig>(file.readText()) else null
    }.getOrNull()

    companion object {
        const val FILE_NAME = "project.json"
    }
}
