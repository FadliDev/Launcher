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
    var failure: String? = null
        private set

    @Synchronized
    fun init(context: Context) {
        if (ready) return
        ready = true
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

    fun colorScheme(theme: String): EditorColorScheme = runCatching {
        ThemeRegistry.getInstance().setTheme(theme)
        TextMateColorScheme.create(ThemeRegistry.getInstance()) as EditorColorScheme
    }.getOrElse { EditorColorScheme() }

    fun language(lang: Lang): Language {
        val scope = lang.scope ?: return EmptyLanguage()
        // createIdentifiers=false: completion ditangani sendiri (Completion.kt) lewat suggestion bar.
        return runCatching { TextMateLanguage.create(scope, false) as Language }.getOrElse { EmptyLanguage() }
    }
}
