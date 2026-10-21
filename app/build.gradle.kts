import java.net.URL
import java.util.zip.ZipInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.sally.editor"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sally.editor"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.2.0"
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-ktx:1.10.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    // Sora Editor: incremental syntax highlighting, auto-complete and editor-native undo/redo.
    implementation("io.github.Rosemoe.sora-editor:editor:0.23.6")
    implementation("io.github.Rosemoe.sora-editor:language-textmate:0.23.6")
    // Sora Editor LSP client integration module. Language servers are separate runtime components.
    implementation("io.github.Rosemoe.sora-editor:editor-lsp:0.23.6")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
}


// Bundle the upstream Friendly Snippets VS Code-compatible JSON collection at build time.
// The APK can then use snippets offline; the repository is not fetched on every app launch.
val friendlySnippetsAssets = layout.buildDirectory.dir("generated/friendly-snippets/assets")
val downloadFriendlySnippets by tasks.registering {
    val outputDir = friendlySnippetsAssets
    outputs.dir(outputDir)
    doLast {
        val root = outputDir.get().asFile
        val snippetsDir = root.resolve("friendly-snippets/snippets")
        if (snippetsDir.exists() && snippetsDir.listFiles()?.isNotEmpty() == true) return@doLast
        snippetsDir.mkdirs()
        val archive = root.resolve("friendly-snippets.zip")
        val url = URL("https://github.com/rafamadriz/friendly-snippets/archive/refs/heads/main.zip")
        val connection: java.net.URLConnection = url.openConnection()
        connection.connectTimeout = 20_000
        connection.readTimeout = 60_000
        connection.getInputStream().use { input ->
            archive.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        ZipInputStream(archive.inputStream().buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val marker = "/snippets/"
                val index = entry.name.indexOf(marker)
                if (!entry.isDirectory && index >= 0 && entry.name.endsWith(".json")) {
                    val relative = entry.name.substring(index + marker.length)
                    // The editor currently consumes top-level language packs; nested framework packs
                    // are also bundled in their relative folders for later extension.
                    val target = snippetsDir.resolve(relative)
                    target.parentFile?.mkdirs()
                    target.outputStream().use { zip.copyTo(it) }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        archive.delete()
        check(snippetsDir.listFiles()?.isNotEmpty() == true) {
            "Friendly Snippets download succeeded but no JSON snippet packs were found"
        }
    }
}

android.sourceSets.getByName("main").assets.srcDir(friendlySnippetsAssets)
tasks.named("preBuild").configure { dependsOn(downloadFriendlySnippets) }
