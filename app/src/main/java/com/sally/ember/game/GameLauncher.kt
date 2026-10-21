package com.sally.ember.game

import android.content.Context
import android.system.Os
import com.oracle.dalvik.VMLauncher
import com.sally.ember.data.Account
import com.sally.ember.data.Paths
import com.sally.ember.data.Prefs
import net.kdt.pojavlaunch.Logger
import net.kdt.pojavlaunch.utils.JREUtils
import org.json.JSONObject
import java.io.File

/** Mirrors Zalith's JREUtils.launchWithUtils: env → LD path → dlopen runtime/renderer → JLI_Launch. */
object GameLauncher {
    private var loaded = false

    @Synchronized private fun loadNative() {
        if (loaded) return
        // linkerhook must be first (-z global); exithook depends on pojavexec.
        listOf("linkerhook", "pojavexec", "exithook").forEach { System.loadLibrary(it) }
        loaded = true
    }

    /** Blocks until the JVM exits. Call from a background thread with a ready Surface already set up. */
    fun launch(ctx: Context, versionId: String, acc: Account, prefs: Prefs, width: Int, height: Int): Int {
        loadNative()
        Paths.logFile.writeText("")
        Logger.begin(Paths.logFile.path)
        fun say(s: String) = Logger.appendToLog(s)

        val vjson = JSONObject(File(Paths.versions, "$versionId/$versionId.json").readText())
        val major = vjson.optJSONObject("javaVersion")?.optInt("majorVersion", 8) ?: 8
        val jre = JreManager.forMajor(major)
        say("Minecraft $versionId needs Java $major -> using ${jre.dir}")
        JreManager.ensure(ctx, jre) { say(it) }
        val glfwJar = JreManager.ensureGlfwJar(ctx)

        val jreHome = JreManager.home(jre)
        val libDir = JreManager.libDir(jreHome)
        val nativeDir = ctx.applicationInfo.nativeLibraryDir
        val renderer = Renderers.resolve(prefs.renderer, File(nativeDir))
        say("Renderer: ${renderer.name} (${renderer.library})")
        if (renderer.id != prefs.renderer && prefs.renderer != null)
            say("WARNING: renderer yang dipilih tidak ada di APK, fallback ke ${renderer.name}")

        // LD_LIBRARY_PATH, same ordering as Zalith (jli, lib, system dirs, app native dir)
        val jvmDir = if (File(jreHome, "$libDir/server/libjvm.so").exists()) "server" else "client"
        val ld = "$jreHome/$libDir/jli:$jreHome/$libDir:/system/lib64:/vendor/lib64:/vendor/lib64/hw:$nativeDir"
        JREUtils.setLdLibraryPath("$jreHome/$libDir/$jvmDir:$ld")

        val env = mutableMapOf(
            "POJAV_NATIVEDIR" to nativeDir, "DRIVER_PATH" to nativeDir,
            "JAVA_HOME" to jreHome.path, "HOME" to Paths.gameDir.path,
            "TMPDIR" to ctx.cacheDir.path, "LD_LIBRARY_PATH" to "$jreHome/$libDir/$jvmDir:$ld",
            "PATH" to "$jreHome/bin:" + (Os.getenv("PATH") ?: ""),
            "FORCE_VSYNC" to "false", "AWTSTUB_WIDTH" to "$width", "AWTSTUB_HEIGHT" to "$height",
            "POJAV_RENDERER" to renderer.id
        )
        env += renderer.env
        env.forEach { (k, v) -> Os.setenv(k, v, true); say("env $k=$v") }

        fun find(name: String) = ld.split(":").map { File(it, name) }.firstOrNull { it.exists() }?.path ?: name
        val jli = find("libjli.so")
        say("libjli: $jli (exists=${File(jli).exists()})")
        if (!File(jli).exists() || !JREUtils.dlopen(jli)) {
            say("ERROR: libjli.so gagal dimuat dari $jli. JRE rusak/tidak lengkap atau folder tidak bisa dieksekusi (jangan taruh JRE di storage eksternal).")
            File(jreHome, ".ember_version").delete() // force re-extract next launch
            return -3
        }
        if (!JREUtils.dlopen("libjvm.so") && !JREUtils.dlopen("$jreHome/$libDir/$jvmDir/libjvm.so")) {
            say("ERROR: libjvm.so gagal dimuat dari $jreHome/$libDir/$jvmDir")
            return -4
        }
        listOf("libverify.so", "libjava.so", "libnet.so", "libnio.so", "libawt.so", "libawt_headless.so",
            "libfreetype.so", "libfontmanager.so").forEach { JREUtils.dlopen(find(it)) }
        JREUtils.dlopen("$nativeDir/libopenal.so")
        if (!JREUtils.dlopen("$nativeDir/${renderer.library}") && !JREUtils.dlopen(find(renderer.library)))
            say("ERROR: gagal memuat renderer ${renderer.library}")

        val args = LaunchArgs.build(versionId, acc, prefs.ramMb, prefs.extraJvm, width, height,
            jreHome, nativeDir, renderer.library, glfwJar)
        args.forEach { say("JVMArg: $it") }

        JREUtils.setupExitMethod(ctx.applicationContext)
        JREUtils.initializeGameExitHook()
        JREUtils.chdir(Paths.gameDir.path)

        val code = VMLauncher.launchJVM((listOf("java") + args).toTypedArray()) // argv[0] = program name
        say("Java exit code: $code")
        return code
    }
}
