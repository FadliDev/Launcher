plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.sally.ember"
    compileSdk = 34
    ndkVersion = "25.2.9519653"
    defaultConfig {
        applicationId = "com.sally.ember"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.2.0"
        ndk { abiFilters += listOf("arm64-v8a") }
    }
    externalNativeBuild { ndkBuild { path = file("src/main/jni/Android.mk") } }
    buildTypes {
        release { isMinifyEnabled = false; signingConfig = signingConfigs.getByName("debug") }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; prefab = true }
    packaging { jniLibs { useLegacyPackaging = true; pickFirsts += listOf("**/libbytehook.so", "**/libc++_shared.so") } }
    androidResources { noCompress += listOf("xz", "zip", "jar") }
}
dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))
    implementation("com.bytedance:bytehook:1.0.10")
    implementation("androidx.annotation:annotation:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.tukaani:xz:1.9")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
