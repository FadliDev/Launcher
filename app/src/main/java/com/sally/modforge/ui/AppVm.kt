package com.sally.modforge.ui

import android.app.Application
import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sally.modforge.BuildConfig
import com.sally.modforge.build.BuildCoordinator
import com.sally.modforge.data.ProjectStore
import com.sally.modforge.data.TemplateEngine
import com.sally.modforge.data.VersionProfiles
import com.sally.modforge.github.DeviceCode
import com.sally.modforge.github.GitHubClient
import com.sally.modforge.github.Secrets
import com.sally.modforge.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class AppVm(app: Application) : AndroidViewModel(app) {
    val store = ProjectStore(app)
    val secrets = Secrets(app)
    private val templates = TemplateEngine(app)
    val profiles = VersionProfiles.load(app)
    private val gh = GitHubClient { secrets.token }
    val builder = BuildCoordinator(store, gh, templates)

    val projects = MutableStateFlow(store.list())
    val login = MutableStateFlow(secrets.login)
    val deviceCode = MutableStateFlow<DeviceCode?>(null)
    val message = MutableStateFlow<String?>(null)

    fun refresh() { projects.value = store.list() }

    fun create(name: String, author: String, loader: Loader, mc: String, priv: Boolean) {
        val pr = profiles.first { it.loader == loader && it.mc == mc }
        val modId = name.toModId()
        val p = Project(
            id = UUID.randomUUID().toString(), name = name, modId = modId, pkg = defaultPackage(author, modId),
            author = author, loader = loader, mcVersion = mc, privateRepo = priv,
            toolchain = Toolchain(pr.java, pr.gradle, pr.loaderVersion, pr.pluginVersion, pr.apiVersion, pr.mappings),
        )
        try {
            templates.generate(p, store.repoDir(p)); store.save(p); refresh()
        } catch (e: Exception) {
            store.delete(p); message.value = e.message
        }
    }

    fun delete(p: Project) { store.delete(p); refresh() }

    /** Save = tulis lokal saja. Menyimpan workflow berarti file itu jadi Custom (tidak ditimpa app). */
    fun saveFile(p: Project, path: String, text: String) {
        store.write(p, path, text)
        if (path == TemplateEngine.WORKFLOW && path !in p.toolchain.customFiles) {
            store.save(p.copy(toolchain = p.toolchain.copy(customFiles = p.toolchain.customFiles + path)))
            refresh()
        }
    }

    fun build(p: Project) {
        if (secrets.token == null) { message.value = "Connect GitHub dulu (tombol di pojok kanan atas)."; return }
        viewModelScope.launch { builder.run(p) { refresh() } }
    }

    fun connect() {
        val cid = secrets.clientId.ifBlank { BuildConfig.GITHUB_CLIENT_ID }
        if (cid.isBlank()) { message.value = "Isi OAuth Client ID dulu."; return }
        viewModelScope.launch {
            try {
                val dc = withContext(Dispatchers.IO) { gh.startDeviceFlow(cid) }
                deviceCode.value = dc
                val t = withContext(Dispatchers.IO) { gh.pollToken(cid, dc) }
                secrets.token = t
                secrets.login = withContext(Dispatchers.IO) { gh.user() }
                login.value = secrets.login
            } catch (e: Exception) {
                message.value = e.message
            } finally { deviceCode.value = null }
        }
    }

    fun disconnect() { secrets.token = null; secrets.login = null; login.value = null }

    fun exportJar(f: File) {
        if (android.os.Build.VERSION.SDK_INT < 29) { message.value = "Jar ada di ${f.path}"; return }
        val cv = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, f.name)
            put(MediaStore.Downloads.MIME_TYPE, "application/java-archive")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/ModForge")
        }
        val r = getApplication<Application>().contentResolver
        val uri = r.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv) ?: return
        r.openOutputStream(uri)?.use { o -> f.inputStream().use { it.copyTo(o) } }
        message.value = "Disimpan ke Download/ModForge/${f.name}"
    }
}
