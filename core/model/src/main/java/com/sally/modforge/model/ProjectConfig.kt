package com.sally.modforge.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProjectConfig(
    val id: String,
    val name: String,
    val modId: String,
    @SerialName("package") val packageName: String,
    val loader: String = "fabric",
    val minecraft: String,
    val java: Int,
    val gradle: String,
    val mappings: String = "mojang",
    val template: String = "empty",
    val github: GitHubLink? = null,
)

@Serializable
data class GitHubLink(
    val owner: String,
    val repo: String,
    val branch: String = "main",
    val visibility: String = "private",
    val lastSyncedSha: String? = null,
)
