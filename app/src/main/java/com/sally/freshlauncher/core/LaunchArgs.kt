package com.sally.freshlauncher.core

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class LaunchSpec(
    val javaArgs: List<String>,
    val mainClass: String,
    val gameArgs: List<String>
) {
    fun preview(): String = (listOf("java") + javaArgs + mainClass + gameArgs).joinToString(" ")
}

object LaunchBuilder {
    fun build(versionId: String, account: OfflineAccount, ramMb: Int): LaunchSpec {
        val dir = File(GamePaths.versions, versionId)
        val json = JSONObject(File(dir, "$versionId.json").readText())
        val classpath = File(dir, "classpath.txt").readText()
        val nativesDir = File(dir, "natives").apply { mkdirs() }

        val ph = mapOf(
            "auth_player_name" to account.name,
            "version_name" to versionId,
            "game_directory" to GamePaths.gameDir.absolutePath,
            "assets_root" to GamePaths.assets.absolutePath,
            "assets_index_name" to json.getJSONObject("assetIndex").getString("id"),
            "auth_uuid" to account.uuid,
            "auth_access_token" to "0",
            "auth_xuid" to "",
            "clientid" to "",
            "user_type" to "legacy",
            "version_type" to json.optString("type", "release"),
            "natives_directory" to nativesDir.absolutePath,
            "launcher_name" to "FreshLauncher",
            "launcher_version" to "0.1.0",
            "classpath" to classpath
        )

        val jvm = mutableListOf<String>()
        val game = mutableListOf<String>()
        if (json.has("arguments")) {
            val args = json.getJSONObject("arguments")
            collect(args.optJSONArray("jvm"), jvm, ph)
            collect(args.optJSONArray("game"), game, ph)
        } else {
            json.optString("minecraftArguments")
                .split(" ")
                .filter { it.isNotBlank() }
                .forEach { game += subst(it, ph) }
        }

        if (jvm.none { it.startsWith("-Djava.library.path") }) {
            jvm += "-Djava.library.path=${nativesDir.absolutePath}"
        }
        if (jvm.none { it == "-cp" || it == "-classpath" }) {
            jvm += listOf("-cp", classpath)
        }
        jvm += "-Xmx${ramMb}M"

        return LaunchSpec(jvm, json.getString("mainClass"), game)
    }

    private fun collect(arr: JSONArray?, out: MutableList<String>, ph: Map<String, String>) {
        if (arr == null) return
        for (i in 0 until arr.length()) {
            when (val e = arr.get(i)) {
                is String -> out += subst(e, ph)
                is JSONObject -> {
                    if (!Rules.allowed(e.optJSONArray("rules"))) continue
                    when (val v = e.get("value")) {
                        is String -> out += subst(v, ph)
                        is JSONArray -> for (j in 0 until v.length()) out += subst(v.getString(j), ph)
                    }
                }
            }
        }
    }

    private fun subst(s: String, ph: Map<String, String>): String =
        ph.entries.fold(s) { acc, (k, v) -> acc.replace("\${$k}", v) }
}
