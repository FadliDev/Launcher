package com.sally.modforge.model

import kotlinx.serialization.Serializable

enum class Loader { FABRIC, FORGE, NEOFORGE }

@Serializable
data class Toolchain(
    val javaVersion: Int,
    val gradleVersion: String,
    val loaderVersion: String,
    val pluginVersion: String,
    val apiVersion: String = "",
    val mappings: String = "official",
    val buildCommand: String = "gradle build --no-daemon",
    val customFiles: Set<String> = emptySet(),
    val branch: String = "main",
)

@Serializable
data class Project(
    val id: String,
    val name: String,
    val modId: String,
    val pkg: String,
    val author: String,
    val loader: Loader,
    val mcVersion: String,
    val toolchain: Toolchain,
    val modVersion: String = "1.0.0",
    val description: String = "",
    val privateRepo: Boolean = true,
    val repoFullName: String = "",
    val lastPushedCommit: String = "",
    val pushedHashes: Map<String, String> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
data class VersionProfile(
    val mc: String,
    val loader: Loader,
    val java: Int,
    val gradle: String,
    val loaderVersion: String,
    val pluginVersion: String,
    val apiVersion: String = "",
    val mappings: String = "official",
)

fun String.toSlug() = lowercase().replace(Regex("[^a-z0-9._-]+"), "-").trim('-').ifEmpty { "mod" }

fun String.toModId(): String {
    val s = lowercase().replace(Regex("[^a-z0-9_]+"), "_").trim('_')
    return if (s.firstOrNull()?.isLetter() == true) s else "mod_$s"
}

fun String.toClassName(): String {
    val base = split(Regex("[^A-Za-z0-9]+")).filter { it.isNotEmpty() }
        .joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }
    return (if (base.firstOrNull()?.isLetter() == true) base else "Mod$base") + "Mod"
}

fun defaultPackage(author: String, modId: String): String {
    val a = author.lowercase().filter { it.isLetterOrDigit() }.ifEmpty { "example" }
    return "com.${if (a.first().isDigit()) "u$a" else a}.${modId.replace("_", "")}"
}
