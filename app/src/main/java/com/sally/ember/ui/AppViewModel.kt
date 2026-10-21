package com.sally.ember.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sally.ember.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {
    val prefs = Prefs(app)

    private val _versions = MutableStateFlow<List<McVersion>>(emptyList())
    val versions: StateFlow<List<McVersion>> = _versions
    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError
    private val _dl = MutableStateFlow<DlState>(DlState.Idle)
    val dl: StateFlow<DlState> = _dl
    private val _accounts = MutableStateFlow(prefs.accounts())
    val accounts: StateFlow<List<Account>> = _accounts
    private val _selAcc = MutableStateFlow(prefs.selectedAccount)
    val selAcc: StateFlow<String?> = _selAcc
    private val _selVer = MutableStateFlow(prefs.selectedVersion)
    val selVer: StateFlow<String?> = _selVer
    private val _installedTick = MutableStateFlow(0)
    val installedTick: StateFlow<Int> = _installedTick

    init { refreshVersions() }

    fun refreshVersions() = viewModelScope.launch {
        _loadError.value = null
        runCatching { Mojang.manifest() }
            .onSuccess { _versions.value = it }
            .onFailure { _loadError.value = it.message ?: "Gagal memuat daftar versi" }
    }

    fun install(v: McVersion) {
        if (_dl.value is DlState.Running) return
        viewModelScope.launch {
            _dl.value = DlState.Running(v.id, 0, 1)
            runCatching { Mojang.install(v) { l, d, t -> _dl.value = DlState.Running(l, d, t) } }
                .onSuccess { _dl.value = DlState.Idle; select(v.id); _installedTick.value++ }
                .onFailure { _dl.value = DlState.Failed(it.message ?: "Download gagal") }
        }
    }

    fun select(id: String) { prefs.selectedVersion = id; _selVer.value = id }

    fun addAccount(name: String) {
        val clean = name.trim()
        if (clean.length !in 3..16 || !clean.all { it.isLetterOrDigit() || it == '_' }) return
        val a = prefs.addOffline(clean)
        _accounts.value = prefs.accounts(); _selAcc.value = a.uuid
    }

    fun selectAccount(uuid: String) { prefs.selectedAccount = uuid; _selAcc.value = uuid }

    fun removeAccount(uuid: String) {
        prefs.removeAccount(uuid)
        _accounts.value = prefs.accounts(); _selAcc.value = prefs.selectedAccount
    }
}
