package com.sally.ide.data

enum class ProjectTemplate(val title: String, val subtitle: String, val badge: String) {
    ANDROID_KOTLIN("Android Kotlin (Compose)", "AGP + Jetpack Compose Material 3", "Kt"),
    ANDROID_JAVA("Android Java (View)", "Activity + View klasik", "Jv"),
    FABRIC_MOD("Mod Fabric", "fabric-loom, Mojang mappings, Java 21", "Fb"),
    EMPTY("Proyek kosong", "Folder dengan README saja", "Ø")
}

/**
 * Template proyek berbentuk string supaya tidak perlu aset zip biner.
 * Placeholder: {{name}}, {{package}}, {{packagePath}}, {{modId}}, {{className}}.
 * Versi plugin/dependency sengaja ditaruh di satu tempat (gradle.properties / root build)
 * supaya mudah diganti nanti lewat Project Settings.
 */
object ProjectTemplates {

    fun render(template: ProjectTemplate, name: String, pkg: String): Map<String, String> {
        val modId = name.lowercase().filter { it.isLetterOrDigit() && it.code < 128 || it == '_' }
            .let { if (it.isEmpty() || !it.first().isLetter()) "mod$it" else it }
            .let { if (it.length < 2) it + "mod" else it }
            .take(64)
        val className = name.split(Regex("[^A-Za-z0-9]+")).filter { it.isNotEmpty() }
            .joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
            .let { if (it.isEmpty() || it.first().isDigit()) "Mod$it" else it }
        val values = mapOf(
            "{{name}}" to name,
            "{{package}}" to pkg,
            "{{packagePath}}" to pkg.replace('.', '/'),
            "{{modId}}" to modId,
            "{{className}}" to className
        )
        val raw = when (template) {
            ProjectTemplate.ANDROID_KOTLIN -> androidKotlin()
            ProjectTemplate.ANDROID_JAVA -> androidJava()
            ProjectTemplate.FABRIC_MOD -> fabricMod()
            ProjectTemplate.EMPTY -> mapOf("README.md" to "# {{name}}\n\nProyek kosong dari Sally IDE.\n")
        }
        return raw.entries.associate { (path, content) ->
            values.entries.fold(path) { acc, (k, v) -> acc.replace(k, v) } to
                values.entries.fold(content) { acc, (k, v) -> acc.replace(k, v) }
        }
    }

    private const val WRAPPER = """distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.11.1-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
"""

    private const val ANDROID_GITIGNORE = ".gradle/\n.idea/\n.kotlin/\nlocal.properties\n**/build/\n*.iml\n"

    private const val ANDROID_SETTINGS = """pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "{{name}}"
include(":app")
"""

    private const val ANDROID_MANIFEST = """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:allowBackup="true"
        android:label="{{name}}"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
"""

    private fun androidKotlin(): Map<String, String> = mapOf(
        "settings.gradle.kts" to ANDROID_SETTINGS,
        "build.gradle.kts" to """plugins {
    id("com.android.application") version "8.9.2" apply false
    id("org.jetbrains.kotlin.android") version "2.1.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.20" apply false
}
""",
        "gradle.properties" to "org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8\nandroid.useAndroidX=true\nkotlin.code.style=official\n",
        "gradle/wrapper/gradle-wrapper.properties" to WRAPPER,
        ".gitignore" to ANDROID_GITIGNORE,
        "README.md" to "# {{name}}\n\nAplikasi Android (Kotlin + Compose) dari template Sally IDE.\n\nPackage: `{{package}}`\n",
        "app/build.gradle.kts" to """plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "{{package}}"
    compileSdk = 35

    defaultConfig {
        applicationId = "{{package}}"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.02.00")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
}
""",
        "app/proguard-rules.pro" to "# Aturan ProGuard proyek.\n",
        "app/src/main/AndroidManifest.xml" to ANDROID_MANIFEST,
        "app/src/main/java/{{packagePath}}/MainActivity.kt" to """package {{package}}

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Halo dari {{name}}!")
                    }
                }
            }
        }
    }
}
"""
    )

