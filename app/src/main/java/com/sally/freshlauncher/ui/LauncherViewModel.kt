package com.sally.freshlauncher.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sally.freshlauncher.core.GameLauncher
import com.sally.freshlauncher.core.GamePaths
import com.sally.freshlauncher.core.Installer
import com.sally.freshlauncher.core.JvmGameLauncher
import com.sally.freshlauncher.core.LaunchBuilder
import com.sally.freshlauncher.core.OfflineAccount
import com.sally.freshlauncher.core.PendingRuntimeLauncher
import com.sally.freshlauncher.core.VersionEntry
import com.sally.freshlauncher.core.VersionManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LauncherViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("fresh", Context.MODE_PRIVATE)

    var versions by mutableStateOf<List<VersionEntry>>(emptyList())
    var installed by mutableStateOf<Set<String>>(emptySet())
    var selected by mutableStateOf(prefs.getString("selected", null))
    var accountName by mutableStateOf(prefs.getString("name", "Player") ?: "Player")
    var busy by mutableStateOf(false)
    val log = mutableStateListOf<String>()

    init {
        GamePaths.init(app)
        installed = GamePaths.installedVersions()
    }

    /** Dipanggil lazy setelah init, supaya GamePaths sudah siap. */
    private val gameLauncher: GameLauncher by lazy {
        if (GamePaths.jliLib.exists()) JvmGameLauncher(getApplication(), GamePaths.jliLib)
        else PendingRuntimeLauncher()
    }

    /** Selalu memasukkan log dari thread manapun ke thread utama. */
    private fun say(msg: String) {
        viewModelScope.launch(Dispatchers.Main) { log.add(msg) }
    }

    fun refreshVersions() = viewModelScope.launch {
        busy = true
        try {
            versions = VersionManifest.fetch()
            say("Daftar versi dimuat (${versions.size})")
        } catch (e: Exception) {
            say("Gagal memuat daftar versi: ${e.message}")
        } finally {
            busy = false
        }
    }

    fun install(entry: VersionEntry) = viewModelScope.launch {
        busy = true
        try {
            Installer.install(entry) { say(it) }
            installed = GamePaths.installedVersions()
            say("Terpasang: ${entry.id}")
        } catch (e: Exception) {
            say("Gagal memasang ${entry.id}: ${e.message}")
        } finally {
            busy = false
        }
    }

    fun select(id: String) {
        selected = id
        prefs.edit().putString("selected", id).apply()
    }

    fun saveAccount(name: String) {
        accountName = name
        prefs.edit().putString("name", name).apply()
    }

    fun play() = viewModelScope.launch {
        val id = selected
        if (id == null) {
            say("Pilih versi terlebih dahulu")
            return@launch
        }
        if (id !in installed) {
            say("Versi $id belum terpasang")
            return@launch
        }
        if (!OfflineAccount.isValid(accountName)) {
            say("Nama pemain tidak valid")
            return@launch
        }
        try {
            val spec = withContext(Dispatchers.IO) { LaunchBuilder.build(id, OfflineAccount(accountName)) }
            say("Perintah: ${spec.preview()}")
            gameLauncher.launch(spec).onFailure { say(it.message ?: "Gagal meluncurkan") }
        } catch (e: Exception) {
            say("Error: ${e.message}")
        }
    }
}
