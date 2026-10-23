package com.sally.modforge.model

/** Membuat ProjectConfig baru dengan nilai default yang sudah kompatibel. */
object ProjectFactory {

    const val AUTHOR_PACKAGE = "com.sally"

    /** "My Cool Mod" -> "my_cool_mod". Selalu menghasilkan mod ID yang valid. */
    fun slugModId(name: String): String {
        val slug = name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').take(64)
        return when {
            slug.isEmpty() -> "mymod"
            slug[0].isDigit() -> "m_$slug".take(64)
            slug.length < 2 -> "${slug}_mod"
            else -> slug
        }
    }

    fun defaultPackage(modId: String): String = "$AUTHOR_PACKAGE.$modId"

    fun create(
        id: String,
        name: String,
        modId: String,
        packageName: String,
        minecraft: String,
        java: Int,
        matrix: VersionMatrix,
    ): ProjectConfig = ProjectConfig(
        id = id,
        name = name,
        modId = modId,
        packageName = packageName,
        loader = "fabric",
        minecraft = minecraft,
        java = java,
        gradle = matrix.find(minecraft)?.gradle.orEmpty(),
        template = "empty",
    )
}
