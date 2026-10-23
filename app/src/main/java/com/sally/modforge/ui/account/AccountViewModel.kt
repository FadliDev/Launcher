package com.sally.modforge.ui.account

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sally.modforge.ModForgeApp
import com.sally.modforge.github.DeviceCodeResponse
import com.sally.modforge.github.GitHubSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountUiState(
    val session: GitHubSession? = null,
    val clientId: String = "",
    val device: DeviceCodeResponse? = null,
    val pat: String = "",
    val repoName: String = "modforge-test",
    val createdRepoUrl: String? = null,
    val busy: Boolean = false,
    val message: String? = null,
    val error: Boolean = false,
)

class AccountViewModel(application: Application) : AndroidViewModel(application) {

    private val account = (application as ModForgeApp).account
    private val _ui = MutableStateFlow(AccountUiState(clientId = account.clientId))
    val ui: StateFlow<AccountUiState> = _ui.asStateFlow()

    private var flowJob: Job? = null

    init {
        viewModelScope.launch {
            account.session.collect { s -> _ui.update { it.copy(session = s) } }
        }
        viewModelScope.launch { account.restore() }
    }

    fun onClientIdChange(value: String) {
        account.clientId = value
        _ui.update { it.copy(clientId = value) }
    }

    fun onPatChange(value: String) {
        _ui.update { it.copy(pat = value) }
    }

    fun onRepoNameChange(value: String) {
        _ui.update { it.copy(repoName = value) }
    }

    fun startDeviceFlow() {
        flowJob?.cancel()
        flowJob = task("Login gagal") {
            val device = account.startDeviceFlow()
            _ui.update { it.copy(device = device) }
            try {
                account.pollDeviceFlow(device)
            } finally {
                _ui.update { it.copy(device = null) }
            }
            "Berhasil masuk"
        }
    }

    fun cancelDeviceFlow() {
        flowJob?.cancel()
        flowJob = null
        _ui.update { it.copy(device = null, message = null, error = false) }
    }

    fun loginWithPat() {
        task("Token tidak valid") {
            account.loginWithToken(_ui.value.pat.trim())
            _ui.update { it.copy(pat = "") }
            "Berhasil masuk"
        }
    }

    fun createTestRepo() {
        task("Gagal membuat repo") {
            val repo = account.createRepoWithReadme(_ui.value.repoName.trim())
            _ui.update { it.copy(createdRepoUrl = repo.htmlUrl) }
            "Repo ${repo.fullName} dibuat"
        }
    }

    fun logout() {
        account.logout()
        _ui.update { it.copy(createdRepoUrl = null, message = null, error = false) }
    }

    private fun task(errorPrefix: String, block: suspend () -> String): Job = viewModelScope.launch {
        _ui.update { it.copy(busy = true, message = null, error = false) }
        try {
            val msg = block()
            _ui.update { it.copy(message = msg, error = false) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _ui.update { it.copy(message = "$errorPrefix: ${e.message ?: "tidak diketahui"}", error = true) }
        } finally {
            _ui.update { it.copy(busy = false) }
        }
    }
}
