package com.sally.ide.editor

import android.content.Context
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import org.eclipse.tm4e.core.registry.IThemeSource

/** Inisialisasi TextMate (grammar + tema) sekali. Kalau gagal, editor tetap jalan tanpa highlight. */
object EditorRuntime {
    private var ready = false
    /** Mode aman: tanpa TextMate (tanpa highlight) — dipakai kalau highlight bikin crash. */
    @Volatile var safe = false
        private set
    var failure: String? = null
        private set

    @Synchronized
    fun init(context: Context) {
        if (ready) return
        ready = true
        if (com.sally.ide.CrashLog.isSafe(context)) { safe = true; return }
        runCatching {
            val files = FileProviderRegistry.getInstance()
            files.addFileProvider(AssetsFileResolver(context.applicationContext.assets))
            val themes = ThemeRegistry.getInstance()
            for (name in listOf("dark", "amoled", "light")) {
                val path = "textmate/themes/$name.json"
                val stream = files.tryGetInputStream(path) ?: continue
                val model = ThemeModel(IThemeSource.fromInputStream(stream, path, null), name)
                model.isDark = name != "light"
                themes.loadTheme(model)
            }
            GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
        }.onFailure { failure = it.message ?: it.javaClass.simpleName }
    }

    fun colorScheme(theme: String): EditorColorScheme = if (safe) EditorColorScheme() else runCatching {
        ThemeRegistry.getInstance().setTheme(theme)
        (TextMateColorScheme.create(ThemeRegistry.getInstance()) as EditorColorScheme).also { applyChrome(it, theme) }
    }.getOrElse { EditorColorScheme() }

    /** Warna "chrome" editor (gutter, handle, scrollbar, pencarian) disamakan dengan token MD3 biru di ui/Theme.kt. */
    private fun applyChrome(s: EditorColorScheme, theme: String) {
        val light = theme == "light"
        val bg = when (theme) { "amoled" -> 0xFF000000; "light" -> 0xFFF9F9FF; else -> 0xFF111318 }.toInt()
        val primary = if (light) 0xFF0B57D0.toInt() else 0xFFADC6FF.toInt()
        val outlineVar = if (light) 0xFFC4C6D0.toInt() else 0xFF44474E.toInt()
        val thumb = if (light) 0x660B57D0 else 0x66ADC6FF
        val match = if (light) 0xFFFFE08A.toInt() else 0xFF5C4A00.toInt()
        val fg = if (light) 0xFF191C20.toInt() else 0xFFE2E2E9.toInt()
        val bracketBg = if (light) 0xFFD8E2FF.toInt() else 0xFF1F3B6E.toInt()
        s.setColor(EditorColorScheme.WHOLE_BACKGROUND, bg)
        s.setColor(EditorColorScheme.TEXT_NORMAL, fg) // teks tanpa scope (kurung, tanda baca)
        s.setColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_FOREGROUND, primary)
        s.setColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_BACKGROUND, bracketBg)
        s.setColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_UNDERLINE, primary)
        s.setColor(EditorColorScheme.LINE_NUMBER_BACKGROUND, bg)
        s.setColor(EditorColorScheme.LINE_DIVIDER, outlineVar)
        s.setColor(EditorColorScheme.SELECTION_INSERT, primary)
        s.setColor(EditorColorScheme.SELECTION_HANDLE, primary)
        s.setColor(EditorColorScheme.SCROLL_BAR_THUMB, thumb.toInt())
        s.setColor(EditorColorScheme.SCROLL_BAR_THUMB_PRESSED, primary)
        s.setColor(EditorColorScheme.MATCHED_TEXT_BACKGROUND, match)
        s.setColor(EditorColorScheme.BLOCK_LINE, outlineVar)
        s.setColor(EditorColorScheme.BLOCK_LINE_CURRENT, primary)
    }

    fun language(lang: Lang): Language {
        val scope = lang.scope ?: return EmptyLanguage()
        if (safe) return EmptyLanguage()
        // createIdentifiers=false: completion ditangani sendiri (Completion.kt) lewat suggestion bar.
        return runCatching { TextMateLanguage.create(scope, false) as Language }.getOrElse { EmptyLanguage() }
    }
}
