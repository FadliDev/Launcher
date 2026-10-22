package com.sally.modforge.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.sally.modforge.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitHubScreen(vm: AppVm) {
    val login by vm.login.collectAsState()
    val dc by vm.deviceCode.collectAsState()
    var cid by remember { mutableStateOf(vm.secrets.clientId) }
    val ctx = LocalContext.current
    Scaffold(topBar = { TopAppBar(title = { Text("GitHub") }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (login != null) {
                Text("Terhubung sebagai @$login")
                OutlinedButton({ vm.disconnect() }) { Text("Disconnect") }
            } else {
                if (BuildConfig.GITHUB_CLIENT_ID.isBlank()) {
                    OutlinedTextField(
                        cid, { cid = it; vm.secrets.clientId = it },
                        label = { Text("OAuth App Client ID (Device Flow aktif)") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                }
                Button({ vm.connect() }) { Text("Connect GitHub") }
                dc?.let {
                    Text("Buka ${it.verificationUri} lalu masukkan kode:")
                    Text(it.userCode, style = MaterialTheme.typography.headlineMedium, fontFamily = FontFamily.Monospace)
                    Button({ ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.verificationUri))) }) { Text("Buka GitHub") }
                }
            }
        }
    }
}
