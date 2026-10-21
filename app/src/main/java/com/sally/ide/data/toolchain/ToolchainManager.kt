package com.sally.ide.data.toolchain

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

enum class ToolCategory(val label: String) { JDK("JDK"), BUILD_TOOLS("Build-tools"), NDK("NDK"), OTHER("Lainnya") }

data class GradleDist(val version: String, val downloadUrl: String, val checksumUrl: String)

data class CustomPackage(val category: ToolCategory, val name: String, val url: String, val sha256: String = "")

enum class TaskPhase { DOWNLOADING, VERIFYING, EXTRACTING, FAILED }

data class TaskState(
    val label: String,
    val phase: TaskPhase,
    val done: Long = 0,
    val total: Long = -1,
    val error: String? = null
)

data class StorageInfo(val sdk: Long, val gradle: Long, val custom: Long, val cache: Long) {
    val total: Long get() = sdk + gradle + custom + cache
}

/**
 * Fase 3: manages the app-private toolchain under `files/toolchain/`:
 *  - `sdk/platforms/android-N/android.jar` (+ source.properties) from Sable/android-platforms
 *  - `gradle/gradle-<version>/` from services.gradle.org (SHA-256 checked)
 *  - `custom/<CATEGORY>/<name>/` for user-provided .zip packages (JDK, build-tools, NDK)
 * Downloads run on an app-level scope so they continue while the user navigates inside the app.
 */
object ToolchainManager {
    private const val PLATFORMS_API = "https://api.github.com/repos/Sable/android-platforms/contents"
    private const val PLATFORM_RAW = "https://raw.githubusercontent.com/Sable/android-platforms/master"
    private const val GRADLE_VERSIONS = "https://services.gradle.org/versions/all"
    private const val MAX_UNZIPPED = 3L * 1024 * 1024 * 1024
    private const val MAX_ENTRIES = 60_000

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val jobs = HashMap<String, Job>()

    /** Active/failed tasks by id: `platform:35`, `gradle:8.11.1`, `custom:JDK/zulu21`. */
    val tasks = mutableStateMapOf<String, TaskState>()

    /** Bumped whenever something is installed/removed so the UI re-reads the disk. */
    var revision by mutableIntStateOf(0)
        private set

    // ---------- paths ----------

    fun root(context: Context) = File(context.filesDir, "toolchain").apply { mkdirs() }
    fun sdkRoot(context: Context) = File(root(context), "sdk")
    fun platformDir(context: Context, api: Int) = File(sdkRoot(context), "platforms/android-$api")
    fun androidJar(context: Context, api: Int) = File(platformDir(context, api), "android.jar")
    fun gradleRoot(context: Context) = File(root(context), "gradle")
    fun gradleHome(context: Context, version: String) = File(gradleRoot(context), "gradle-$version")
    private fun customRoot(context: Context) = File(root(context), "custom")
    private fun partialDir(context: Context) = File(root(context), ".partial")
    private fun cacheDir(context: Context) = File(root(context), ".cache")

    // ---------- installed ----------

    fun installedPlatforms(context: Context): List<Int> =
        File(sdkRoot(context), "platforms").listFiles().orEmpty()
            .mapNotNull { dir -> dir.name.removePrefix("android-").toIntOrNull()?.takeIf { File(dir, "android.jar").isFile } }
            .sortedDescending()

    fun installedGradle(context: Context): List<String> =
        gradleRoot(context).listFiles().orEmpty()
            .filter { it.isDirectory && File(it, "lib").isDirectory }
            .map { it.name.removePrefix("gradle-") }
            .sortedDescending()

    fun installedCustom(context: Context): List<CustomPackage> =
        ToolCategory.values().flatMap { category ->
            File(customRoot(context), category.name).listFiles().orEmpty().filter { it.isDirectory }.map { dir ->
                CustomPackage(category, dir.name, runCatching { File(dir, ".source-url").readText() }.getOrDefault(""))
            }
        }

    fun folderSize(file: File): Long = when {
        !file.exists() -> 0L
        file.isFile -> file.length()
        else -> file.listFiles().orEmpty().sumOf { folderSize(it) }
    }

    fun storage(context: Context) = StorageInfo(
        sdk = folderSize(sdkRoot(context)),
        gradle = folderSize(gradleRoot(context)),
        custom = folderSize(customRoot(context)),
        cache = folderSize(partialDir(context)) + folderSize(cacheDir(context))
    )

