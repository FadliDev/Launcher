package com.sally.modforge.ui

import com.sally.modforge.data.ProjectStore
import com.sally.modforge.data.TemplateEngine
import com.sally.modforge.github.GitHubClient
import com.sally.modforge.github.gitBlobSha
import com.sally.modforge.github.long
import com.sally.modforge.github.str
import com.sally.modforge.model.Project
import com.sally.modforge.model.toSlug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.zip.ZipInputStream

sealed interface BuildState {
    data object Idle : BuildState
    data class Working(val step: String) : BuildState
    data class Done(val runUrl: String, val jars: List<File>) : BuildState
    data class Failed(val message: String, val log: String = "") : BuildState
}

/**
 * Syncs a project through GitHub's Git Data API, waits for its Actions workflow,
 * and extracts the resulting JAR artifact into the app's private project storage.
 */
class BuildCoordinator(
    private val store: ProjectStore,
    private val github: GitHubClient,
    private val templates: TemplateEngine,
) {
    val state = MutableStateFlow<BuildState>(BuildState.Idle)

    suspend fun run(project: Project, onProjectUpdated: () -> Unit = {}) {
        if (state.value is BuildState.Working) return

        var workingProject = project
        var repository = project.repoFullName
        var activeRunId: Long? = null
        var actionLog = ""

        state.value = BuildState.Working("Menyiapkan project...")
        try {
            // Create the remote repository on the first build.
            if (repository.isBlank()) {
                val created = withContext(Dispatchers.IO) {
                    github.createRepo(project.name.toSlug(), project.privateRepo)
                }
                repository = created.first
                workingProject = workingProject.copy(
                    repoFullName = repository,
                    toolchain = workingProject.toolchain.copy(branch = created.second.ifBlank { "main" }),
                )
                withContext(Dispatchers.IO) { store.save(workingProject) }
                onProjectUpdated()
            }

            state.value = BuildState.Working("Mengunggah source ke GitHub...")
            val pushed = withContext(Dispatchers.IO) {
                templates.writeWorkflow(workingProject, store.repoDir(workingProject))
                val files = store.readAll(workingProject)
                check(files.isNotEmpty()) { "Project kosong; tidak ada file untuk di-build." }

                val hashes = files.mapValues { (_, bytes) -> gitBlobSha(bytes) }
                val branch = workingProject.toolchain.branch.ifBlank { "main" }
                val parent = github.head(repository, branch)
                val commit = github.commit(
                    repo = repository,
                    branch = branch,
                    files = files,
                    shas = hashes,
                    known = workingProject.pushedHashes,
                    message = "Build ${workingProject.name} (${System.currentTimeMillis()})",
                    parent = parent,
                )
                val updated = workingProject.copy(
                    repoFullName = repository,
                    lastPushedCommit = commit,
                    pushedHashes = hashes,
                    toolchain = workingProject.toolchain.copy(branch = branch),
                )
                store.save(updated)
                PushResult(updated, commit)
            }
            workingProject = pushed.project
            onProjectUpdated()

            val branch = workingProject.toolchain.branch.ifBlank { "main" }
            state.value = BuildState.Working("Menunggu GitHub Actions...")

            var actionRun: JsonObject? = null
            var dispatchSent = false
            val startTime = System.currentTimeMillis()
            val deadline = startTime + ACTION_TIMEOUT_MS

            while (System.currentTimeMillis() < deadline) {
                actionRun = withContext(Dispatchers.IO) {
                    github.runs(repository, branch).firstOrNull {
                        it.str("head_sha") == pushed.commitSha
                    }
                }

                val status = actionRun?.str("status")
                if (status == "completed") break

                // Managed workflows run on push. If a custom workflow has no push trigger,
                // fall back to workflow_dispatch after allowing the push event to appear.
                if (actionRun == null && !dispatchSent &&
                    System.currentTimeMillis() - startTime >= DISPATCH_FALLBACK_MS
                ) {
                    withContext(Dispatchers.IO) { github.dispatch(repository, branch) }
                    dispatchSent = true
                }

                state.value = BuildState.Working(
                    when (status) {
                        "queued", "requested", "waiting" -> "Build masuk antrean GitHub Actions..."
                        "in_progress" -> "GitHub Actions sedang build..."
                        else -> "Menunggu workflow GitHub Actions..."
                    }
                )
                delay(POLL_INTERVAL_MS)
            }

            val completedRun = actionRun
                ?: throw IOException("Workflow GitHub Actions tidak ditemukan sebelum timeout.")
            activeRunId = completedRun.long("id")
            if (completedRun.str("status") != "completed") {
                throw IOException("Build GitHub Actions timeout setelah menunggu 15 menit.")
            }

            val runUrl = completedRun.str("html_url")
                ?: "https://github.com/$repository/actions/runs/$activeRunId"
            val conclusion = completedRun.str("conclusion")
            if (conclusion != "success") {
                actionLog = withContext(Dispatchers.IO) {
                    github.failedLog(repository, activeRunId!!)
                }
                throw IOException("GitHub Actions selesai dengan status: ${conclusion ?: "tidak diketahui"}.")
            }

            state.value = BuildState.Working("Mengunduh artifact JAR...")
            val artifactList = withContext(Dispatchers.IO) { github.artifacts(repository, activeRunId!!) }
                .filter { it.str("expired") != "true" }
                .let { all -> all.filter { it.str("name") == "mod-jar" }.ifEmpty { all } }

            if (artifactList.isEmpty()) {
                throw IOException("Build berhasil, tetapi GitHub Actions tidak menyediakan artifact untuk diunduh.")
            }

            val destination = File(store.dir(workingProject), "artifacts/run-$activeRunId")
            withContext(Dispatchers.IO) {
                destination.deleteRecursively()
                destination.mkdirs()
                val temporary = File(destination, ".downloads").apply { mkdirs() }

                artifactList.forEachIndexed { index, artifact ->
                    val url = artifact.str("archive_download_url")
                        ?: throw IOException("URL download artifact tidak tersedia dari GitHub.")
                    val archive = File(temporary, "artifact-$index.zip")
                    github.download(url, archive)
                    extractZipSafely(archive, destination)
                    archive.delete()
                }
                temporary.deleteRecursively()
            }

            val jars = withContext(Dispatchers.IO) {
                destination.walkTopDown()
                    .filter { it.isFile && it.extension.equals("jar", ignoreCase = true) }
                    .toList()
            }
            if (jars.isEmpty()) {
                throw IOException("Artifact berhasil diunduh, tetapi file .jar tidak ditemukan di dalamnya.")
            }

            state.value = BuildState.Done(runUrl, jars)
        } catch (error: Exception) {
            if (actionLog.isBlank() && activeRunId != null && repository.isNotBlank()) {
                actionLog = runCatching {
                    withContext(Dispatchers.IO) { github.failedLog(repository, activeRunId!!) }
                }.getOrDefault("")
            }
            state.value = BuildState.Failed(
                message = error.message?.takeIf { it.isNotBlank() } ?: "Build gagal karena error yang tidak diketahui.",
                log = actionLog,
            )
        }
    }

    private data class PushResult(val project: Project, val commitSha: String)

    private fun extractZipSafely(archive: File, destination: File) {
        val root = destination.canonicalFile
        val rootPrefix = root.path + File.separator
        ZipInputStream(BufferedInputStream(FileInputStream(archive))).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val output = File(destination, entry.name).canonicalFile
                if (output.path != root.path && !output.path.startsWith(rootPrefix)) {
                    throw IOException("Artifact ZIP berisi path tidak valid.")
                }
                if (entry.isDirectory) {
                    output.mkdirs()
                } else {
                    output.parentFile?.mkdirs()
                    output.outputStream().use { zip.copyTo(it) }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }

    private companion object {
        const val POLL_INTERVAL_MS = 5_000L
        const val DISPATCH_FALLBACK_MS = 15_000L
        const val ACTION_TIMEOUT_MS = 15 * 60 * 1_000L
    }
}
