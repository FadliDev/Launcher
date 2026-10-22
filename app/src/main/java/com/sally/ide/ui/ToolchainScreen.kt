package com.sally.ide.ui

import android.text.format.Formatter
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sally.ide.IdeViewModel
import com.sally.ide.data.BuildConfig
import com.sally.ide.data.Compat
import com.sally.ide.data.Pkg
import com.sally.ide.data.PkgKind
import com.sally.ide.data.ProjectConfig
import com.sally.ide.data.ToolchainModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolchainScreen(vm: IdeViewModel, snackbar: SnackbarHostState) {
    val tc = vm.toolchain
    LaunchedEffect(Unit) { tc.refresh() }
    val ctx = LocalContext.current
    fun size(b: Long) = Formatter.formatShortFileSize(ctx, b)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Toolchain") },
                navigationIcon = { IconButton(onClick = { vm.showToolchain = false }) { Icon(Ic.Back, "Kembali") } },
            )
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(Modifier.widthIn(max = 640.dp).fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    ListItem(
                        headlineContent = { Text("Penyimpanan toolchain") },
                        supportingContent = { Text("${size(tc.usedBytes)} terpakai · cache unduhan ${size(tc.cacheBytes)}") },
                        trailingContent = {
                            TextButton(onClick = { tc.clearCache() }, enabled = tc.cacheBytes > 0) { Text("Bersihkan cache") }
                        },
                    )
                }
                item { SectionTitle("Gradle") }
                items(tc.catalog.filter { it.kind == PkgKind.GRADLE }, key = { it.id }) { PkgRow(it, tc) }
                item { SectionTitle("Android platform (android.jar)") }
                items(tc.catalog.filter { it.kind == PkgKind.PLATFORM }, key = { it.id }) { PkgRow(it, tc) }
                item { SectionTitle("Belum tersedia") }
                item {
                    Text(
                        "JDK, build-tools (aapt2, d8), dan NDK menunggu spike Fase 0: cara menjalankan biner di perangkat harus terbukti dulu.",
                        Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun PkgRow(p: Pkg, tc: ToolchainModel) {
    val busy = tc.progress[p.id]
    val installed = tc.installed[p.id] == true
    ListItem(
        headlineContent = { Text(p.title) },
        supportingContent = {
            Text(
                when {
                    busy != null -> if (busy >= 0f) "Mengunduh… ${(busy * 100).toInt()}%" else "Mengunduh…"
                    installed -> "Terpasang"
                    else -> p.desc
                },
            )
        },
        trailingContent = {
            when {
                busy != null -> Row(verticalAlignment = Alignment.CenterVertically) {
                    if (busy >= 0f) CircularProgressIndicator(progress = { busy }, Modifier.size(24.dp), strokeWidth = 3.dp)
                    else CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
                    IconButton(onClick = { tc.cancel(p) }) { Icon(Ic.Close, "Batal") }
                }
                installed -> IconButton(onClick = { tc.remove(p) }) { Icon(Ic.Delete, "Hapus") }
                else -> IconButton(onClick = { tc.install(p) }) { Icon(Ic.Download, "Unduh") }
            }
        },
    )
}

/** Setelan build per proyek → `.sallyide/project.json`, plus peringatan kompatibilitas Gradle ↔ AGP ↔ JDK. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildSettingsSheet(project: File, tc: ToolchainModel, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var cfg by remember { mutableStateOf<BuildConfig?>(null) }
    var wrapper by remember { mutableStateOf<String?>(null) }
    var agp by remember { mutableStateOf<String?>(null) }
    var sdkText by remember { mutableStateOf("") }
    var ndkText by remember { mutableStateOf("") }
    var argsText by remember { mutableStateOf("") }
    var gradle by remember { mutableStateOf<String?>(null) }
    var jdk by remember { mutableStateOf(17) }
    val installed = remember { tc.installedGradle() }

    LaunchedEffect(project) {
        withContext(Dispatchers.IO) {
            val c = ProjectConfig.load(project)
            val w = ProjectConfig.wrapperVersion(project)
            val a = ProjectConfig.detectAgp(project)
            withContext(Dispatchers.Main) {
                cfg = c; wrapper = w; agp = a
                sdkText = c.compileSdk?.toString() ?: ""; ndkText = c.ndkVersion ?: ""; argsText = c.jvmArgs
                gradle = c.gradleVersion; jdk = c.jdkVersion
            }
        }
    }

    val effectiveGradle = gradle ?: wrapper
    val warnings = Compat.warnings(effectiveGradle, jdk, agp)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("Setelan build", style = MaterialTheme.typography.titleMedium)
            Text(
                "Disimpan di .sallyide/project.json" + (agp?.let { " · AGP terdeteksi $it" } ?: ""),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text("Versi Gradle", Modifier.padding(top = 12.dp), style = MaterialTheme.typography.labelLarge)
            Row(Modifier.padding(top = 4.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = gradle == null, onClick = { gradle = null }, label = { Text("Wrapper proyek" + (wrapper?.let { " ($it)" } ?: "")) })
                installed.forEach { v -> FilterChip(selected = gradle == v, onClick = { gradle = v }, label = { Text(v) }) }
            }
            if (installed.isEmpty()) {
                Text("Belum ada Gradle terpasang — unduh dari menu Toolchain.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Text("JDK", Modifier.padding(top = 12.dp), style = MaterialTheme.typography.labelLarge)
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(8, 17, 21).forEach { n -> FilterChip(selected = jdk == n, onClick = { jdk = n }, label = { Text("$n") }) }
            }

            OutlinedTextField(
                sdkText, { sdkText = it.filter(Char::isDigit).take(3) }, Modifier.fillMaxWidth().padding(top = 12.dp),
                label = { Text("compileSdk (kosong = ikuti proyek)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            OutlinedTextField(
                ndkText, { ndkText = it }, Modifier.fillMaxWidth().padding(top = 8.dp),
                label = { Text("Versi NDK (opsional)") }, singleLine = true,
            )
            OutlinedTextField(
                argsText, { argsText = it }, Modifier.fillMaxWidth().padding(top = 8.dp),
                label = { Text("JVM args") }, singleLine = true,
            )

            if (warnings.isNotEmpty()) {
                Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    warnings.forEach { Text("⚠ $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                }
            }

            FilledTonalButton(
                onClick = {
                    val c = BuildConfig(gradle, jdk, sdkText.toIntOrNull(), ndkText.ifBlank { null }, argsText.ifBlank { "-Xmx1536m" })
                    scope.launch { withContext(Dispatchers.IO) { runCatching { ProjectConfig.save(project, c) } }; onDismiss() }
                },
                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
                enabled = cfg != null,
            ) { Text("Simpan") }
        }
    }
}
