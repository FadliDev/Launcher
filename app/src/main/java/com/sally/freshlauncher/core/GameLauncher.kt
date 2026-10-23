package com.sally.freshlauncher.core

import android.content.Context
import android.content.Intent
import java.io.File

/**
 * Titik integrasi ke runtime JVM. Implementasi utama mengirim argumen ke GameService
 * (proses :game), yang menyiapkan environment JRE lalu memanggil JVM.
 */
interface GameLauncher {
    suspend fun launch(spec: LaunchSpec): Result<Unit>
}

class PendingRuntimeLauncher : GameLauncher {
    override suspend fun launch(spec: LaunchSpec): Result<Unit> =
        Result.failure(IllegalStateException("JRE belum terpasang. Pasang dulu dari menu Pengaturan."))
}

class JvmGameLauncher(
    private val context: Context,
    private val jreHome: File
) : GameLauncher {
    override suspend fun launch(spec: LaunchSpec): Result<Unit> = runCatching {
        check(JreEnvironment.isInstalled(jreHome)) { "JRE belum terpasang di ${jreHome.absolutePath}" }
        val argv = (listOf("java") + spec.javaArgs + spec.mainClass + spec.gameArgs).toTypedArray()
        val intent = Intent(context, GameService::class.java)
            .putExtra(GameService.EXTRA_JRE_HOME, jreHome.absolutePath)
            .putExtra(GameService.EXTRA_GAME_DIR, GamePaths.gameDir.absolutePath)
            .putExtra(GameService.EXTRA_ARGS, argv)
        context.startForegroundService(intent)
    }
}
