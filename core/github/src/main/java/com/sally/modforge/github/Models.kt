package com.sally.modforge.github

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Sesi login aktif. Token sengaja tidak tampil di toString. */
class GitHubSession(val login: String, val token: String) {
    override fun toString() = "GitHubSession(login=$login)"
}

@Serializable
data class GhUser(val login: String, val id: Long)

@Serializable
data class GhRepo(
    val id: Long,
    val name: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("html_url") val htmlUrl: String,
    @SerialName("default_branch") val defaultBranch: String = "main",
    @SerialName("private") val isPrivate: Boolean = true,
)

@Serializable
data class DeviceCodeResponse(
    @SerialName("device_code") val deviceCode: String,
    @SerialName("user_code") val userCode: String,
    @SerialName("verification_uri") val verificationUri: String,
    @SerialName("expires_in") val expiresIn: Int,
    val interval: Int = 5,
)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String? = null,
    val error: String? = null,
    @SerialName("error_description") val errorDescription: String? = null,
)
