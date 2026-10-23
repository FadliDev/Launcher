package com.sally.modforge.ui.account

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sally.modforge.github.GitHubAccount

@Composable
fun AccountScreen(vm: AccountViewModel = viewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("GitHub", style = MaterialTheme.typography.titleLarge)

        val session = ui.session
        if (session == null) {
            LoggedOutSection(
                ui = ui,
                vm = vm,
                onCopy = { copyText(context, it) },
                onOpen = { openUrl(context, it) },
            )
        } else {
            LoggedInSection(
                login = session.login,
                ui = ui,
                vm = vm,
                onOpen = { openUrl(context, it) },
            )
        }

        ui.message?.let { msg ->
            Text(
                msg,
                color = if (ui.error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LoggedOutSection(
    ui: AccountUiState,
    vm: AccountViewModel,
    onCopy: (String) -> Unit,
    onOpen: (String) -> Unit,
) {
    val device = ui.device
    if (device != null) {
        SectionCard {
            Text("Masuk dengan GitHub", style = MaterialTheme.typography.titleMedium)
            Text("Buka github.com/login/device dan masukkan kode:")
            Text(
                device.userCode,
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = FontFamily.Monospace,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onCopy(device.userCode) }) { Text("Salin") }
                Button(onClick = { onOpen(device.verificationUri) }) { Text("Buka GitHub") }
            }
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Text("  Menunggu konfirmasi...")
            }
            TextButton(onClick = vm::cancelDeviceFlow) { Text("Batal") }
        }
        return
    }

    SectionCard {
        Text("Masuk", style = MaterialTheme.typography.titleMedium)
        Text(
            "Repo dibuat otomatis per project, build berjalan di GitHub Actions, token disimpan aman di HP.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = ui.clientId,
            onValueChange = vm::onClientIdChange,
            label = { Text("OAuth Client ID") },
            supportingText = { Text("Identifier publik dari OAuth App. Kosongkan jika memakai token.") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = vm::startDeviceFlow,
            enabled = !ui.busy && ui.clientId.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Masuk dengan GitHub") }
    }

    SectionCard {
        Text("Atau pakai Personal Access Token", style = MaterialTheme.typography.titleMedium)
        Text(
            "Token klasik dengan scope repo dan workflow. Hanya disimpan terenkripsi di perangkat ini.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = ui.pat,
            onValueChange = vm::onPatChange,
            label = { Text("Token") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = vm::loginWithPat,
            enabled = !ui.busy && ui.pat.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Masuk dengan token") }
    }
}

@Composable
private fun LoggedInSection(
    login: String,
    ui: AccountUiState,
    vm: AccountViewModel,
    onOpen: (String) -> Unit,
) {
    SectionCard {
        Text("@$login", style = MaterialTheme.typography.titleMedium)
        Text("Terhubung ke GitHub", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    SectionCard {
        Text("Tes buat repo", style = MaterialTheme.typography.titleMedium)
        Text(
            "Membuat repo privat baru dengan README sebagai commit awal.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = ui.repoName,
            onValueChange = vm::onRepoNameChange,
            label = { Text("Nama repo") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = vm::createTestRepo,
            enabled = !ui.busy && ui.repoName.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Buat repo") }
        ui.createdRepoUrl?.let { url ->
            TextButton(onClick = { onOpen(url) }) { Text("Buka di GitHub") }
        }
    }

    SectionCard {
        Text("Akun", style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = vm::logout, modifier = Modifier.fillMaxWidth()) {
            Text("Keluar dari ModForge")
        }
        TextButton(onClick = { onOpen(GitHubAccount.REVOKE_URL) }) {
            Text("Cabut izin di GitHub")
        }
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

private fun copyText(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Kode GitHub", text))
}

private fun openUrl(context: Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
