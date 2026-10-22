package com.sally.ide.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CrashScreen(trace: String, onSafe: () -> Unit, onRetry: () -> Unit) {
    val clip = LocalClipboardManager.current
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.statusBarsPadding().padding(16.dp)) {
            Text("Sally IDE crash sebelumnya", style = MaterialTheme.typography.titleMedium)
            Text("Salin teks di bawah dan kirim ke Claude.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                trace.take(6000), fontFamily = FontFamily.Monospace, fontSize = 10.sp, lineHeight = 13.sp,
                modifier = Modifier.weight(1f).padding(vertical = 8.dp).verticalScroll(rememberScrollState()),
            )
            Row {
                OutlinedButton(onClick = { clip.setText(AnnotatedString(trace)) }) { Text("Salin") }
                OutlinedButton(onClick = onRetry, modifier = Modifier.padding(start = 8.dp)) { Text("Coba normal") }
                Button(onClick = onSafe, modifier = Modifier.padding(start = 8.dp)) { Text("Mode aman") }
            }
        }
    }
}
