package com.sally.modforge.data

import org.json.JSONObject


enum class ModLoader(val label: String) {
    FABRIC("Fabric"), FORGE("Forge"), NEOFORGE("NeoForge");

    companion object {
        fun from(value: String?): ModLoader = entries.firstOrNull { it.name == value } ?: FABRIC
    }
}

enum class ModTemplate(val label: String, val description: String) {
    EMPTY("Empty Mod", "Project dasar untuk mulai menulis kode."),
    ITEM("Item", "Contoh item terdaftar untuk Fabric 1.21.1."),
    BLOCK("Block", "Contoh block terdaftar untuk Fabric 1.21.1."),
    COMMAND("Command", "Contoh command /hello untuk Fabric 1.21.1.");

    companion object {
        fun from(value: String?): ModTemplate = entries.firstOrNull { it.name == value } ?: EMPTY
    }
}

data class ModProject(
    val id: String = "",
    val name: String,
    val modId: String,
    val packageName: String,
    val modVersion: String = "1.0.0",
    val author: String = "",
    val description: String = "A Minecraft Java mod",
    val license: String = "MIT",
    val loader: ModLoader = ModLoader.FABRIC,
    val minecraftVersion: String = "1.21.1",
    val template: ModTemplate = ModTemplate.EMPTY,
    val buildStatus: String = "Belum pernah build",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("modId", modId)
        .put("packageName", packageName)
        .put("modVersion", modVersion)
        .put("author", author)
        .put("description", description)
        .put("license", license)
        .put("loader", loader.name)
        .put("minecraftVersion", minecraftVersion)
        .put("template", template.name)
        .put("buildStatus", buildStatus)
        .put("createdAt", createdAt)
        .put("updatedAt", updatedAt)

    companion object {
        fun fromJson(json: JSONObject) = ModProject(
            id = json.optString("id"),
            name = json.optString("name", "Untitled Mod"),
            modId = json.optString("modId", "example_mod"),
            packageName = json.optString("packageName", "com.example.examplemod"),
            modVersion = json.optString("modVersion", "1.0.0"),
            author = json.optString("author", ""),
            description = json.optString("description", "A Minecraft Java mod"),
            license = json.optString("license", "MIT"),
            loader = ModLoader.from(json.optString("loader")),
            minecraftVersion = json.optString("minecraftVersion", "1.21.1"),
            template = ModTemplate.from(json.optString("template")),
            buildStatus = json.optString("buildStatus", "Belum pernah build"),
            createdAt = json.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
        )
    }
}
