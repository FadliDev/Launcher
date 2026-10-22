package com.sally.ide.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sally.ide.EditorTab
import com.sally.ide.editor.Kind
import com.sally.ide.editor.Suggestion

@Composable
fun TabBar(tabs: List<EditorTab>, active: Int, onSelect: (Int) -> Unit, onClose: (Int) -> Unit) {
    if (tabs.isEmpty()) return
    ScrollableTabRow(
        selectedTabIndex = active.coerceIn(0, tabs.lastIndex),
        edgePadding = 0.dp,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        tabs.forEachIndexed { i, t ->
            Tab(selected = i == active, onClick = { onSelect(i) }) {
                Row(Modifier.padding(start = 12.dp, end = 2.dp).height(44.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(t.file.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.widthIn(max = 160.dp))
                    if (t.dirty) Box(Modifier.padding(start = 6.dp).size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    IconButton(onClick = { onClose(i) }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Tutup ${t.file.name}", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
    HorizontalDivider()
}

@Composable
fun SuggestionBar(items: List<Suggestion>, onPick: (Suggestion) -> Unit) {
    if (items.isEmpty()) return
    LazyRow(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
    ) {
        items(items, key = { it.kind.name + it.label }) { s ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (s.kind == Kind.SNIPPET) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.clickable { onPick(s) },
            ) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(s.kind.tag, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(s.label, Modifier.padding(start = 6.dp), fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                }
            }
        }
    }
}

private class Key(val label: String, val onTap: () -> Unit)

@Composable
fun ExtraKeyRow(h: EditorHandle, tabSize: Int) {
    val keys = remember(h, tabSize) {
        listOf(
            Key("Tab") { h.insert(" ".repeat(tabSize)) },
            Key("←") { h.left() }, Key("→") { h.right() }, Key("↑") { h.up() }, Key("↓") { h.down() },
            Key("{") { h.insert("{") }, Key("}") { h.insert("}") }, Key("(") { h.insert("(") }, Key(")") { h.insert(")") },
            Key("[") { h.insert("[") }, Key("]") { h.insert("]") }, Key("<") { h.insert("<") }, Key(">") { h.insert(">") },
            Key(";") { h.insert(";") }, Key("\"") { h.insert("\"") }, Key("'") { h.insert("'") }, Key("=") { h.insert("=") },
            Key("/") { h.insert("/") }, Key("\\") { h.insert("\\") }, Key("_") { h.insert("_") }, Key("+") { h.insert("+") },
            Key("Home") { h.home() }, Key("End") { h.end() },
        )
    }
    LazyRow(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
        items(keys) { k ->
            Box(
                Modifier.height(48.dp).widthIn(min = 44.dp).clickable { k.onTap() }.padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) { Text(k.label, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleSmall) }
        }
    }
}

@Composable
fun StatusBar(langLabel: String, line: Int, col: Int, tabSize: Int) {
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val st = MaterialTheme.typography.labelSmall
        Text(langLabel, style = st)
        Text("Ln $line, Col $col", style = st)
        Text("Spasi: $tabSize", style = st)
        Text("UTF-8", style = st)
        Text("LF", style = st)
    }
}

@Composable
private fun CompactField(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier) {
    Box(
        modifier.height(40.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) Text(hint, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        BasicTextField(
            value = value, onValueChange = onChange, singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions.Default,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun FindBar(h: EditorHandle, onClose: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var repl by remember { mutableStateOf("") }
    var ignoreCase by remember { mutableStateOf(true) }
    var regex by remember { mutableStateOf(false) }
    var invalid by remember { mutableStateOf(false) }
    fun run(q: String = query, ic: Boolean = ignoreCase, rx: Boolean = regex) { invalid = !h.search(q, ic, rx) }

    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            CompactField(query, { query = it; run(it) }, if (invalid) "Regex tidak valid" else "Cari", Modifier.weight(1f))
            IconButton(onClick = { h.prev() }) { Icon(Icons.Filled.KeyboardArrowUp, "Sebelumnya") }
            IconButton(onClick = { h.next() }) { Icon(Icons.Filled.KeyboardArrowDown, "Berikutnya") }
            IconButton(onClick = { h.stopSearch(); onClose() }) { Icon(Icons.Filled.Close, "Tutup pencarian") }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            CompactField(repl, { repl = it }, "Ganti dengan", Modifier.weight(1f))
            TextButton(onClick = { h.replaceCurrent(repl) }) { Text("Ganti") }
            TextButton(onClick = { h.replaceAll(repl) }) { Text("Semua") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !ignoreCase, onClick = { ignoreCase = !ignoreCase; run(ic = ignoreCase) }, label = { Text("Aa") })
            FilterChip(selected = regex, onClick = { regex = !regex; run(rx = regex) }, label = { Text(".*") })
        }
    }
    HorizontalDivider()
}
