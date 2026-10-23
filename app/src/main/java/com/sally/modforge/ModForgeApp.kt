package com.sally.modforge

import android.app.Application
import com.sally.modforge.data.VersionMatrixRepository
import com.sally.modforge.github.GitHubAccount
import com.sally.modforge.github.GitHubClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

class ModForgeApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Dibuat lazy agar startup tetap ringan. */
    val account: GitHubAccount by lazy { GitHubAccount(this) }

    val versions: VersionMatrixRepository by lazy {
        VersionMatrixRepository(
            cacheFile = File(filesDir, "version-matrix.json"),
            client = GitHubClient(),
            remoteUrl = REMOTE_MATRIX_URL,
            bundled = { assets.open("version-matrix.json").bufferedReader().use { it.readText() } },
        )
    }

    override fun onCreate() {
        super.onCreate()
        appScope.launch { versions.refresh() }
    }

    companion object {
        /**
         * URL raw ke version-matrix.json di repo ModForge (mis. raw.githubusercontent.com/.../main/...).
         * Kosong = hanya pakai data bawaan APK.
         */
        const val REMOTE_MATRIX_URL = ""
    }
}
