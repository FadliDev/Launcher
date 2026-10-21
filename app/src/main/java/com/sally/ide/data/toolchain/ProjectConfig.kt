package com.sally.ide.data.toolchain

import org.json.JSONObject
import java.io.File

/** Per-project build settings stored in `<project>/.sallyide/project.json`. Blank means "not chosen / auto". */
data class ProjectConfig(
    val gradleVersion: String = "",
    val jdkVersion: String = "",
    val compileSdk: Int = 0,
    val ndkVersion: String = "",
    val jvmArgs: String = "-Xmx1536m"
)

object ProjectConfigStore {
    fun file(projectDir: File) = File(projectDir, ".sallyide/project.json")

    fun load(projectDir: File): ProjectConfig {
        val stored = runCatching { JSONObject(file(projectDir).readText()) }.getOrNull()
        val detected = detect(projectDir)
        if (stored == null) return detected
        return ProjectConfig(
            gradleVersion = stored.optString("gradleVersion", detected.gradleVersion),
            jdkVersion = stored.optString("jdkVersion", detected.jdkVersion),
            compileSdk = stored.optInt("compileSdk", detected.compileSdk),
            ndkVersion = stored.optString("ndkVersion", detected.ndkVersion),
            jvmArgs = stored.optString("jvmArgs", detected.jvmArgs)
        )
    }

    fun save(projectDir: File, config: ProjectConfig) {
        val target = file(projectDir)
        target.parentFile?.mkdirs()
        val json = JSONObject()
            .put("gradleVersion", config.gradleVersion)
            .put("jdkVersion", config.jdkVersion)
            .put("compileSdk", config.compileSdk)
            .put("ndkVersion", config.ndkVersion)
            .put("jvmArgs", config.jvmArgs)
        target.writeText(json.toString(2))
    }

    /** Pre-fills from gradle-wrapper.properties and app/build.gradle(.kts) when there is no saved config yet. */
    fun detect(projectDir: File): ProjectConfig {
        val wrapper = runCatching { File(projectDir, "gradle/wrapper/gradle-wrapper.properties").readText() }.getOrDefault("")
        val gradle = Regex("gradle-([0-9][0-9A-Za-z.\\-]*?)-(?:bin|all)\\.zip").find(wrapper)?.groupValues?.get(1).orEmpty()
        val buildFile = listOf("app/build.gradle.kts", "app/build.gradle", "build.gradle.kts", "build.gradle")
            .map { File(projectDir, it) }.firstOrNull { it.isFile }
        val buildText = runCatching { buildFile?.readText().orEmpty() }.getOrDefault("")
        val sdk = Regex("compileSdk\\s*(?:=|\\s)\\s*(\\d+)").find(buildText)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val jdk = when {
            Regex("JavaVersion\\.VERSION_21|jvmTarget\\s*=\\s*\"21\"").containsMatchIn(buildText) -> "21"
            Regex("JavaVersion\\.VERSION_17|jvmTarget\\s*=\\s*\"17\"").containsMatchIn(buildText) -> "17"
            Regex("JavaVersion\\.VERSION_1_8|jvmTarget\\s*=\\s*\"1\\.8\"").containsMatchIn(buildText) -> "8"
            else -> ""
        }
        return ProjectConfig(gradleVersion = gradle, jdkVersion = jdk, compileSdk = sdk)
    }
}
