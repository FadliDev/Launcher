package com.sally.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sally.launcher.data.MinecraftVersion
import com.sally.launcher.data.MinecraftVersionRepository
import com.sally.launcher.data.InstallResult
import com.sally.launcher.engine.UnavailableMinecraftEngineAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val Ink = Color(0xFF10131A)
private val Panel = Color(0xFF1B202B)
private val Mint = Color(0xFF8CE6C0)
private val SoftText = Color(0xFFB5C0D0)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = MinecraftVersionRepository(applicationContext)
        setContent { SallyTheme { LauncherScreen(repository) } }
    }
}

@Composable
private fun SallyTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(
        primary = Mint,
        onPrimary = Ink,
        background = Ink,
        onBackground = Color(0xFFF5F7FA),
        surface = Panel,
        onSurface = Color(0xFFF5F7FA),
        secondary = Color(0xFFB9C8FF)
    ), content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LauncherScreen(repository: MinecraftVersionRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var versions by remember { mutableStateOf<List<MinecraftVersion>>(emptyList()) }
    var selected by remember { mutableStateOf<MinecraftVersion?>(null) }
    var installed by remember { mutableStateOf<InstallResult?>(null) }
    var loading by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf("release") }
    var status by remember { mutableStateOf("Siap menyiapkan Minecraft.") }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun refreshVersions() {
        loading = true
        error = null
        try {
            val fetched = withContext(Dispatchers.IO) { repository.fetchVersions() }
            versions = fetched
            if (selected == null) selected = fetched.firstOrNull { it.type == "release" }
            status = "Manifest Mojang dimuat · ${fetched.size} versi ditemukan"
        } catch (t: Throwable) {
            error = t.message ?: "Gagal memuat versi. Periksa koneksi internet."
        } finally { loading = false }
    }

    LaunchedEffect(Unit) { refreshVersions() }
    val visibleVersions = versions.filter { it.type == filter }.take(80)

    Scaffold(
        containerColor = Ink,
        topBar = { TopAppBar(title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(36.dp).background(Mint, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.SportsEsports, contentDescription = null, tint = Ink)
                }
                Column {
                    Text("SALLY", fontWeight = FontWeight.Black, letterSpacing = 2.2f)
                    Text("JAVA LAUNCHER · EARLY BUILD", style = MaterialTheme.typography.labelSmall, color = SoftText)
                }
            }
        }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                    Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF243D3A), Color(0xFF1B202B), Color(0xFF282441)))).padding(22.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(color = Mint.copy(alpha = .14f), shape = CircleShape) {
                                Text("  FIRST PLAYABLE · STEP 01  ", modifier = Modifier.padding(vertical = 7.dp, horizontal = 5.dp), color = Mint, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                            Text("Your next\nworld starts here.", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, lineHeight = MaterialTheme.typography.headlineLarge.lineHeight)
                            Text("Launcher baru. Fondasi bersih. Dibangun bertahap untuk Minecraft Java di Android.", color = SoftText, style = MaterialTheme.typography.bodyMedium)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.Gamepad, contentDescription = null, tint = Mint)
                                Text("Pilih versi untuk menyiapkan file game", color = Mint, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
            item {
                SectionHeading("GAME SETUP", "Versi Minecraft")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = filter == "release", onClick = { filter = "release" }, label = { Text("Release") })
                    FilterChip(selected = filter == "snapshot", onClick = { filter = "snapshot" }, label = { Text("Snapshot") })
                    FilterChip(selected = filter == "old_beta", onClick = { filter = "old_beta" }, label = { Text("Beta lama") })
                }
            }
            if (loading && versions.isEmpty()) {
                item { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Mint) } }
            }
            if (error != null) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF3A2429))) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Tidak bisa memuat versi", fontWeight = FontWeight.Bold)
                            Text(error.orEmpty(), color = SoftText)
                            Button(onClick = { scope.launch { refreshVersions() } }) { Icon(Icons.Rounded.Refresh, null); Text("Coba lagi") }
                        }
                    }
                }
            }
            if (visibleVersions.isNotEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("VERSI DIPILIH", style = MaterialTheme.typography.labelMedium, color = SoftText)
                            Text(selected?.id ?: "Pilih versi", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text(selected?.releaseTime?.take(10) ?: "", color = SoftText, style = MaterialTheme.typography.bodySmall)
                            Button(onClick = { selected = visibleVersions.firstOrNull() }, enabled = visibleVersions.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                                Text("Gunakan versi terbaru di daftar")
                            }
                        }
                    }
                }
                items(visibleVersions, key = { it.id }) { version ->
                    VersionRow(version = version, selected = selected?.id == version.id, onClick = { selected = version })
                }
            }
            item {
                Button(
                    onClick = {
                        val version = selected ?: return@Button
                        scope.launch {
                            busy = true; error = null
                            try {
                                installed = repository.installClient(version) { msg -> status = msg }
                                status = "${version.id}: metadata dan client JAR sudah tersimpan."
                            } catch (t: Throwable) { error = t.message ?: "Instalasi gagal." }
                            finally { busy = false }
                        }
                    }, enabled = selected != null && !busy, modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Rounded.CloudDownload, contentDescription = null)
                    Spacer(Modifier.size(10.dp)); Text(if (busy) "Menyiapkan file…" else "Unduh metadata + client JAR", fontWeight = FontWeight.Bold)
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("STATUS", style = MaterialTheme.typography.labelMedium, color = Mint, fontWeight = FontWeight.Bold)
                        Text(status, color = SoftText)
                        if (installed != null) Text("Tersimpan di penyimpanan privat aplikasi: ${installed!!.clientJar.name}", style = MaterialTheme.typography.bodySmall, color = SoftText)
                        Button(onClick = {
                            val result = installed
                            if (result == null) status = "Pilih versi dan unduh file terlebih dahulu."
                            else scope.launch { status = UnavailableMinecraftEngineAdapter().launch(context, result).message }
                        }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Gamepad, null); Spacer(Modifier.size(8.dp)); Text("Mainkan") }
                        Text("Catatan: tombol Mainkan belum menjalankan game. Integrasi Java runtime, libraries/assets, renderer, dan adapter mesin game adalah milestone berikutnya.", style = MaterialTheme.typography.bodySmall, color = Color(0xFFE5C58A))
                    }
                }
            }
            item { Text("SALLY LAUNCHER · 0.1.0 · FOUNDATION BUILD", modifier = Modifier.fillMaxWidth().padding(top = 4.dp), color = SoftText, style = MaterialTheme.typography.labelSmall) }
        }
    }
}

@Composable
private fun SectionHeading(eyebrow: String, title: String) {
    Text(eyebrow, color = Mint, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 1.5f)
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
}

@Composable
private fun VersionRow(version: MinecraftVersion, selected: Boolean, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFF263A36) else Panel), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(42.dp).background(if (selected) Mint else Color(0xFF303746), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.SportsEsports, null, tint = if (selected) Ink else SoftText)
            }
            Column(Modifier.weight(1f)) {
                Text(version.id, fontWeight = FontWeight.Bold)
                Text("${version.type.uppercase()} · ${version.releaseTime.take(10)}", color = SoftText, style = MaterialTheme.typography.labelSmall)
            }
            if (selected) Text("DIPILIH", color = Mint, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}
