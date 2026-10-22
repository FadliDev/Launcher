package com.sally.modforge.data

import android.content.Context
import com.sally.modforge.model.VersionProfile
import kotlinx.serialization.json.Json

/** Aturan per versi dibaca dari data (assets/version-profiles.json), bukan hardcode di kode. */
object VersionProfiles {
    fun load(ctx: Context): List<VersionProfile> {
        val text = ctx.assets.open("version-profiles.json").bufferedReader().use { it.readText() }
        return Json { ignoreUnknownKeys = true }.decodeFromString(text)
    }
}
