package com.sally.ide.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sally.ide.APP_VERSION
import com.sally.ide.CrashLog
import com.sally.ide.data.Prefs
import com.sally.ide.data.ThemeMode
import com.sally.ide.editor.Snippet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnippetSheet(snippets: List<Snippet>, langLabel: String, onPick: (Snippet) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text("Snippet — $langLabel", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        if (snippets.isEmpty()) {
            Text("Belum ada snippet untuk bahasa ini.", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else LazyColumn {
            items(snippets, key = { it.name }) { s ->
                ListItem(
                    headlineContent = { Text(s.name) },
                    supportingContent = { Text(s.description) },
                    trailingContent = { Text(s.prefix, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable { onPick(s) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    prefs: Prefs,
    onTheme: (ThemeMode) -> Unit, onFont: (Float) -> Unit,
    onWrap: (Boolean) -> Unit, onLines: (Boolean) -> Unit, onTab: (Int) -> Unit, onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("Setelan", style = MaterialTheme.typography.titleMedium)
            Text("Tema", Modifier.padding(top = 12.dp), style = MaterialTheme.typography.labelLarge)
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ThemeMode.entries.forEach { m ->
                    FilterChip(selected = prefs.themeMode == m, onClick = { onTheme(m) }, label = { Text(m.label) })
                }
            }
            Text("Ukuran font editor: ${prefs.fontSize.toInt()} sp", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.labelLarge)
            Slider(value = prefs.fontSize, onValueChange = { onFont(it.toInt().toFloat()) }, valueRange = 10f..24f, steps = 13)
            SwitchRow("Word wrap", prefs.wordWrap, onWrap)
            SwitchRow("Nomor baris", prefs.lineNumbers, onLines)
            Text("Lebar tab: ${prefs.tabSize} spasi", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.labelLarge)
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(2, 4, 8).forEach { n -> FilterChip(selected = prefs.tabSize == n, onClick = { onTab(n) }, label = { Text("$n") }) }
            }
            val ctx = LocalContext.current
            var safe by remember { mutableStateOf(CrashLog.isSafe(ctx)) }
            SwitchRow("Mode aman: tanpa highlight (mulai ulang app)", safe) { safe = it; CrashLog.setSafe(ctx, it) }
            Text(
                "Sally IDE v$APP_VERSION", Modifier.padding(top = 12.dp, bottom = 24.dp),
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SwitchRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = onChange)
    }
}

@Composable
fun TextInputDialog(
    title: String, hint: String, initial: String = "", numeric: Boolean = false,
    onConfirm: (String) -> Unit, onDismiss: () -> Unit,
) {
    var v by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text(title) },
        text = {
            OutlinedTextField(
                value = v, onValueChange = { v = it }, singleLine = true, label = { Text(hint) },
                keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text),
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(v) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
fun ConfirmDialog(title: String, text: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
fun UnsavedDialog(name: String, onSave: () -> Unit, onDiscard: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Simpan perubahan?") },
        text = { Text("$name punya perubahan yang belum disimpan.") },
        confirmButton = { TextButton(onClick = onSave) { Text("Simpan") } },
        dismissButton = {
            Row {
                TextButton(onClick = onDiscard) { Text("Buang") }
                TextButton(onClick = onCancel) { Text("Batal") }
            }
        },
    )
}
