package com.sally.modforge.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sally.modforge.github.DeviceLogin
import com.sally.modforge.github.GitHubApi
import com.sally.modforge.github.GitHubUser
import com.sally.modforge.github.RemoteRepository
import com.sally.modforge.github.TokenVault
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ModForgeController(private val context: Context) {
    val store = LocalProjectStore(context)
    private val vault = TokenVault(context)
    private val github = GitHubApi(context, store)
    private val prefs = context.getSharedPreferences("modforge_settings", Context.MODE_PRIVATE)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var buildJob: Job? = null

    var projects by mutableStateOf(store.loadProjects())
        private set
    var tab by mutableStateOf(MainTab.Projects)
    var page by mutableStateOf(AppPage.Home)
    var selectedProjectId by mutableStateOf("")
    var projectSection by mutableStateOf(ProjectSection.Overview)
    var editingPath by mutableStateOf("")
    var editorContent by mutableStateOf("")
    var isDark by mutableStateOf(prefs.getBoolean("dark_theme", false))
    var tokenConnected by mutableStateOf(vault.hasToken())
        private set
    var githubUser by mutableStateOf<GitHubUser?>(null)
        private set
    var githubRepos by mutableStateOf<List<RemoteRepository>>(emptyList())
        private set
    var oauthClientId by mutableStateOf(vault.readOAuthClientId())
    var busy by mutableStateOf(false)
        private set
    var message by mutableStateOf("")
        private set
    var buildStatus by mutableStateOf("Belum ada build untuk project ini.")
        private set
    var buildLog by mutableStateOf<List<String>>(emptyList())
        private set
    var lastBuildUrl by mutableStateOf("")
        private set
    var lastArtifactPath by mutableStateOf("")
        private set
    var deviceCode by mutableStateOf("")
        private set
    var deviceVerificationUri by mutableStateOf("https://github.com/login/device")
        private set
    var deviceStatus by mutableStateOf("")
        private set

    val selectedProject: ModProject?
        get() = projects.firstOrNull { it.id == selectedProjectId }

    init {
        if (tokenConnected) refreshGitHub()
    }

    fun dismissMessage() { message = "" }
    fun showMessage(text: String) { message = text }

    fun navigateHome(target: MainTab = MainTab.Projects) {
        tab = target
        page = AppPage.Home
        if (target == MainTab.GitHub && tokenConnected) refreshGitHub()
    }

    fun openNewProject() {
        page = AppPage.NewProject
    }

    fun createProject(name: String, modIdInput: String, packageInput: String, minecraft: String, template: String, visibility: String) {
        val safeName = name.trim().ifBlank { "My First Mod" }
        val modId = sanitizeModId(modIdInput)
        val packageName = sanitizePackage(packageInput)
        if (!modId.matches(Regex("^[a-z][a-z0-9_-]{0,63}$"))) {
            message = "Mod ID harus diawali huruf kecil dan hanya berisi a-z, angka, _ atau -."
            return
        }
        if (packageName.split('.').any { !it.matches(Regex("^[a-z][a-z0-9_]*$")) }) {
            message = "Package Java belum valid. Contoh: com.sally.mymod"
            return
        }
        val profile = VersionMatrix.profile(minecraft)
        val project = ModProject(
            name = safeName,
            modId = modId,
            packageName = packageName,
            minecraftVersion = minecraft,
            javaVersion = profile.java,
            gradleVersion = profile.gradle,
            template = template,
            visibility = visibility
        )
        scope.launch {
            busy = true
            message = "Membuat struktur project…"
            try {
                withContext(Dispatchers.IO) { store.createProject(project) }
                projects = withContext(Dispatchers.IO) { store.loadProjects() }
                selectedProjectId = project.id
                projectSection = ProjectSection.Overview
                page = AppPage.Project
                message = "Project ${project.name} dibuat di penyimpanan lokal."
            } catch (e: Exception) {
                message = "Gagal membuat project: ${e.message ?: "kesalahan tidak diketahui"}"
            } finally {
                busy = false
            }
        }
    }

    fun openProject(project: ModProject) {
        selectedProjectId = project.id
        projectSection = ProjectSection.Overview
        page = AppPage.Project
        buildStatus = "Belum ada build untuk project ini."
        buildLog = emptyList()
        lastBuildUrl = ""
        lastArtifactPath = ""
    }

    fun deleteProject(project: ModProject) {
        scope.launch {
            withContext(Dispatchers.IO) { store.deleteProject(project.id) }
            projects = withContext(Dispatchers.IO) { store.loadProjects() }
            if (selectedProjectId == project.id) page = AppPage.Home
            message = "Project ${project.name} dihapus."
        }
    }

    fun toggleDarkTheme() {
        isDark = !isDark
        prefs.edit().putBoolean("dark_theme", isDark).apply()
    }

    fun connectWithPersonalToken(token: String) {
        if (token.isBlank()) { message = "Token GitHub belum diisi."; return }
        scope.launch {
            busy = true
            message = "Memvalidasi token GitHub…"
            try {
                val user = withContext(Dispatchers.IO) { github.getUser(token.trim()) }
                vault.saveToken(token.trim())
                tokenConnected = true
                githubUser = user
                message = "Terhubung sebagai @${user.login}."
                refreshRepositories()
            } catch (e: Exception) {
                message = "Login gagal: ${e.message ?: "token tidak valid"}"
            } finally {
                busy = false
            }
        }
    }

    fun saveOAuthClientId(value: String) {
        oauthClientId = value.trim()
        vault.saveOAuthClientId(oauthClientId)
        message = "OAuth Client ID disimpan di perangkat."
    }

    fun startDeviceFlow() {
        if (oauthClientId.isBlank()) {
            message = "Isi OAuth Client ID milik aplikasi di Pengaturan terlebih dahulu."
            return
        }
        scope.launch {
            busy = true
            deviceStatus = "Meminta kode login…"
            try {
                val login = withContext(Dispatchers.IO) { github.beginDeviceLogin(oauthClientId) }
                deviceCode = login.userCode
                deviceVerificationUri = login.verificationUri
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(login.verificationUri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                deviceStatus = "Masukkan kode di halaman GitHub. Menunggu konfirmasi…"
                val token = withContext(Dispatchers.IO) {
                    github.pollDeviceLogin(oauthClientId, login) { status ->
                        mainHandler.post { deviceStatus = status }
                    }
                }
                val user = withContext(Dispatchers.IO) { github.getUser(token) }
                vault.saveToken(token)
                tokenConnected = true
                githubUser = user
                deviceStatus = "Berhasil masuk sebagai @${user.login}."
                message = deviceStatus
                refreshRepositories()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                deviceStatus = e.message ?: "Login GitHub gagal."
                message = deviceStatus
            } finally {
                busy = false
            }
        }
    }

    fun disconnectGitHub() {
        vault.clearToken()
        tokenConnected = false
        githubUser = null
        githubRepos = emptyList()
        message = "Token lokal dihapus. Untuk mencabut akses OAuth, cabut juga dari GitHub Settings."
    }

    fun refreshGitHub() {
        val token = vault.readToken() ?: run { tokenConnected = false; return }
        scope.launch {
            try {
                val user = withContext(Dispatchers.IO) { github.getUser(token) }
                githubUser = user
                tokenConnected = true
                refreshRepositories()
            } catch (e: Exception) {
                tokenConnected = false
                githubUser = null
                message = "Sesi GitHub tidak valid. Masuk kembali melalui tab GitHub."
            }
        }
    }

    private fun refreshRepositories() {
        val token = vault.readToken() ?: return
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { github.listRepositories(token) } }
                .onSuccess { githubRepos = it }
                .onFailure { message = "Tidak bisa memuat repository: ${it.message}" }
        }
    }

    fun openEditor(path: String) {
        val project = selectedProject ?: return
        runCatching { store.readText(project, path) }
            .onSuccess { editorContent = it; editingPath = path; page = AppPage.Editor }
            .onFailure { message = "File tidak dapat dibuka: ${it.message}" }
    }

    fun saveEditor() {
        val project = selectedProject ?: return
        runCatching { store.writeText(project, editingPath, editorContent) }
            .onSuccess { message = "File disimpan." }
            .onFailure { message = "Gagal menyimpan: ${it.message}" }
    }

    fun startBuild() {
        val project = selectedProject ?: return
        val token = vault.readToken()
        if (token.isNullOrBlank()) {
            message = "Hubungkan GitHub dulu. Build menggunakan GitHub Actions, bukan CPU HP."
            tab = MainTab.GitHub
            page = AppPage.Home
            return
        }
        buildJob?.cancel()
        buildJob = scope.launch {
            busy = true
            buildLog = emptyList()
            buildStatus = "Menyiapkan build…"
            projectSection = ProjectSection.Build
            try {
                val result = withContext(Dispatchers.IO) {
                    github.buildProject(token, project) { status ->
                        mainHandler.post {
                            buildStatus = status
                            buildLog = (buildLog + status).takeLast(100)
                        }
                    }
                }
                projects = withContext(Dispatchers.IO) { store.loadProjects() }
                buildStatus = result.summary
                buildLog = buildLog + result.summary
                lastBuildUrl = result.runUrl
                lastArtifactPath = result.artifact?.absolutePath.orEmpty()
                message = result.summary
            } catch (e: CancellationException) {
                buildStatus = "Pemantauan build dibatalkan. Run yang sudah dikirim ke GitHub bisa tetap berjalan."
                buildLog = buildLog + buildStatus
                throw e
            } catch (e: Exception) {
                buildStatus = e.message ?: "Build gagal."
                buildLog = buildLog + buildStatus
                message = buildStatus
            } finally {
                busy = false
            }
        }
    }

    fun cancelBuild() {
        buildJob?.cancel()
        busy = false
    }


    fun shareArtifact() {
        val path = lastArtifactPath
        if (path.isBlank()) { message = "Belum ada JAR yang bisa dibagikan."; return }
        runCatching {
            val file = File(path)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "application/java-archive"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(send, "Bagikan mod JAR").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure { message = "Tidak bisa membuka menu berbagi: ${it.message}" }
    }

    fun openUrl(url: String) {
        if (url.isBlank()) return
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { message = "Tidak bisa membuka link: ${it.message}" }
    }

    fun close() {
        scope.cancel()
    }
}

enum class MainTab { Projects, GitHub, Settings }
enum class AppPage { Home, NewProject, Project, Editor }
enum class ProjectSection(val label: String) {
    Overview("Overview"), Code("Code"), Visual("Visual"), Models("3D Models"), Assets("Assets"), Build("Build")
}
