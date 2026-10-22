package com.sally.modforge.data

import android.content.Context
import com.sally.modforge.model.Loader
import com.sally.modforge.model.Project
import com.sally.modforge.model.toClassName
import java.io.File

class TemplateEngine(private val ctx: Context) {
    companion object { const val WORKFLOW = ".github/workflows/build.yml" }

    private val fabric = listOf(
        "build.gradle" to "build.gradle", "settings.gradle" to "settings.gradle",
        "gradle.properties" to "gradle.properties",
        "fabric.mod.json" to "src/main/resources/fabric.mod.json",
        "Main.java" to "src/main/java/{{pkgPath}}/{{className}}.java",
    )
    private val forge = listOf(
        "build.gradle" to "build.gradle", "settings.gradle" to "settings.gradle",
        "gradle.properties" to "gradle.properties",
        "mods.toml" to "src/main/resources/META-INF/mods.toml",
        "pack.mcmeta" to "src/main/resources/pack.mcmeta",
        "Main.java" to "src/main/java/{{pkgPath}}/{{className}}.java",
    )

    private fun asset(path: String) = ctx.assets.open(path).bufferedReader().use { it.readText() }

    private fun vars(p: Project): Map<String, String> = mapOf(
        "modId" to p.modId, "modName" to p.name.replace("\"", "'"), "package" to p.pkg,
        "pkgPath" to p.pkg.replace('.', '/'), "className" to p.name.toClassName(),
        "modVersion" to p.modVersion, "author" to p.author.replace("\"", "'"),
        "description" to p.description.ifBlank { p.name }.replace("\"", "'"),
        "mcVersion" to p.mcVersion, "java" to p.toolchain.javaVersion.toString(),
        "gradle" to p.toolchain.gradleVersion, "loaderVersion" to p.toolchain.loaderVersion,
        "pluginVersion" to p.toolchain.pluginVersion, "apiVersion" to p.toolchain.apiVersion,
        "buildCommand" to p.toolchain.buildCommand, "branch" to p.toolchain.branch,
        "forgeMajor" to p.toolchain.loaderVersion.substringBefore('.'),
    )

    private fun render(s: String, v: Map<String, String>) =
        v.entries.fold(s) { acc, (k, x) -> acc.replace("{{$k}}", x) }

    private fun put(out: File, rel: String, text: String) {
        File(out, rel).apply { parentFile?.mkdirs(); writeText(text) }
    }

    fun generate(p: Project, out: File) {
        val v = vars(p)
        val list = when (p.loader) {
            Loader.FABRIC -> fabric
            Loader.FORGE -> forge
            Loader.NEOFORGE -> throw UnsupportedOperationException("Template NeoForge belum ada (Fase 2).")
        }
        for ((src, dst) in list) {
            put(out, render(dst, v), render(asset("templates/${p.loader.name.lowercase()}/$src"), v))
        }
        put(out, ".gitignore", asset("templates/common/gitignore"))
        writeWorkflow(p, out)
    }

    /** Mode Managed: ditulis ulang tiap Build. Mode Custom (file ada di customFiles): tidak disentuh. */
    fun writeWorkflow(p: Project, out: File) {
        if (WORKFLOW in p.toolchain.customFiles) return
        put(out, WORKFLOW, render(asset("templates/common/build.yml"), vars(p)))
    }
}