    /** Removes unfinished downloads and cached remote lists (installed tools stay). Active tasks are cancelled first. */
    fun clearCache(context: Context) {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        tasks.clear()
        partialDir(context).deleteRecursively()
        cacheDir(context).deleteRecursively()
        revision++
    }

    // ---------- remote lists ----------

    private val builtInApis = (3..37).filter { it != 20 }

    suspend fun loadPlatformApis(context: Context): List<Int> {
        val cache = File(cacheDir(context), "platforms.json")
        val fromNet = runCatching {
            val array = JSONArray(Downloader.fetchText(PLATFORMS_API, "application/vnd.github+json"))
            val apis = (0 until array.length()).mapNotNull { i ->
                val item = array.getJSONObject(i)
                if (item.optString("type") != "dir") null
                else Regex("android-(\\d+)").matchEntire(item.optString("name"))?.groupValues?.get(1)?.toIntOrNull()
            }.sortedDescending()
            if (apis.isNotEmpty()) writeCache(cache, apis.joinToString(","))
            apis
        }.getOrNull()
        if (!fromNet.isNullOrEmpty()) return fromNet
        val cached = runCatching { cache.readText().split(',').mapNotNull { it.toIntOrNull() } }.getOrNull()
        return if (!cached.isNullOrEmpty()) cached else builtInApis.sortedDescending()
    }

    suspend fun loadGradleVersions(context: Context): List<GradleDist> {
        val cache = File(cacheDir(context), "gradle-versions.json")
        val text = runCatching {
            Downloader.fetchText(GRADLE_VERSIONS, "application/json").also { writeCache(cache, it) }
        }.getOrNull() ?: runCatching { cache.readText() }.getOrNull() ?: throw IOException("Daftar versi Gradle belum bisa diambil (offline?)")
        return withContext(Dispatchers.Default) {
            val array = JSONArray(text)
            (0 until array.length()).map { array.getJSONObject(it) }.filter {
                !it.optBoolean("snapshot") && !it.optBoolean("nightly") && !it.optBoolean("releaseNightly") &&
                    !it.optBoolean("broken") && it.optString("rcFor").isEmpty() && it.optString("milestoneFor").isEmpty() &&
                    it.optString("version").isNotEmpty() && it.optString("downloadUrl").startsWith("https://")
            }.map {
                GradleDist(it.getString("version"), it.getString("downloadUrl"), it.optString("checksumUrl"))
            }
        }
    }

    private fun writeCache(file: File, text: String) {
        runCatching {
            file.parentFile?.mkdirs()
            file.writeText(text)
        }
    }

    // ---------- install / remove ----------

    fun installPlatform(context: Context, api: Int) {
        val app = context.applicationContext
        launchTask("platform:$api", "Android SDK platform $api") { update ->
            val partial = File(partialDir(app), "android-$api.jar.part")
            val staging = File(partialDir(app), "android-$api.jar")
            val result = Downloader.download("$PLATFORM_RAW/android-$api/android.jar", staging, partial) { done, total ->
                update(TaskState("Android SDK platform $api", TaskPhase.DOWNLOADING, done, total))
            }
            update(TaskState("Android SDK platform $api", TaskPhase.VERIFYING))
            withContext(Dispatchers.IO) {
                // A Git LFS pointer or an HTML error page is not a zip: reject it instead of installing garbage.
                val valid = runCatching { ZipFile(result.file).use { it.size() > 0 } }.getOrDefault(false)
                if (!valid) {
                    result.file.delete()
                    throw IOException("File yang diunduh bukan android.jar yang valid")
                }
                val dir = platformDir(app, api)
                dir.mkdirs()
                val jar = File(dir, "android.jar")
                if (jar.exists()) jar.delete()
                if (!result.file.renameTo(jar)) {
                    result.file.copyTo(jar, overwrite = true)
                    result.file.delete()
                }
                File(dir, "android.jar.sha256").writeText(result.sha256)
                File(dir, "source.properties").writeText(
                    "Pkg.Desc=Android SDK Platform $api\n" +
                        "Pkg.UserSrc=false\n" +
                        "Pkg.Path=platforms;android-$api\n" +
                        "Pkg.Revision=1\n" +
                        "AndroidVersion.ApiLevel=$api\n" +
                        "Layout.Version=3\n"
                )
            }
        }
    }

