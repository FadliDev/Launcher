package com.sally.modforge.data

import com.sally.modforge.github.GitHubClient
import com.sally.modforge.model.VersionMatrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Urutan sumber: cache hasil refresh terakhir → data bawaan APK.
 * Refresh dari [remoteUrl] berjalan di belakang layar; gagal = tetap pakai data yang ada.
 */
class VersionMatrixRepository(
    private val cacheFile: File,
    private val client: GitHubClient,
    private val remoteUrl: String,
    private val bundled: () -> String,
) {
    private val _matrix = MutableStateFlow(loadLocal())
    val matrix: StateFlow<VersionMatrix> = _matrix.asStateFlow()

    private fun loadLocal(): VersionMatrix {
        val cached = runCatching {
            if (cacheFile.exists()) VersionMatrix.parse(cacheFile.readText()) else null
        }.getOrNull()?.takeIf { it.isUsable() }
        return cached ?: VersionMatrix.parse(bundled())
    }

    suspend fun refresh() {
        if (remoteUrl.isBlank()) return
        val text = runCatching { client.fetchText(remoteUrl) }.getOrNull() ?: return
        val parsed = runCatching { VersionMatrix.parse(text) }.getOrNull() ?: return
        if (!parsed.isUsable()) return

        withContext(Dispatchers.IO) {
            // tulis ke file sementara lalu ganti, supaya cache tidak setengah jadi
            val tmp = File(cacheFile.path + ".tmp")
            runCatching {
                tmp.writeText(text)
                tmp.renameTo(cacheFile)
            }
        }
        _matrix.value = parsed
    }

    private fun VersionMatrix.isUsable(): Boolean =
        schema <= SUPPORTED_SCHEMA && versions.isNotEmpty()

    companion object {
        const val SUPPORTED_SCHEMA = 1
    }
}
