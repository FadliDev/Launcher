package com.sally.ide.data

import java.io.File
import java.util.zip.ZipInputStream

enum class PkgKind { GRADLE, PLATFORM }

/** Satu paket toolchain yang bisa diunduh. [id] unik, dipakai sebagai kunci status. */
data class Pkg(val id: String, val kind: PkgKind, val title: String, val desc: String, val version: String)

/**
 * Toolchain terkelola app, di `filesDir/toolchain`:
 *   gradle/gradle-<v>/            distribusi Gradle (bin/lib)
 *   sdk/platforms/android-<N>/    android.jar + source.properties
 *   downloads/                    file sementara (.part)
 * JDK, build-tools (aapt2/d8), dan NDK sengaja belum ada: menunggu hasil spike Fase 0 (lihat PLAN §4.5).
 */
class Toolchain(val root: File) {
    val gradleDir = File(root, "gradle")
    val sdkDir = File(root, "sdk")
    val downloads = File(root, "downloads")

    companion object {
        val GRADLE_VERSIONS = listOf("8.13", "8.11.1", "8.10.2", "8.9", "8.7", "7.6.4")
        val API_LEVELS = listOf(36, 35, 34, 33, 31, 30, 28)
        /** Bisa diganti kalau repo berubah/hilang (PLAN §5: sediakan mirror / URL custom). */
        const val PLATFORM_BASE = "https://raw.githubusercontent.com/Sable/android-platforms/master"
        fun gradleUrl(v: String) = "https://services.gradle.org/distributions/gradle-$v-bin.zip"
        fun platformUrl(api: Int) = "$PLATFORM_BASE/android-$api/android.jar"

        fun catalog(): List<Pkg> =
            GRADLE_VERSIONS.map { Pkg("gradle-$it", PkgKind.GRADLE, "Gradle $it", "Distribusi -bin (~130 MB)", it) } +
                API_LEVELS.map { Pkg("platform-$it", PkgKind.PLATFORM, "Android API $it", "android.jar untuk compileSdk $it", it.toString()) }
    }

    fun dirOf(p: Pkg): File = when (p.kind) {
        PkgKind.GRADLE -> File(gradleDir, "gradle-${p.version}")
        PkgKind.PLATFORM -> File(sdkDir, "platforms/android-${p.version}")
    }

    fun isInstalled(p: Pkg): Boolean = when (p.kind) {
        PkgKind.GRADLE -> File(dirOf(p), "lib").isDirectory
        PkgKind.PLATFORM -> File(dirOf(p), "android.jar").isFile
    }

    fun installedGradle(): List<String> = GRADLE_VERSIONS.filter { File(gradleDir, "gradle-$it/lib").isDirectory }

    /** Blocking. Panggil dari IO dispatcher. */
    suspend fun install(p: Pkg, onProgress: (Long, Long) -> Unit) {
        downloads.mkdirs()
        when (p.kind) {
            PkgKind.GRADLE -> {
                val url = gradleUrl(p.version)
                val expected = Downloader.fetchText("$url.sha256").trim().substringBefore(' ').takeIf { it.length == 64 }
                val zip = File(downloads, "gradle-${p.version}-bin.zip")
                Downloader.download(url, zip, expected, onProgress)
                val tmp = File(gradleDir, ".tmp-${p.version}").also { it.deleteRecursively(); it.mkdirs() }
                unzip(zip, tmp)
                val inner = File(tmp, "gradle-${p.version}")
                val dest = dirOf(p)
                dest.deleteRecursively()
                gradleDir.mkdirs()
                if (!inner.renameTo(dest)) throw java.io.IOException("Struktur zip Gradle tidak dikenali")
                tmp.deleteRecursively()
                File(dest, "bin").listFiles()?.forEach { it.setExecutable(true) }
                zip.delete()
            }
            PkgKind.PLATFORM -> {
                val dest = dirOf(p)
                dest.mkdirs()
                val jar = File(dest, "android.jar")
                // Repo tidak menyediakan checksum: hash dicatat lokal setelah unduhan pertama (PLAN §5.6).
                val sum = Downloader.download(platformUrl(p.version.toInt()), jar, null, onProgress)
                File(dest, "android.jar.sha256").writeText(sum)
                File(dest, "source.properties").writeText(
                    "Pkg.Desc=Android SDK Platform ${p.version}\nPkg.Revision=1\nAndroidVersion.ApiLevel=${p.version}\n"
                )
            }
        }
    }

    fun remove(p: Pkg): Boolean = dirOf(p).deleteRecursively()

    fun usedBytes(): Long = root.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    fun cacheBytes(): Long = downloads.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    fun clearCache() { downloads.deleteRecursively() }

    private fun unzip(zip: File, dest: File) {
        val base = dest.canonicalPath + File.separator
        ZipInputStream(zip.inputStream().buffered()).use { zin ->
            while (true) {
                val e = zin.nextEntry ?: break
                val out = File(dest, e.name)
                if (!out.canonicalPath.startsWith(base)) throw java.io.IOException("Entri zip tidak aman: ${e.name}") // zip-slip
                if (e.isDirectory) out.mkdirs() else {
                    out.parentFile?.mkdirs()
                    out.outputStream().use { zin.copyTo(it) }
                }
            }
        }
    }
}
