package com.sally.ember.game

import java.io.File

data class Renderer(
    val id: String,        // POJAV_RENDERER value; must start with "opengles" to select the GLES bridge in libpojavexec
    val name: String,
    val library: String,   // dlopen'd and passed as -Dorg.lwjgl.opengl.libname
    val env: Map<String, String>
)

object Renderers {
    /** NG-GL4ES. Built by CI from BZLZHH/NG-GL4ES into jniLibs as libng_gl4es.so. */
    val KRYPTON = Renderer(
        id = "opengles3_krypton", name = "Krypton Wrapper", library = "libng_gl4es.so",
        env = mapOf("LIBGL_ES" to "3", "LIBGL_MIPMAP" to "3", "LIBGL_NOERROR" to "1", "LIBGL_NOINTOVLHACK" to "1", "LIBGL_NORMALIZE" to "1")
    )
    val GL4ES = Renderer(
        id = "opengles2", name = "GL4ES", library = "libgl4es_114.so",
        env = mapOf("LIBGL_ES" to "2", "LIBGL_MIPMAP" to "3", "LIBGL_NOERROR" to "1", "LIBGL_NOINTOVLHACK" to "1", "LIBGL_NORMALIZE" to "1")
    )

    val all = listOf(KRYPTON, GL4ES)
    val DEFAULT = KRYPTON

    fun isAvailable(r: Renderer, nativeDir: File) = File(nativeDir, r.library).exists()

    /** Chosen renderer if its library is in the APK, otherwise the first one that is (Krypton → GL4ES). */
    fun resolve(id: String?, nativeDir: File): Renderer {
        val wanted = all.firstOrNull { it.id == id } ?: DEFAULT
        if (isAvailable(wanted, nativeDir)) return wanted
        return all.firstOrNull { isAvailable(it, nativeDir) } ?: GL4ES
    }
}