    private fun androidJava(): Map<String, String> = mapOf(
        "settings.gradle.kts" to ANDROID_SETTINGS,
        "build.gradle.kts" to """plugins {
    id("com.android.application") version "8.9.2" apply false
}
""",
        "gradle.properties" to "org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8\nandroid.useAndroidX=true\n",
        "gradle/wrapper/gradle-wrapper.properties" to WRAPPER,
        ".gitignore" to ANDROID_GITIGNORE,
        "README.md" to "# {{name}}\n\nAplikasi Android (Java + View) dari template Sally IDE.\n\nPackage: `{{package}}`\n",
        "app/build.gradle.kts" to """plugins {
    id("com.android.application")
}

android {
    namespace = "{{package}}"
    compileSdk = 35

    defaultConfig {
        applicationId = "{{package}}"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
}
""",
        "app/proguard-rules.pro" to "# Aturan ProGuard proyek.\n",
        "app/src/main/AndroidManifest.xml" to ANDROID_MANIFEST,
        "app/src/main/java/{{packagePath}}/MainActivity.java" to """package {{package}};

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView view = new TextView(this);
        view.setText("Halo dari {{name}}!");
        view.setGravity(Gravity.CENTER);
        view.setTextSize(22f);
        setContentView(view);
    }
}
"""
    )

    private fun fabricMod(): Map<String, String> = mapOf(
        "settings.gradle" to """pluginManagement {
    repositories {
        maven {
            name = 'Fabric'
            url = 'https://maven.fabricmc.net/'
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = '{{name}}'
""",
        "gradle.properties" to """org.gradle.jvmargs=-Xmx2G
org.gradle.parallel=true

# Cek versi di https://fabricmc.net/develop sebelum build dan sesuaikan dengan Minecraft target.
minecraft_version=1.21.1
loader_version=0.16.9
fabric_version=0.115.0+1.21.1

mod_version=1.0.0
maven_group={{package}}
archives_base_name={{modId}}
""",
        "gradle/wrapper/gradle-wrapper.properties" to WRAPPER,
        "build.gradle" to """plugins {
    id 'fabric-loom' version '1.9-SNAPSHOT'
    id 'maven-publish'
}

version = project.mod_version
group = project.maven_group

base {
    archivesName = project.archives_base_name
}

repositories {
}

dependencies {
    minecraft "com.mojang:minecraft:" + project.minecraft_version
    mappings loom.officialMojangMappings()
    modImplementation "net.fabricmc:fabric-loader:" + project.loader_version
    modImplementation "net.fabricmc.fabric-api:fabric-api:" + project.fabric_version
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    withSourcesJar()
}

tasks.withType(JavaCompile).configureEach {
    it.options.release = 21
}
""",
        ".gitignore" to ".gradle/\n.idea/\nbuild/\nrun/\n*.iml\n",
        "README.md" to "# {{name}}\n\nMod Fabric dari template Sally IDE.\n\nMod ID: `{{modId}}`\n\nVersi Minecraft, loader, dan Fabric API ada di `gradle.properties`.\n",
        "src/main/resources/fabric.mod.json" to """{
  "schemaVersion": 1,
  "id": "{{modId}}",
  "version": "1.0.0",
  "name": "{{name}}",
  "description": "Mod dibuat dengan Sally IDE.",
  "authors": ["sally"],
  "license": "MIT",
  "environment": "*",
  "entrypoints": {
    "main": ["{{package}}.{{className}}"]
  },
  "depends": {
    "fabricloader": ">=0.16.0",
    "minecraft": "~1.21",
    "java": ">=21",
    "fabric-api": "*"
  }
}
""",
        "src/main/java/{{packagePath}}/{{className}}.java" to """package {{package}};

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class {{className}} implements ModInitializer {
    public static final String MOD_ID = "{{modId}}";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("{{name}} dimuat.");
    }
}
"""
    )
}
