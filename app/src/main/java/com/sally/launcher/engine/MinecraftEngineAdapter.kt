package com.sally.launcher.engine

import android.content.Context
import com.sally.launcher.data.InstallResult

/** Boundary for the future runtime integration. No game process is launched by this MVP adapter. */
interface MinecraftEngineAdapter {
    suspend fun launch(context: Context, install: InstallResult): EngineLaunchResult
}

data class EngineLaunchResult(val started: Boolean, val message: String)

class UnavailableMinecraftEngineAdapter : MinecraftEngineAdapter {
    override suspend fun launch(context: Context, install: InstallResult): EngineLaunchResult =
        EngineLaunchResult(
            started = false,
            message = "File client ${install.versionId} sudah diunduh, tetapi game belum bisa dijalankan. Tahap berikutnya adalah resolver libraries/assets, Java runtime, renderer, dan adapter mesin Pojav yang kompatibel."
        )
}
