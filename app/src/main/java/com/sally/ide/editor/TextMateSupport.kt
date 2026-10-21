package com.sally.ide.editor

import android.content.Context
import android.util.Log
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import org.eclipse.tm4e.core.registry.IThemeSource

/** Loads bundled TextMate grammars once and maps common source-file extensions to scopes. */
object TextMateSupport {
    private const val TAG = "SallyTextMate"
    @Volatile private var initialized = false
    @Volatile private var initializationFailed = false

    private val scopes = mapOf(
        "kt" to "source.kotlin", "kts" to "source.kotlin", "java" to "source.java",
        "js" to "source.js", "jsx" to "source.js", "mjs" to "source.js", "cjs" to "source.js",
        "ts" to "source.ts", "tsx" to "source.ts", "html" to "text.html.basic", "htm" to "text.html.basic",
        "css" to "source.css", "scss" to "source.css", "less" to "source.css",
        "json" to "source.json", "jsonc" to "source.json", "py" to "source.python",
        "c" to "source.c", "h" to "source.c", "cpp" to "source.cpp", "cc" to "source.cpp",
        "cxx" to "source.cpp", "hpp" to "source.cpp", "hh" to "source.cpp", "hxx" to "source.cpp",
        "xml" to "text.xml", "svg" to "text.xml", "gradle" to "source.groovy", "groovy" to "source.groovy",
        "md" to "text.markdown", "mdx" to "text.markdown", "toml" to "source.toml", "ini" to "source.ini",
        "properties" to "source.ini", "yaml" to "source.yaml", "yml" to "source.yaml", "sh" to "source.shell",
        "bash" to "source.shell", "zsh" to "source.shell", "sql" to "source.sql", "rs" to "source.rust",
        "go" to "source.go", "php" to "source.php", "rb" to "source.ruby", "dart" to "source.dart",
        "swift" to "source.swift", "lua" to "source.lua", "ktm" to "source.kotlin"
    )

    fun initialize(context: Context) {
        if (initialized || initializationFailed) return
        synchronized(this) {
            if (initialized || initializationFailed) return
            try {
                FileProviderRegistry.getInstance().addFileProvider(
                    AssetsFileResolver(context.applicationContext.assets)
                )
                val registry = ThemeRegistry.getInstance()
                GrammarRegistry.getInstance()
                loadTheme(context, "sally-dark", true, false)
                loadTheme(context, "sally-light", false, false)
                registry.setTheme("sally-dark")
                GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
                initialized = true
            } catch (error: Exception) {
                initializationFailed = true
                Log.e(TAG, "Could not initialize TextMate assets; plain editor fallback is enabled", error)
            }
        }
    }

    private fun loadTheme(context: Context, name: String, dark: Boolean, activate: Boolean) {
        val path = "textmate/$name.json"
        val stream = context.assets.open(path)
        val source = IThemeSource.fromInputStream(stream, path, null)
        val model = ThemeModel(source, name).apply { setDark(dark) }
        ThemeRegistry.getInstance().loadTheme(model, activate)
    }

    fun setTheme(dark: Boolean) {
        if (!initialized) return
        ThemeRegistry.getInstance().setTheme(if (dark) "sally-dark" else "sally-light")
    }

    fun colorScheme(): TextMateColorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())

    fun extension(fileName: String): String = fileName.substringAfterLast('.', "").lowercase()

    fun scopeFor(fileName: String): String? = scopes[extension(fileName)]

    fun languageFor(fileName: String): Language {
        val scope = scopeFor(fileName)
        if (!initialized || scope == null) return EmptyLanguage()
        return runCatching { TextMateLanguage.create(scope, true) }
            .onFailure { Log.w(TAG, "No grammar for $fileName", it) }
            .getOrElse { EmptyLanguage() }
    }

    fun languageLabel(fileName: String): String {
        val ext = extension(fileName)
        return when (ext) {
            "kt", "kts" -> "Kotlin"
            "java" -> "Java"
            "js", "jsx", "mjs", "cjs" -> "JavaScript"
            "ts", "tsx" -> "TypeScript"
            "html", "htm" -> "HTML"
            "css", "scss", "less" -> "CSS"
            "json", "jsonc" -> "JSON"
            "py" -> "Python"
            "c", "h" -> "C"
            "cpp", "cc", "cxx", "hpp", "hh", "hxx" -> "C++"
            "xml", "svg" -> "XML"
            "gradle", "groovy" -> "Gradle/Groovy"
            "md", "mdx" -> "Markdown"
            "toml" -> "TOML"
            "yml", "yaml" -> "YAML"
            "sh", "bash", "zsh" -> "Shell"
            "sql" -> "SQL"
            "rs" -> "Rust"
            "go" -> "Go"
            "php" -> "PHP"
            "rb" -> "Ruby"
            "dart" -> "Dart"
            "swift" -> "Swift"
            "lua" -> "Lua"
            "ini", "properties" -> "Properties"
            "" -> "Plain Text"
            else -> ext.uppercase()
        }
    }
}
