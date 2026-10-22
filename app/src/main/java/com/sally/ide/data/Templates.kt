package com.sally.ide.data

class Template(val id: String, val label: String, val desc: String, val files: (String) -> Map<String, String>)

object Templates {
    private fun slug(name: String): String {
        val s = name.lowercase().filter { it in 'a'..'z' || it in '0'..'9' }
        return when {
            s.isEmpty() -> "app"
            s[0].isDigit() -> "p$s"
            else -> s
        }
    }

    private fun pkg(name: String) = "com.example." + slug(name)

    private const val GITIGNORE = "build/\n.gradle/\n.idea/\nlocal.properties\n*.iml\n"

    val empty = Template("empty", "Kosong", "README + satu file Kotlin") { n ->
        mapOf(
            "README.md" to "# $n\n\n",
            "Main.kt" to "fun main() {\n    println(\"Halo dari $n\")\n}\n",
        )
    }

    val androidKotlin = Template("android-kotlin", "Android Kotlin (Compose)", "Aplikasi Compose minimal, AGP 8.7 / Kotlin 2.0") { n ->
        val p = pkg(n)
        val dir = p.replace('.', '/')
        mapOf(
            ".gitignore" to GITIGNORE,
            "gradle.properties" to "org.gradle.jvmargs=-Xmx1536m\nandroid.useAndroidX=true\n",
            "settings.gradle.kts" to """
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositories { google(); mavenCentral() }
}
rootProject.name = "$n"
include(":app")
""".trimStart(),
            "build.gradle.kts" to """
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}
""".trimStart(),
            "app/build.gradle.kts" to """
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "$p"
    compileSdk = 35
    defaultConfig {
        applicationId = "$p"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
}
""".trimStart(),
            "app/src/main/AndroidManifest.xml" to """
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:label="$n"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">
        <activity android:name=".MainActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
""".trimStart(),
            "app/src/main/java/$dir/MainActivity.kt" to """
package $p

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { Text("Halo, $n!") } }
    }
}
""".trimStart(),
        )
    }

    val androidJava = Template("android-java", "Android Java", "Aplikasi View minimal tanpa library tambahan") { n ->
        val p = pkg(n)
        val dir = p.replace('.', '/')
        mapOf(
            ".gitignore" to GITIGNORE,
            "gradle.properties" to "org.gradle.jvmargs=-Xmx1536m\nandroid.useAndroidX=true\n",
            "settings.gradle.kts" to """
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositories { google(); mavenCentral() }
}
rootProject.name = "$n"
include(":app")
""".trimStart(),
            "build.gradle.kts" to """
plugins {
    id("com.android.application") version "8.7.3" apply false
}
""".trimStart(),
            "app/build.gradle.kts" to """
plugins {
    id("com.android.application")
}

android {
    namespace = "$p"
    compileSdk = 35
    defaultConfig {
        applicationId = "$p"
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
""".trimStart(),
            "app/src/main/AndroidManifest.xml" to """
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:label="$n"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">
        <activity android:name=".MainActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
""".trimStart(),
            "app/src/main/java/$dir/MainActivity.java" to """
package $p;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView t = new TextView(this);
        t.setText("Halo, $n!");
        t.setTextSize(24);
        setContentView(t);
    }
}
""".trimStart(),
        )
    }

    val fabricMod = Template("fabric-mod", "Mod Fabric", "Kerangka mod Fabric. Versi Loom/Loader/API harus diisi dari fabricmc.net/develop") { n ->
        val id = slug(n)
        val p = pkg(n)
        val dir = p.replace('.', '/')
        mapOf(
            ".gitignore" to GITIGNORE + "run/\n",
            "README.md" to "# $n\n\nIsi versi di `gradle.properties` dan versi plugin Loom di `build.gradle`\nmemakai generator resmi: https://fabricmc.net/develop\n",
            "gradle.properties" to "org.gradle.jvmargs=-Xmx2G\n\nminecraft_version=1.21.11\nloader_version=ISI_DARI_FABRICMC_NET\nfabric_version=ISI_DARI_FABRICMC_NET\n\nmod_version=1.0.0\nmaven_group=$p\n",
            "settings.gradle" to """
pluginManagement {
    repositories {
        maven { url = 'https://maven.fabricmc.net/' }
        mavenCentral()
        gradlePluginPortal()
    }
}
rootProject.name = '$id'
""".trimStart(),
            "build.gradle" to """
plugins {
    id 'fabric-loom' version 'ISI_VERSI_LOOM'
}

version = project.mod_version
group = project.maven_group

dependencies {
    minecraft "com.mojang:minecraft:" + project.minecraft_version
    mappings loom.officialMojangMappings()
    modImplementation "net.fabricmc:fabric-loader:" + project.loader_version
    modImplementation "net.fabricmc.fabric-api:fabric-api:" + project.fabric_version
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
""".trimStart(),
            "src/main/resources/fabric.mod.json" to """
{
  "schemaVersion": 1,
  "id": "$id",
  "version": "1.0.0",
  "name": "$n",
  "description": "",
  "environment": "*",
  "entrypoints": {
    "main": ["$p.ModMain"]
  },
  "depends": {
    "fabricloader": "*",
    "minecraft": "*"
  }
}
""".trimStart(),
            "src/main/java/$dir/ModMain.java" to """
package $p;

import net.fabricmc.api.ModInitializer;

public class ModMain implements ModInitializer {
    @Override
    public void onInitialize() {
        System.out.println("$n dimuat");
    }
}
""".trimStart(),
        )
    }

    val all = listOf(androidKotlin, androidJava, fabricMod, empty)
}
