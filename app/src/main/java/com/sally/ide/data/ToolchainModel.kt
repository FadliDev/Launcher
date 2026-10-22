package com.sally.ide.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** State Compose untuk layar Toolchain: paket terpasang, progres unduhan, pemakaian storage. */
class ToolchainModel(
    private val tc: Toolchain,
    private val scope: CoroutineScope,
    private val notify: (String) -> Unit,
) {
    val catalog: List<Pkg> = Toolchain.catalog()
    val installed = mutableStateMapOf<String, Boolean>()
    /** 0..1, atau -1 = ukuran belum diketahui. Ada di map = sedang diunduh/dipasang. */
    val progress = mutableStateMapOf<String, Float>()
    var usedBytes by mutableLongStateOf(0L)
        private set
    var cacheBytes by mutableLongStateOf(0L)
        private set
    var loaded by mutableStateOf(false)
        private set
    private val jobs = HashMap<String, Job>()

    fun installedGradle(): List<String> = tc.installedGradle()

    fun refresh() {
        scope.launch {
            val (inst, used, cache) = withContext(Dispatchers.IO) {
                Triple(catalog.associate { it.id to tc.isInstalled(it) }, tc.usedBytes(), tc.cacheBytes())
            }
            inst.forEach { (k, v) -> installed[k] = v }
            usedBytes = used; cacheBytes = cache; loaded = true
        }
    }

    fun install(p: Pkg) {
        if (jobs.containsKey(p.id)) return
        progress[p.id] = -1f
        jobs[p.id] = scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    tc.install(p) { done, total ->
                        scope.launch { if (progress.containsKey(p.id)) progress[p.id] = if (total > 0) done.toFloat() / total else -1f }
                    }
                }
                notify("${p.title} terpasang")
            } catch (e: CancellationException) {
                notify("${p.title}: dibatalkan (unduhan bisa dilanjutkan)")
                throw e
            } catch (e: Exception) {
                notify("Gagal: ${e.message ?: e.javaClass.simpleName}")
            } finally {
                jobs.remove(p.id)
                progress.remove(p.id)
                refresh()
            }
        }
    }

    fun cancel(p: Pkg) { jobs[p.id]?.cancel() }

    fun remove(p: Pkg) {
        scope.launch {
            val ok = withContext(Dispatchers.IO) { tc.remove(p) }
            if (!ok) notify("Gagal menghapus ${p.title}")
            refresh()
        }
    }

    fun clearCache() {
        scope.launch {
            withContext(Dispatchers.IO) { tc.clearCache() }
            refresh()
        }
    }
}