    fun installGradle(context: Context, dist: GradleDist) {
        val app = context.applicationContext
        val label = "Gradle ${dist.version}"
        launchTask("gradle:${dist.version}", label) { update ->
            val expected = if (dist.checksumUrl.startsWith("https://")) {
                runCatching { Downloader.fetchText(dist.checksumUrl).trim().substringBefore(' ').substringBefore('\n') }.getOrNull()
                    ?.takeIf { it.length == 64 }
            } else null
            val zip = File(partialDir(app), "gradle-${dist.version}-bin.zip")
            val partial = File(partialDir(app), "gradle-${dist.version}-bin.zip.part")
            Downloader.download(dist.downloadUrl, zip, partial, expected) { done, total ->
                update(TaskState(label, TaskPhase.DOWNLOADING, done, total))
            }
            update(TaskState(label, TaskPhase.EXTRACTING))
            withContext(Dispatchers.IO) {
                try {
                    val home = gradleHome(app, dist.version)
                    home.deleteRecursively()
                    unzip(zip, gradleRoot(app))
                    if (!File(home, "lib").isDirectory) throw IOException("Struktur distribusi Gradle tidak dikenali")
                    File(home, "bin").listFiles()?.forEach { it.setExecutable(true, false) }
                } finally {
                    zip.delete()
                }
            }
        }
    }

    fun installCustom(context: Context, pkg: CustomPackage) {
        val app = context.applicationContext
        val safeName = pkg.name.trim().replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "paket" }
        val label = "${pkg.category.label} · $safeName"
        launchTask("custom:${pkg.category.name}/$safeName", label) { update ->
            val file = File(partialDir(app), "custom-$safeName.zip")
            val partial = File(partialDir(app), "custom-$safeName.zip.part")
            val expected = pkg.sha256.trim().takeIf { it.isNotEmpty() }
            if (expected != null && !Regex("[0-9a-fA-F]{64}").matches(expected)) throw IOException("SHA-256 harus 64 karakter hex")
            Downloader.download(pkg.url.trim(), file, partial, expected) { done, total ->
                update(TaskState(label, TaskPhase.DOWNLOADING, done, total))
            }
            update(TaskState(label, TaskPhase.EXTRACTING))
            withContext(Dispatchers.IO) {
                try {
                    val dest = File(customRoot(app), "${pkg.category.name}/$safeName")
                    dest.deleteRecursively()
                    dest.mkdirs()
                    unzip(file, dest)
                    File(dest, ".source-url").writeText(pkg.url.trim())
                } finally {
                    file.delete()
                }
            }
        }
    }

    fun cancel(id: String) {
        jobs.remove(id)?.cancel()
        tasks.remove(id)
    }

    fun removePlatform(context: Context, api: Int) {
        platformDir(context, api).deleteRecursively()
        revision++
    }

    fun removeGradle(context: Context, version: String) {
        gradleHome(context, version).deleteRecursively()
        revision++
    }

    fun removeCustom(context: Context, pkg: CustomPackage) {
        File(customRoot(context), "${pkg.category.name}/${pkg.name}").deleteRecursively()
        revision++
    }

    // ---------- internals ----------

    private fun launchTask(id: String, label: String, block: suspend (update: (TaskState) -> Unit) -> Unit) {
        if (jobs[id]?.isActive == true) return
        tasks[id] = TaskState(label, TaskPhase.DOWNLOADING)
        jobs[id] = scope.launch {
            try {
                block { state -> tasks[id] = state }
                tasks.remove(id)
                revision++
            } catch (cancelled: CancellationException) {
                tasks.remove(id)
                throw cancelled
            } catch (error: Throwable) {
                tasks[id] = TaskState(label, TaskPhase.FAILED, error = error.message ?: error.javaClass.simpleName)
            } finally {
                jobs.remove(id)
            }
        }
    }

    /** Zip extraction with zip-slip protection plus entry-count and size limits. */
    private fun unzip(zip: File, destination: File) {
        destination.mkdirs()
        val base = destination.canonicalFile
        var entries = 0
        var written = 0L
        ZipInputStream(zip.inputStream().buffered()).use { input ->
            while (true) {
                val entry = input.nextEntry ?: break
                if (++entries > MAX_ENTRIES) throw IOException("Arsip punya terlalu banyak file")
                val out = File(base, entry.name).canonicalFile
                if (out.path != base.path && !out.path.startsWith(base.path + File.separator)) {
                    throw IOException("Arsip ditolak (path berbahaya): ${entry.name}")
                }
                if (entry.isDirectory) {
                    out.mkdirs()
                } else {
                    out.parentFile?.mkdirs()
                    out.outputStream().use { sink ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            written += read
                            if (written > MAX_UNZIPPED) throw IOException("Isi arsip melebihi batas ukuran")
                            sink.write(buffer, 0, read)
                        }
                    }
                }
                input.closeEntry()
            }
        }
    }
}
