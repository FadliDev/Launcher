package com.sally.modforge

import android.app.Application
import com.sally.modforge.github.GitHubAccount
import com.sally.modforge.model.VersionMatrix

class ModForgeApp : Application() {

    /** Dibuat lazy agar startup tetap ringan. */
    val account: GitHubAccount by lazy { GitHubAccount(this) }

    /** Matriks versi bawaan (fallback offline). Remote fetch menyusul di M1 berikutnya. */
    val versionMatrix: VersionMatrix by lazy {
        assets.open("version-matrix.json").bufferedReader().use { VersionMatrix.parse(it.readText()) }
    }
}
