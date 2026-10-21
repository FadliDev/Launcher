package com.sally.ember.game

import android.os.Build
import com.sally.ember.data.Account
import com.sally.ember.data.Mojang
import com.sally.ember.data.Paths
import org.json.JSONObject
import java.io.File
import java.util.TimeZone

object LaunchArgs {
    fun build(
        versionId: String, acc: Account, ramMb: Int, extraJvm: String, width: Int, height: Int,
        jreHome: File, nativeDir: String, rendererLib: String, glfwJar: File
    ): List<String> {
        val vdir = File(Paths.versions, versionId)
        val json = JSONObject(File(vdir, "$versionId.json").readText())

        val cp = buildList {
            add(glfwJar.path) // must come first so our GLFW stub wins over the stock classes
            Mojang.libraries(json).forEach { add(File(Paths.libraries, it.second).path) }
            add(File(vdir, "$versionId.jar").path)
        }.joinToString(":")

        val tmp = File(Paths.root, "tmp").apply { mkdirs() }
        val jvm = mutableListOf(
            "-Xms${ramMb}M", "-Xmx${ramMb}M",
            "-Djava.home=${jreHome.path}",
            "-Djava.io.tmpdir=${tmp.path}",
            "-Djava.library.path=$nativeDir",
            "-Djna.boot.library.path=$nativeDir",
            "-Dorg.lwjgl.librarypath=$nativeDir",
            "-Dorg.lwjgl.opengl.libname=$rendererLib",
            "-Dorg.lwjgl.vulkan.libname=libvulkan.so",
            "-Dorg.lwjgl.freetype.libname=$nativeDir/libfreetype.so",
            "-Duser.home=${Paths.gameDir.path}",
            "-Duser.timezone=${TimeZone.getDefault().id}",
            "-Dos.name=Linux", "-Dos.version=Android-${Build.VERSION.RELEASE}",
            "-Dpojav.path.minecraft=${Paths.gameDir.path}",
            "-Dglfwstub.windowWidth=$width", "-Dglfwstub.windowHeight=$height", "-Dglfwstub.initEgl=false",
            "-Dlog4j2.formatMsgNoLookups=true",
            "-Dfml.earlyprogresswindow=false", "-Dloader.disable_forked_guis=true",
            "-Djdk.lang.Process.launchMechanism=FORK", // POSIX_SPAWN needs jspawnhelper, which does not work on Android
            "-XX:ActiveProcessorCount=${java.lang.Runtime.getRuntime().availableProcessors()}",
            "-cp", cp
        )
        jvm += extraJvm.split(" ").filter { it.isNotBlank() }

        val vars = mapOf(
            "auth_player_name" to acc.name, "auth_uuid" to acc.uuid, "auth_access_token" to "0",
            "auth_session" to "0", "user_type" to "legacy", "user_properties" to "{}", "clientid" to "0", "auth_xuid" to "0",
            "version_name" to versionId, "version_type" to json.optString("type", "release"),
            "game_directory" to Paths.gameDir.path, "assets_root" to Paths.assets.path, "game_assets" to Paths.assets.path,
            "assets_index_name" to json.optJSONObject("assetIndex")?.optString("id", versionId).orEmpty(),
            "resolution_width" to "$width", "resolution_height" to "$height",
            "launcher_name" to "Ember", "launcher_version" to "0.2.0"
        )
        val game = gameArgs(json).map { a -> vars.entries.fold(a) { s, (k, v) -> s.replace("\${$k}", v) } }
        return jvm + json.getString("mainClass") + game
    }

    /** Supports the modern `arguments.game` and the legacy `minecraftArguments` formats. */
    private fun gameArgs(json: JSONObject): List<String> {
        json.optString("minecraftArguments").takeIf { it.isNotEmpty() }?.let { return it.split(" ") }
        val arr = json.getJSONObject("arguments").getJSONArray("game")
        // Plain strings only: conditional entries (demo, custom resolution, quick play) are skipped.
        return (0 until arr.length()).mapNotNull { arr.get(it) as? String }
    }
}
