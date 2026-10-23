package com.sally.modforge.model

enum class Severity { ERROR, WARNING }

data class ValidationIssue(
    val field: String,
    val message: String,
    val severity: Severity,
    /** Saran Java untuk tombol "Perbaiki otomatis", null bila tidak ada */
    val fixedJava: Int? = null,
)

private val MOD_ID = Regex("^[a-z][a-z0-9_]{1,63}$")
private val PACKAGE = Regex("""^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$""")

fun validate(config: ProjectConfig, matrix: VersionMatrix): List<ValidationIssue> {
    val issues = mutableListOf<ValidationIssue>()

    if (config.name.isBlank()) {
        issues += ValidationIssue("name", "Nama mod wajib diisi.", Severity.ERROR)
    }
    if (!MOD_ID.matches(config.modId)) {
        issues += ValidationIssue("modId", "Mod ID hanya huruf kecil, angka, dan underscore (2–64 karakter).", Severity.ERROR)
    }
    if (!PACKAGE.matches(config.packageName)) {
        issues += ValidationIssue("package", "Package harus berbentuk com.nama.mod (huruf kecil).", Severity.ERROR)
    }
    if (config.loader != "fabric") {
        issues += ValidationIssue("loader", "Loader ${config.loader} belum didukung.", Severity.ERROR)
    }

    val entry = matrix.find(config.minecraft)
    if (entry == null) {
        issues += ValidationIssue("minecraft", "Minecraft ${config.minecraft} belum ada di daftar versi.", Severity.ERROR)
        return issues
    }

    if (config.java !in entry.javaMin..entry.javaMax) {
        issues += ValidationIssue(
            field = "java",
            message = "Java ${config.java} tidak cocok dengan Minecraft ${config.minecraft}. Pakai Java ${entry.javaRecommended}?",
            severity = Severity.ERROR,
            fixedJava = entry.javaRecommended,
        )
    }
    if (entry.stability == "experimental") {
        issues += ValidationIssue(
            "minecraft",
            "Minecraft ${config.minecraft} masih eksperimental; build belum tentu lulus.",
            Severity.WARNING,
        )
    }
    return issues
}
