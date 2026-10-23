package com.sally.modforge.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class VersionEntry(
    val minecraft: String,
    val javaMin: Int,
    val javaMax: Int,
    val javaRecommended: Int,
    val gradle: String,
    val loom: String,
    val fabricLoader: String,
    val fabricApi: String,
    val mappings: List<String>,
    val packFormat: Int? = null,
    /** "stable" atau "experimental" */
    val stability: String = "stable",
    /** false = nilai belum diuji di CI; verifikasi di M0 */
    val verified: Boolean = false,
    val features: Map<String, Boolean> = emptyMap(),
)

@Serializable
data class VersionMatrix(
    val schema: Int,
    val updated: String,
    val versions: List<VersionEntry>,
) {
    fun find(minecraft: String): VersionEntry? = versions.firstOrNull { it.minecraft == minecraft }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(text: String): VersionMatrix = json.decodeFromString(text)
    }
}
