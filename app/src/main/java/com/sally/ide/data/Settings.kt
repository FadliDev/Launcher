package com.sally.ide.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.store by preferencesDataStore(name = "sally_ide_settings")

enum class ThemeMode(val label: String) { SYSTEM("Ikuti sistem"), LIGHT("Terang"), DARK("Gelap"), AMOLED("AMOLED") }

data class Prefs(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val fontSize: Float = 14f,
    val wordWrap: Boolean = false,
    val lineNumbers: Boolean = true,
    val tabSize: Int = 4,
)

/** Tab yang terbuka di sebuah proyek (path relatif) + indeks tab aktif. */
data class Session(val paths: List<String>, val active: Int)

object Keys {
    val THEME = stringPreferencesKey("theme_mode")
    val FONT = floatPreferencesKey("font_size")
    val WRAP = booleanPreferencesKey("word_wrap")
    val LINES = booleanPreferencesKey("line_numbers")
    val TAB = intPreferencesKey("tab_size")
}

class SettingsRepo(private val ctx: Context) {
    val flow: Flow<Prefs> = ctx.store.data.map { p ->
        Prefs(
            themeMode = p[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            fontSize = (p[Keys.FONT] ?: 14f).coerceIn(10f, 24f),
            wordWrap = p[Keys.WRAP] ?: false,
            lineNumbers = p[Keys.LINES] ?: true,
            tabSize = (p[Keys.TAB] ?: 4).coerceIn(2, 8),
        )
    }

    suspend fun loadSession(project: String): Session {
        val raw = ctx.store.data.first()[stringPreferencesKey("session:$project")] ?: return Session(emptyList(), -1)
        val lines = raw.split('\n')
        return Session(lines.drop(1).filter { it.isNotBlank() }, lines.firstOrNull()?.toIntOrNull() ?: -1)
    }

    suspend fun saveSession(project: String, paths: List<String>, active: Int) {
        ctx.store.edit { it[stringPreferencesKey("session:$project")] = (listOf(active.toString()) + paths).joinToString("\n") }
    }

    suspend fun edit(block: (MutablePreferences) -> Unit) {
        ctx.store.edit { block(it) }
    }
}
