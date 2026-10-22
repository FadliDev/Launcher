package com.sally.ide.data

import org.json.JSONObject
import java.io.File

/** Setelan build per proyek, disimpan di `.sallyide/project.json` (PLAN §4.2). Kosong/null = ikuti proyek. */
data class BuildConfig(
    val gradleVersion: String? = null,
    val jdkVersion: Int = 17,
    val compileSdk: Int? = null,
    val ndkVersion: String? = null,
    val jvmArgs: String = "-Xmx1536m",
)

object ProjectConfig {
    private fun file(project: File) = File(project, ".sallyide/project.json")

    fun load(project: File): BuildConfig = runCatching {
        val j = JSONObject(file(project).readText())
        BuildConfig(
            gradleVersion = j.optString("gradleVersion").ifBlank { null },
            jdkVersion = j.optInt("jdkVersion", 17),
            compileSdk = if (j.has("compileSdk")) j.optInt("compileSdk").takeIf { it > 0 } else null,
            ndkVersion = j.optString("ndkVersion").ifBlank { null },
            jvmArgs = j.optString("jvmArgs", "-Xmx1536m"),
        )
    }.getOrElse { BuildConfig() }

    fun save(project: File, c: BuildConfig) {
        val j = JSONObject()
        c.gradleVersion?.let { j.put("gradleVersion", it) }
        j.put("jdkVersion", c.jdkVersion)
        c.compileSdk?.let { j.put("compileSdk", it) }
        c.ndkVersion?.let { j.put("ndkVersion", it) }
        j.put("jvmArgs", c.jvmArgs)
        file(project).apply { parentFile?.mkdirs(); writeText(j.toString(2)) }
    }

    /** Versi Gradle dari `gradle/wrapper/gradle-wrapper.properties` proyek (mis. "8.9"), atau null. */
    fun wrapperVersion(project: File): String? = runCatching {
        val t = File(project, "gradle/wrapper/gradle-wrapper.properties").readText()
        Regex("""gradle-([\d.]+)-(?:bin|all)\.zip""").find(t)?.groupValues?.get(1)
    }.getOrNull()

    /** Versi AGP dari build.gradle(.kts) / settings / libs.versions.toml, atau null. */
    fun detectAgp(project: File): String? {
        val toml = File(project, "gradle/libs.versions.toml")
        val files = listOf(toml, File(project, "build.gradle.kts"), File(project, "build.gradle"),
            File(project, "settings.gradle.kts"), File(project, "settings.gradle"))
        val pats = listOf(
            Regex("""(?m)^\s*agp\s*=\s*"([\d.]+)""""),
            Regex("""com\.android\.(?:application|library)["']?\)?\s*(?:version\s*)?\(?["']([\d.]+)["']"""),
            Regex("""com\.android\.tools\.build:gradle:([\d.]+)"""),
        )
        for (f in files) {
            val t = runCatching { f.readText() }.getOrNull() ?: continue
            for (p in pats) p.find(t)?.let { return it.groupValues[1] }
        }
        return null
    }
}

/** Tabel kompatibilitas Gradle ↔ AGP ↔ JDK (ringkas, dari dokumentasi resmi; bisa usang). */
object Compat {
    private fun v(s: String): List<Int> = s.split('.').map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
    fun cmp(a: String, b: String): Int {
        val x = v(a); val y = v(b)
        for (i in 0 until maxOf(x.size, y.size)) {
            val d = x.getOrElse(i) { 0 } - y.getOrElse(i) { 0 }
            if (d != 0) return d
        }
        return 0
    }

    /** AGP (major.minor) → Gradle minimum. */
    private val agpMinGradle = listOf(
        "8.0" to "8.0", "8.1" to "8.0", "8.2" to "8.2", "8.3" to "8.4", "8.4" to "8.6", "8.5" to "8.7",
        "8.6" to "8.7", "8.7" to "8.9", "8.8" to "8.10.2", "8.9" to "8.11.1", "8.10" to "8.11.1", "8.11" to "8.13",
    )

    fun warnings(gradle: String?, jdk: Int, agp: String?): List<String> {
        val out = ArrayList<String>()
        if (gradle != null) {
            if (jdk >= 21 && cmp(gradle, "8.5") < 0) out += "JDK $jdk butuh Gradle ≥ 8.5 (sekarang $gradle)"
            else if (jdk >= 17 && cmp(gradle, "7.3") < 0) out += "JDK 17 butuh Gradle ≥ 7.3 (sekarang $gradle)"
            if (jdk <= 8 && cmp(gradle, "9.0") >= 0) out += "Gradle $gradle tidak bisa jalan di JDK 8"
        }
        if (agp != null) {
            val key = agp.split('.').take(2).joinToString(".")
            val min = agpMinGradle.firstOrNull { it.first == key }?.second
            if (min != null && gradle != null && cmp(gradle, min) < 0) out += "AGP $agp butuh Gradle ≥ $min (sekarang $gradle)"
            if (cmp(agp, "8.0") >= 0 && jdk < 17) out += "AGP $agp butuh JDK 17+"
        }
        return out
    }
}
