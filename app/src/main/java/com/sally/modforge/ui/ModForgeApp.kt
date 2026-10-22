package com.sally.modforge.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sally.modforge.data.AppPage
import com.sally.modforge.data.MainTab
import com.sally.modforge.data.ModForgeController
import com.sally.modforge.data.ModProject
import com.sally.modforge.data.ProjectSection
import com.sally.modforge.data.VersionMatrix
import com.sally.modforge.data.sanitizeModId
import java.io.File

@Composable
fun ModForgeApp(controller: ModForgeController) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(controller.message) {
        if (controller.message.isNotBlank()) {
            snackbarHostState.showSnackbar(controller.message)
            controller.dismissMessage()
        }
    }
    when (controller.page) {
        AppPage.Home -> HomeShell(controller, snackbarHostState)
        AppPage.NewProject -> NewProjectScreen(controller, snackbarHostState)
        AppPage.Project -> ProjectScreen(controller, snackbarHostState)
        AppPage.Editor -> EditorScreen(controller, snackbarHostState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeShell(controller: ModForgeController, snackbar: SnackbarHostState) {
    var query by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    if (searching && controller.tab == MainTab.Projects) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("Cari project") }
                        )
                    } else {
                        Column {
                            Text(
                                when (controller.tab) {
                                    MainTab.Projects -> "ModForge"
                                    MainTab.GitHub -> "GitHub"
                                    MainTab.Settings -> "Pengaturan"
                                },
                                fontWeight = FontWeight.SemiBold
                            )
                            if (controller.tab == MainTab.Projects) {
                                Text("Bikin mod Fabric dari HP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                },
                actions = {
                    if (controller.tab == MainTab.Projects) {
                        IconButton(onClick = { searching = !searching; if (!searching) query = "" }) {
                            Icon(Icons.Outlined.Search, contentDescription = "Cari project")
                        }
                    }
                    if (controller.tab == MainTab.GitHub && controller.tokenConnected) {
                        IconButton(onClick = { controller.refreshGitHub() }) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Muat ulang akun")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = controller.tab == MainTab.Projects,
                    onClick = { controller.navigateHome(MainTab.Projects) },
                    icon = { Icon(Icons.Outlined.Folder, contentDescription = null) },
                    label = { Text("Projects") }
                )
                NavigationBarItem(
                    selected = controller.tab == MainTab.GitHub,
                    onClick = { controller.navigateHome(MainTab.GitHub) },
                    icon = { Icon(Icons.Outlined.AccountCircle, contentDescription = null) },
                    label = { Text("GitHub") }
                )
                NavigationBarItem(
                    selected = controller.tab == MainTab.Settings,
                    onClick = { controller.navigateHome(MainTab.Settings) },
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                    label = { Text("Pengaturan") }
                )
            }
        },
        floatingActionButton = {
            if (controller.tab == MainTab.Projects) {
                FloatingActionButton(onClick = controller::openNewProject, containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                    Icon(Icons.Outlined.Add, contentDescription = "Buat project baru")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (controller.tab) {
                MainTab.Projects -> ProjectsHome(controller, query)
                MainTab.GitHub -> GitHubHome(controller)
                MainTab.Settings -> SettingsHome(controller)
            }
        }
    }
}

@Composable
private fun ProjectsHome(controller: ModForgeController, query: String) {
    val filtered = controller.projects.filter { it.name.contains(query, true) || it.modId.contains(query, true) }
    if (filtered.isEmpty()) {
        Column(
            Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Outlined.Code, contentDescription = null, modifier = Modifier.size(54.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text(if (query.isBlank()) "Belum ada project" else "Project tidak ditemukan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                if (query.isBlank()) "Mulai dari template Fabric. File dan konfigurasi akan dibuat langsung di perangkat." else "Coba kata kunci lain.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (query.isBlank()) {
                Spacer(Modifier.height(18.dp))
                Button(onClick = controller::openNewProject) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("Buat mod pertama") }
            }
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Project kamu", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("${controller.projects.size} project tersimpan lokal", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        items(filtered, key = { it.id }) { project -> ProjectCard(controller, project) }
        item {
            Spacer(Modifier.height(8.dp))
            Text("Build dikompilasi di GitHub Actions; HP tidak perlu JDK/Gradle.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProjectCard(controller: ModForgeController, project: ModProject) {
    var showDelete by remember { mutableStateOf(false) }
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f).clickable { controller.openProject(project) }) {
                    Text(project.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(project.modId, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { showDelete = true }) { Icon(Icons.Outlined.DeleteOutline, contentDescription = "Hapus project") }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                TinyChip("MC ${project.minecraftVersion}")
                TinyChip("Java ${project.javaVersion}")
                TinyChip("Gradle ${project.gradleVersion}")
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { controller.openProject(project) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Folder, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Buka")
                }
                Button(onClick = { controller.openProject(project); controller.startBuild() }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Build")
                }
            }
        }
    }
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Hapus project?") },
            text = { Text("${project.name} dan semua file lokalnya akan dihapus. Repository GitHub tidak ikut dihapus.") },
            confirmButton = { TextButton(onClick = { showDelete = false; controller.deleteProject(project) }) { Text("Hapus", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Batal") } }
        )
    }
}

@Composable
private fun TinyChip(label: String) {
    AssistChip(onClick = {}, label = { Text(label, maxLines = 1) }, modifier = Modifier.height(32.dp))
}

@Composable
private fun GitHubHome(controller: ModForgeController) {
    var pat by rememberSaveable { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (controller.tokenConnected) "GitHub terhubung" else "Hubungkan GitHub", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            controller.githubUser?.let { "@${it.login}" } ?: "Repo project dan cloud build",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (controller.tokenConnected) Icon(Icons.Outlined.CheckCircle, contentDescription = "Terhubung", tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(12.dp))
                Text("ModForge mengirim source project ke repository GitHub milikmu. Token dienkripsi dengan Android Keystore dan tidak dimasukkan ke source project.", style = MaterialTheme.typography.bodyMedium)
                if (!controller.tokenConnected) {
                    Spacer(Modifier.height(16.dp))
                    Text("Masuk dengan Device Flow", fontWeight = FontWeight.SemiBold)
                    Text("Atur OAuth Client ID di Pengaturan terlebih dahulu.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = controller::startDeviceFlow, enabled = !controller.busy && controller.oauthClientId.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                        if (controller.busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.Outlined.OpenInNew, null)
                        Spacer(Modifier.width(8.dp)); Text("Masuk dengan GitHub")
                    }
                    if (controller.deviceCode.isNotBlank()) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp)) {
                                Text("Kode perangkat", style = MaterialTheme.typography.labelMedium)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(controller.deviceCode, style = MaterialTheme.typography.headlineSmall, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                                    val clipboard = LocalClipboardManager.current
                                    IconButton(onClick = { clipboard.setText(AnnotatedString(controller.deviceCode)); controller.showMessage("Kode disalin.") }) { Icon(Icons.Outlined.ContentCopy, contentDescription = "Salin kode") }
                                }
                                Text(controller.deviceStatus, style = MaterialTheme.typography.bodySmall)
                                TextButton(onClick = { controller.openUrl(controller.deviceVerificationUri) }) { Text("Buka GitHub") }
                            }
                        }
                    }
                    Divider()
                    Text("Atau hubungkan dengan Personal Access Token", fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = pat,
                        onValueChange = { pat = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("GitHub token") },
                        placeholder = { Text("github_pat_… atau ghp_…") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, capitalization = KeyboardCapitalization.None)
                    )
                    Text("Token classic membutuhkan scope repo dan workflow. Gunakan hanya di perangkat pribadi.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { controller.connectWithPersonalToken(pat); pat = "" }, enabled = !controller.busy && pat.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                        Text(if (controller.busy) "Memeriksa token…" else "Hubungkan token")
                    }
                } else {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = controller::refreshGitHub, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Refresh, null); Spacer(Modifier.width(8.dp)); Text("Muat ulang repository") }
                    TextButton(onClick = controller::disconnectGitHub, modifier = Modifier.align(Alignment.End)) { Text("Keluar / hapus token lokal") }
                }
            }
        }
        if (controller.tokenConnected) {
            Text("Repository terbaru", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (controller.githubRepos.isEmpty()) {
                Text("Belum ada repository atau daftar belum dimuat.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            controller.githubRepos.take(30).forEach { repo ->
                OutlinedCard(Modifier.fillMaxWidth().clickable { controller.openUrl(repo.htmlUrl) }) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (repo.isPrivate) Icons.Outlined.Lock else Icons.Outlined.Cloud, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(repo.fullName, fontWeight = FontWeight.SemiBold)
                            Text(if (repo.isPrivate) "Private" else "Public", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Outlined.OpenInNew, contentDescription = "Buka repository")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsHome(controller: ModForgeController) {
    var clientId by rememberSaveable(controller.oauthClientId) { mutableStateOf(controller.oauthClientId) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Tampilan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (controller.isDark) "Tema gelap" else "Tema terang")
                        Text("Gunakan aksen hijau ModForge", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = controller.isDark, onCheckedChange = { controller.toggleDarkTheme() })
                }
            }
        }
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("OAuth Device Flow", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Isi Client ID dari OAuth App milik penerbit aplikasi. OAuth App harus mengaktifkan Device Flow. Client ID bukan secret; token hasil login tetap disimpan terenkripsi di perangkat.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = clientId,
                    onValueChange = { clientId = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("OAuth Client ID") },
                    placeholder = { Text("Iv1.…") }
                )
                Button(onClick = { controller.saveOAuthClientId(clientId) }, modifier = Modifier.fillMaxWidth()) { Text("Simpan Client ID") }
            }
        }
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tentang ModForge", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Versi 0.1.0 · Fondasi MVP", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Dibuat oleh sally. ModForge bukan produk resmi Mojang atau Microsoft.")
                Divider()
                Text("MVP sekarang: project Fabric lokal, generator template dasar, editor teks sederhana, GitHub sync dan remote build. Editor cerdas, visual scripting, model 3D, dan asset painter menyusul sesuai roadmap.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewProjectScreen(controller: ModForgeController, snackbar: SnackbarHostState) {
    var name by rememberSaveable { mutableStateOf("My First Mod") }
    var modId by rememberSaveable { mutableStateOf("myfirstmod") }
    var packageName by rememberSaveable { mutableStateOf("com.sally.myfirstmod") }
    var minecraft by rememberSaveable { mutableStateOf("1.21.1") }
    var template by rememberSaveable { mutableStateOf("Empty") }
    var visibility by rememberSaveable { mutableStateOf("private") }
    var modIdEdited by rememberSaveable { mutableStateOf(false) }
    var packageEdited by rememberSaveable { mutableStateOf(false) }
    var versionMenu by remember { mutableStateOf(false) }
    val profile = VersionMatrix.profile(minecraft)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Column { Text("Project baru", fontWeight = FontWeight.SemiBold); Text("Langkah 1–3 · dasar dan template", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                navigationIcon = { IconButton(onClick = { controller.navigateHome() }) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Kembali") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                StepDot("1", active = true); Divider(Modifier.width(24.dp)); StepDot("2", active = true); Divider(Modifier.width(24.dp)); StepDot("3", active = true)
                Spacer(Modifier.weight(1f)); Text("Fabric", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
            Text("Dasar project", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = name,
                onValueChange = { new ->
                    name = new
                    if (!modIdEdited) modId = sanitizeModId(new.replace(' ', '_'))
                    if (!packageEdited) packageName = "com.sally.${sanitizeModId(new.replace(' ', '_'))}"
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nama mod") },
                singleLine = true
            )
            OutlinedTextField(
                value = modId,
                onValueChange = { modId = it.lowercase(); modIdEdited = true },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Mod ID") },
                supportingText = { Text("Huruf kecil, angka, _ atau -") },
                singleLine = true
            )
            OutlinedTextField(
                value = packageName,
                onValueChange = { packageName = it; packageEdited = true },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Package Java") },
                supportingText = { Text("Contoh: com.sally.myfirstmod") },
                singleLine = true
            )
            Divider()
            Text("Versi Minecraft", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Box {
                OutlinedButton(onClick = { versionMenu = true }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                        Text("Minecraft $minecraft", fontWeight = FontWeight.SemiBold)
                        Text("Java ${profile.java} · Gradle ${profile.gradle}${if (profile.experimental) " · Eksperimental" else ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("⌄")
                }
                DropdownMenu(expanded = versionMenu, onDismissRequest = { versionMenu = false }) {
                    VersionMatrix.profiles.forEach { item ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text("Minecraft ${item.minecraft}")
                                    Text("Java ${item.java} · Gradle ${item.gradle}${if (item.experimental) " · Eksperimental" else ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            onClick = {
                                minecraft = item.minecraft
                                if (item.minecraft == "26.3") template = "Empty"
                                versionMenu = false
                            }
                        )
                    }
                }
            }
            if (profile.experimental) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2D7)), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Outlined.Info, contentDescription = null, tint = Color(0xFF8C5B00))
                        Spacer(Modifier.width(8.dp))
                        Text(
                    if (minecraft == "26.3") "Minecraft 26.3 masih eksperimental. Untuk mencegah template API yang belum diverifikasi, pilihan saat ini dibatasi ke Empty." else "Kombinasi versi ini masih eksperimental dalam build awal ModForge. Build aktual bergantung pada kompatibilitas Fabric Loom dan dependency versi tersebut.",
                    color = Color(0xFF5B3B00), style = MaterialTheme.typography.bodySmall
                )
                    }
                }
            }
            Text("Template", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text("Pilih struktur awal. Kode dapat diedit setelah project dibuat.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Empty", "Item", "Block", "Item + Block", "Event").chunked(2).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowItems.forEach { item ->
                            FilterChip(
                                selected = template == item,
                                onClick = { template = item },
                                enabled = minecraft != "26.3" || item == "Empty",
                                label = { Text(item) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            Text("Repository", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = visibility == "private", onClick = { visibility = "private" }, label = { Icon(Icons.Outlined.Lock, null, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(4.dp)); Text("Private") }, modifier = Modifier.weight(1f))
                FilterChip(selected = visibility == "public", onClick = { visibility = "public" }, label = { Icon(Icons.Outlined.Cloud, null, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(4.dp)); Text("Public") }, modifier = Modifier.weight(1f))
            }
            Text("Project dibuat di perangkat terlebih dahulu. Repository GitHub dibuat saat kamu menekan Build setelah login.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { controller.createProject(name, modId, packageName, minecraft, template, visibility) },
                enabled = !controller.busy,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (controller.busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.Outlined.Add, null)
                Spacer(Modifier.width(8.dp)); Text(if (controller.busy) "Membuat project…" else "Buat Project")
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun StepDot(number: String, active: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.size(28.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(number, color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectScreen(controller: ModForgeController, snackbar: SnackbarHostState) {
    val project = controller.selectedProject
    if (project == null) {
        LaunchedEffect(Unit) { controller.navigateHome() }
        return
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(project.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                        Text("MC ${project.minecraftVersion} · Java ${project.javaVersion}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = { IconButton(onClick = { controller.navigateHome() }) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Kembali ke projects") } },
                actions = {
                    IconButton(onClick = { controller.projectSection = ProjectSection.Build; controller.startBuild() }) {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = "Build mod")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(selectedTabIndex = controller.projectSection.ordinal, edgePadding = 8.dp, containerColor = MaterialTheme.colorScheme.background) {
                ProjectSection.entries.forEach { section ->
                    Tab(
                        selected = controller.projectSection == section,
                        onClick = { controller.projectSection = section },
                        text = { Text(section.label) }
                    )
                }
            }
            when (controller.projectSection) {
                ProjectSection.Overview -> OverviewSection(controller, project)
                ProjectSection.Code -> FileListSection(controller, project, showAll = true)
                ProjectSection.Visual -> FeatureComingSection(
                    icon = "◇", title = "Visual scripting", milestone = "Milestone M5",
                    body = "Node graph, koneksi port bertipe, validasi dan generator Java belum diaktifkan pada build fondasi ini."
                )
                ProjectSection.Models -> FeatureComingSection(
                    icon = "◈", title = "Editor model 3D", milestone = "Milestone M4",
                    body = "Viewport 3D, cuboid, texture painter dan ekspor model akan ditambahkan setelah fondasi project/build stabil."
                )
                ProjectSection.Assets -> FileListSection(controller, project, showAll = false)
                ProjectSection.Build -> BuildSection(controller, project)
            }
        }
    }
}

@Composable
private fun OverviewSection(controller: ModForgeController, project: ModProject) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Instance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                InfoRow("Minecraft", project.minecraftVersion)
                InfoRow("Java", project.javaVersion.toString())
                InfoRow("Gradle", project.gradleVersion)
                InfoRow("Loader", "Fabric")
                InfoRow("Template", project.template)
            }
        }
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Metadata", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                InfoRow("Mod ID", project.modId)
                InfoRow("Package", project.packageName)
                InfoRow("Author", project.author)
                InfoRow("Visibilitas repo", project.visibility.replaceFirstChar(Char::uppercase))
            }
        }
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Repository", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (project.repoOwner.isBlank()) {
                    Text("Belum terhubung ke GitHub. Tekan Build untuk membuat repo dan memulai build cloud.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { controller.projectSection = ProjectSection.Build; controller.startBuild() }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Build di GitHub Actions") }
                } else {
                    InfoRow("Repo", "${project.repoOwner}/${project.repoName}")
                    InfoRow("Branch", project.branch)
                    InfoRow("Status", if (project.lastSyncedSha.isBlank()) "Belum sync" else "Tersinkron")
                    OutlinedButton(onClick = { controller.openUrl("https://github.com/${project.repoOwner}/${project.repoName}") }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.OpenInNew, null); Spacer(Modifier.width(8.dp)); Text("Buka repository") }
                }
            }
        }
        Text("Tip: buka Code untuk mengedit file sumber. File tersimpan otomatis di direktori privat aplikasi.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        Text(label, modifier = Modifier.weight(0.9f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, modifier = Modifier.weight(1.3f), fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun FileListSection(controller: ModForgeController, project: ModProject, showAll: Boolean) {
    val files = controller.store.listSourceFiles(project).filter { path ->
        if (showAll) path != "README.md" && path != ".gitignore" && !path.startsWith(".github/") else path.startsWith("src/main/resources/")
    }
    if (files.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(if (showAll) Icons.Outlined.Code else Icons.Outlined.Description, contentDescription = null, modifier = Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(10.dp))
            Text(if (showAll) "Tidak ada file kode" else "Belum ada asset tambahan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(if (showAll) "Template ini belum mempunyai file yang bisa diedit." else "Resource seperti JSON, lang dan texture akan tampil di sini.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(if (showAll) "File project" else "Resource & assets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
        files.forEach { path ->
            OutlinedCard(Modifier.fillMaxWidth().clickable { controller.openEditor(path) }) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(File(path).name, fontWeight = FontWeight.Medium)
                        Text(path, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Text("›", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun FeatureComingSection(icon: String, title: String, milestone: String, body: String) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(icon, color = MaterialTheme.colorScheme.primary, fontSize = 44.sp)
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        TinyChip(milestone)
        Spacer(Modifier.height(12.dp))
        Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BuildSection(controller: ModForgeController, project: ModProject) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Remote build", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text("${project.modId} · Java ${project.javaVersion}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text(controller.buildStatus, style = MaterialTheme.typography.bodyMedium)
                if (controller.busy) {
                    androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth())
                    OutlinedButton(onClick = controller::cancelBuild, modifier = Modifier.fillMaxWidth()) { Text("Batalkan pemantauan") }
                } else {
                    Button(onClick = controller::startBuild, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                        Icon(Icons.Outlined.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text(if (project.repoOwner.isBlank()) "Buat repo & Build" else "Build ulang")
                    }
                }
                Text("Build memakai GitHub Actions. Source file akan disinkronkan ke repo ${if (project.repoOwner.isBlank()) "baru" else "${project.repoOwner}/${project.repoName}"}.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (controller.lastBuildUrl.isNotBlank()) {
                    OutlinedButton(onClick = { controller.openUrl(controller.lastBuildUrl) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.OpenInNew, null); Spacer(Modifier.width(8.dp)); Text("Buka log di GitHub") }
                }
                if (controller.lastArtifactPath.isNotBlank()) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(File(controller.lastArtifactPath).name, fontWeight = FontWeight.SemiBold)
                                Text("JAR tersimpan di data aplikasi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = controller::shareArtifact) { Text("Bagikan") }
                        }
                    }
                }
            }
        }
        if (controller.buildLog.isNotEmpty()) {
            Text("Aktivitas build", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    controller.buildLog.takeLast(24).forEach { line ->
                        Text("› $line", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorScreen(controller: ModForgeController, snackbar: SnackbarHostState) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(File(controller.editingPath).name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                        Text(controller.editingPath, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = { IconButton(onClick = { controller.saveEditor(); controller.page = AppPage.Project; controller.projectSection = ProjectSection.Code }) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Kembali ke file") } },
                actions = { TextButton(onClick = controller::saveEditor) { Text("Simpan") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp, vertical = 8.dp)) {
            Card(Modifier.fillMaxSize(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                OutlinedTextField(
                    value = controller.editorContent,
                    onValueChange = { controller.editorContent = it },
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 20.sp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                    placeholder = { Text("Isi file…", fontFamily = FontFamily.Monospace) }
                )
            }
            Spacer(Modifier.height(6.dp))
            Text("Editor dasar · syntax highlighting, autocomplete dan quick-fix masuk milestone M3.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
