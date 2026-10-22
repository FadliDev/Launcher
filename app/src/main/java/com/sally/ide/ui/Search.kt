package com.sally.ide.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sally.ide.IdeViewModel
import com.sally.ide.data.Hit
import kotlinx.coroutines.delay
import java.io.File

/** Overlay pencarian proyek: nama file (fuzzy) atau isi teks. Editor di bawahnya tetap hidup (kursor tidak hilang). */
@Composable
fun ProjectSearchScreen(vm: IdeViewModel, root: File, onOpen: (File, Int?) -> Unit, onClose: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var textMode by remember { mutableStateOf(false) }
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var hits by remember { mutableStateOf<List<Hit>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    LaunchedEffect(query, textMode) {
        if (query.isBlank()) { files = emptyList(); hits = emptyList(); busy = false; return@LaunchedEffect }
        busy = true
        delay(250)
        if (textMode) { hits = vm.findText(query, true); files = emptyList() } else { files = vm.findFiles(query); hits = emptyList() }
        busy = false
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.statusBarsPadding().imePadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Ic.Back, "Kembali") }
                CompactField(query, { query = it }, if (textMode) "Cari teks di proyek" else "Cari nama file", Modifier.weight(1f).padding(end = 12.dp), focus)
            }
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !textMode, onClick = { textMode = false }, label = { Text("Nama file") },
                    leadingIcon = { Icon(Ic.File, null, Modifier.size(18.dp)) },
                )
                FilterChip(
                    selected = textMode, onClick = { textMode = true }, label = { Text("Isi teks") },
                    leadingIcon = { Icon(Ic.Code, null, Modifier.size(18.dp)) },
                )
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp)) else Spacer(Modifier.height(12.dp))

            val empty = if (textMode) hits.isEmpty() else files.isEmpty()
            if (query.isNotBlank() && !busy && empty) {
                Text("Tidak ada hasil", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LazyColumn(Modifier.fillMaxSize()) {
                items(files, key = { it.path }) { f ->
                    val v = fileVisual(f.name)
                    ListItem(
                        headlineContent = { Text(f.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text(f.relativeTo(root).path, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingContent = { Icon(v.icon, null, tint = v.tintOr(MaterialTheme.colorScheme.onSurfaceVariant)) },
                        modifier = Modifier.clickable { onOpen(f, null) },
                    )
                }
                items(hits, key = { it.file.path + ":" + it.line }) { h ->
                    ListItem(
                        headlineContent = { Text(h.text, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium) },
                        supportingContent = { Text(h.file.relativeTo(root).path + ":" + h.line, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.clickable { onOpen(h.file, h.line) },
                    )
                }
            }
        }
    }
}
