package com.sally.freshlauncher.core

import android.content.Context
import android.system.Os
import net.kdt.pojavlaunch.utils.JREUtils
import java.io.File

/**
 * Menyiapkan environment JRE sebelum JVM dipanggil. Urutannya mengikuti
 * JREUtils.launchWithUtils di ZalithLauncher: LD_LIBRARY_PATH, env variable,
 * dlopen library JRE, lalu chdir dan exit hook.
 */
object JreEnvironment {

    /** Folder arsitektur di dalam lib/ JRE, misalnya lib/aarch64 (berisi jli/libjli.so). */
    fun archLibDir(jreHome: File): File? =
        File(jreHome, "lib").listFiles()?.firstOrNull { File(it, "jli/libjli.so").exists() }

    fun isInstalled(jreHome: File): Boolean = archLibDir(jreHome) != null

    fun prepare(context: Context, jreHome: File, gameDir: File) {
        NativeJvm.ensureLoaded()

        val archDir = archLibDir(jreHome) ?: error("libjli.so tidak ditemukan di ${jreHome.absolutePath}")
        val jvmDir = File(archDir, if (File(archDir, "server/libjvm.so").exists()) "server" else "client")
        val nativeDir = context.applicationInfo.nativeLibraryDir

        val ldLibraryPath = listOf(
            jvmDir.absolutePath,
            File(archDir, "jli").absolutePath,
            archDir.absolutePath,
            "/system/lib64",
            "/vendor/lib64",
            nativeDir
        ).joinToString(":")

        val env = mapOf(
            "POJAV_NATIVEDIR" to nativeDir,
            "DRIVER_PATH" to nativeDir,
            "JAVA_HOME" to jreHome.absolutePath,
            "HOME" to gameDir.absolutePath,
            "TMPDIR" to context.cacheDir.absolutePath,
            "LD_LIBRARY_PATH" to ldLibraryPath,
            "PATH" to jreHome.absolutePath + "/bin:" + (Os.getenv("PATH") ?: ""),
            "FORCE_VSYNC" to "false",
            "AWTSTUB_WIDTH" to "1280",
            "AWTSTUB_HEIGHT" to "720",
            "MOD_ANDROID_RUNTIME" to ""
        )
        env.forEach { (k, v) -> Os.setenv(k, v, true) }
        JREUtils.setLdLibraryPath(ldLibraryPath)

        // Urutan dlopen sama dengan ZalithLauncher; library lain di folder arsitektur menyusul.
        val priority = listOf(
            File(archDir, "jli/libjli.so"),
            File(jvmDir, "libjvm.so"),
            File(archDir, "libverify.so"),
            File(archDir, "libjava.so"),
            File(archDir, "libnet.so"),
            File(archDir, "libnio.so"),
            File(archDir, "libawt.so"),
            File(archDir, "libawt_headless.so"),
            File(archDir, "libfreetype.so"),
            File(archDir, "libfontmanager.so")
        )
        val others = archDir.walkTopDown().filter { it.isFile && it.name.endsWith(".so") }.toList()
        (priority + others)
            .filter { it.exists() }
            .distinct()
            .forEach { JREUtils.dlopen(it.absolutePath) }

        JREUtils.chdir(gameDir.absolutePath)
        // Wajib sebelum JVM berjalan: nominal_exit di native memakai referensi yang diisi di sini.
        JREUtils.setupExitMethod(context.applicationContext)
        JREUtils.initializeGameExitHook()
    }
}
