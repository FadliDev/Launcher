package com.sally.ide.editor

/** Bahasa yang dikenali editor. [scope] = scopeName TextMate, [snippetFile] = assets/snippets/<file>.json */
enum class Lang(
    val label: String,
    val scope: String?,
    val snippetFile: String?,
    val keywords: List<String> = emptyList(),
) {
    PLAIN("Teks", null, null),
    KOTLIN("Kotlin", "source.kotlin", "kotlin", listOf(
        "fun", "val", "var", "class", "object", "interface", "data", "sealed", "enum", "when", "if", "else", "for", "while",
        "return", "import", "package", "override", "private", "public", "internal", "protected", "suspend", "companion",
        "lateinit", "by", "lazy", "null", "true", "false", "this", "super", "is", "in", "as", "try", "catch", "finally",
        "throw", "typealias", "inline", "abstract", "open", "const", "Unit", "String", "Int", "Long", "Boolean", "Float",
        "Double", "List", "Map", "Set", "mutableListOf", "mutableStateOf", "remember", "println")),
    JAVA("Java", "source.java", "java", listOf(
        "public", "private", "protected", "static", "final", "class", "interface", "enum", "extends", "implements", "import",
        "package", "return", "if", "else", "for", "while", "do", "switch", "case", "break", "continue", "new", "this", "super",
        "try", "catch", "finally", "throw", "throws", "void", "int", "long", "boolean", "float", "double", "char", "byte",
        "short", "String", "null", "true", "false", "abstract", "synchronized", "volatile", "instanceof", "System")),
    CPP("C/C++", "source.cpp", "cpp", listOf(
        "include", "define", "int", "long", "char", "float", "double", "void", "bool", "const", "static", "struct", "class",
        "namespace", "using", "template", "typename", "return", "if", "else", "for", "while", "switch", "case", "break",
        "continue", "new", "delete", "nullptr", "true", "false", "auto", "std", "vector", "string", "size_t", "uint8_t",
        "uint32_t", "int32_t", "int64_t", "virtual", "override", "public", "private", "protected", "extern", "inline")),
    XML("XML", "text.xml", "xml", listOf(
        "android:layout_width", "android:layout_height", "android:id", "android:text", "android:orientation",
        "android:name", "android:exported", "match_parent", "wrap_content", "xmlns:android", "xmlns:app", "tools:ignore")),
    GRADLE("Gradle", "source.groovy", "groovy", listOf(
        "plugins", "dependencies", "repositories", "android", "implementation", "api", "compileOnly", "runtimeOnly",
        "testImplementation", "id", "version", "apply", "google", "mavenCentral", "namespace", "compileSdk", "minSdk",
        "targetSdk", "defaultConfig", "buildTypes", "release", "debug", "tasks", "register", "doLast", "true", "false")),
    JSON("JSON", "source.json", null, listOf("true", "false", "null")),
    TOML("TOML", "source.toml", null, listOf("true", "false")),
    PROPERTIES("Properties", "source.properties", null),
    MARKDOWN("Markdown", "text.html.markdown", "markdown"),
    ;

    companion object {
        fun forFile(name: String): Lang {
            val n = name.lowercase()
            return when {
                n.endsWith(".kt") -> KOTLIN
                n.endsWith(".kts") -> GRADLE
                n.endsWith(".gradle") -> GRADLE
                n.endsWith(".java") -> JAVA
                n.endsWith(".c") || n.endsWith(".cc") || n.endsWith(".cpp") || n.endsWith(".cxx") ||
                    n.endsWith(".h") || n.endsWith(".hpp") || n.endsWith(".hh") -> CPP
                n.endsWith(".xml") || n.endsWith(".svg") || n.endsWith(".html") -> XML
                n.endsWith(".json") || n.endsWith(".mcmeta") -> JSON
                n.endsWith(".toml") -> TOML
                n.endsWith(".properties") -> PROPERTIES
                n.endsWith(".md") || n.endsWith(".markdown") -> MARKDOWN
                else -> PLAIN
            }
        }
    }
}
