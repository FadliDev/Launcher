package com.sally.modforge.data

import org.json.JSONObject
import java.util.UUID

data class VersionProfile(
    val minecraft: String,
    val java: Int,
    val gradle: String,
    val loom: String,
    val yarn: String,
    val loader: String,
    val fabricApi: String,
    val experimental: Boolean = false
)

object VersionMatrix {
    val profiles = listOf(
        VersionProfile("1.21.1", 21, "8.10.2", "1.7-SNAPSHOT", "1.21.1+build.3", "0.16.5", "0.102.0+1.21.1"),
        VersionProfile("1.20.1", 17, "8.5", "1.3-SNAPSHOT", "1.20.1+build.10", "0.14.21", "0.92.2+1.20.1"),
        VersionProfile("1.18.2", 17, "7.6.4", "1.2-SNAPSHOT", "1.18.2+build.4", "0.14.25", "0.77.0+1.18.2", true),
        VersionProfile("1.16.5", 8, "7.3.3", "0.12-SNAPSHOT", "1.16.5+build.10", "0.14.24", "0.42.0+1.16", true),
        VersionProfile("26.3", 25, "9.6.0", "1.18-SNAPSHOT", "official", "0.19.5", "0.164.0+26.3", true)
    )

    fun profile(version: String): VersionProfile = profiles.firstOrNull { it.minecraft == version } ?: profiles.first()
}

data class ModProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val modId: String,
    val packageName: String,
    val minecraftVersion: String = "1.21.1",
    val javaVersion: Int = 21,
    val gradleVersion: String = "8.10.2",
    val template: String = "Empty",
    val author: String = "sally",
    val description: String = "A Minecraft Fabric mod created with ModForge.",
    val visibility: String = "private",
    val repoOwner: String = "",
    val repoName: String = "",
    val branch: String = "main",
    val lastSyncedSha: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("modId", modId)
        .put("packageName", packageName)
        .put("minecraftVersion", minecraftVersion)
        .put("javaVersion", javaVersion)
        .put("gradleVersion", gradleVersion)
        .put("template", template)
        .put("author", author)
        .put("description", description)
        .put("visibility", visibility)
        .put("repoOwner", repoOwner)
        .put("repoName", repoName)
        .put("branch", branch)
        .put("lastSyncedSha", lastSyncedSha)
        .put("createdAt", createdAt)

    companion object {
        fun fromJson(json: JSONObject): ModProject = ModProject(
            id = json.optString("id", UUID.randomUUID().toString()),
            name = json.optString("name", "Untitled Mod"),
            modId = json.optString("modId", "untitledmod"),
            packageName = json.optString("packageName", "com.sally.untitledmod"),
            minecraftVersion = json.optString("minecraftVersion", "1.21.1"),
            javaVersion = json.optInt("javaVersion", 21),
            gradleVersion = json.optString("gradleVersion", "8.10.2"),
            template = json.optString("template", "Empty"),
            author = json.optString("author", "sally"),
            description = json.optString("description", "A Minecraft Fabric mod created with ModForge."),
            visibility = json.optString("visibility", "private"),
            repoOwner = json.optString("repoOwner", ""),
            repoName = json.optString("repoName", ""),
            branch = json.optString("branch", "main"),
            lastSyncedSha = json.optString("lastSyncedSha", ""),
            createdAt = json.optLong("createdAt", System.currentTimeMillis())
        )
    }
}

fun sanitizeModId(value: String): String {
    val cleaned = value.lowercase()
        .replace(Regex("[^a-z0-9_-]"), "")
        .trim('_', '-')
    return when {
        cleaned.isBlank() -> "mymod"
        cleaned.first().isDigit() -> "mod$cleaned"
        else -> cleaned.take(64)
    }
}

fun sanitizePackage(value: String): String {
    val parts = value.split('.').map { it.lowercase().replace(Regex("[^a-z0-9_]"), "") }
        .filter { it.isNotBlank() }
        .map { if (it.first().isDigit()) "p$it" else it }
    return parts.joinToString(".").ifBlank { "com.sally.mymod" }
}

fun javaClassName(name: String): String = name.split(Regex("[^A-Za-z0-9]+"))
    .filter { it.isNotBlank() }
    .joinToString("") { it.replaceFirstChar(Char::uppercase) }
    .ifBlank { "ModForgeMod" }
