package com.sally.freshlauncher.core

/**
 * Titik integrasi ke runtime JVM. Implementasi nyata membutuhkan:
 *  - JRE terbundel (jre-17/jre-21) dan pemanggilan libjli.so via JNI
 *  - bridge GLFW/GL (lihat ZalithLauncher/jni sebagai referensi arsitektur)
 *  - native library LWJGL dari folder natives
 */
interface GameLauncher {
    suspend fun launch(spec: LaunchSpec): Result<Unit>
}

class PendingRuntimeLauncher : GameLauncher {
    override suspend fun launch(spec: LaunchSpec): Result<Unit> =
        Result.failure(IllegalStateException("Runtime JVM belum terpasang. Lihat README bagian 'Yang belum ada'."))
}
