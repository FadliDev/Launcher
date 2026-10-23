package com.sally.freshlauncher.core

import android.content.Context
import android.content.Intent
import java.io.File

/**
 * Titik integrasi ke runtime JVM. Implementasi utama mengirim argumen ke GameService
 * (proses :game), yang lalu memanggil NativeJvm.launch.
 */
interface GameLauncher {
    suspend fun launch(spec: LaunchSpec): Result<Unit>
}

class PendingRuntimeLauncher : GameLauncher {
    override suspend fun launch(spec: LaunchSpec): Result<Unit> =
        Result.failure(IllegalStateException("JRE belum tersedia di ${GamePaths.jreHome.absolutePath}"))
}

class JvmGameLauncher(
    private val context: Context,
    private val jliLib: File
) : GameLauncher {
    override suspend fun launch(spec: LaunchSpec): Result<Unit> = runCatching {
        check(jliLib.exists()) { "libjli.so tidak ditemukan di ${jliLib.absolutePath}" }
        val argv = (listOf("java") + spec.javaArgs + spec.mainClass + spec.gameArgs).toTypedArray()
        val intent = Intent(context, GameService::class.java)
            .putExtra(GameService.EXTRA_JLI, jliLib.absolutePath)
            .putExtra(GameService.EXTRA_ARGS, argv)
        context.startForegroundService(intent)
    }
}
