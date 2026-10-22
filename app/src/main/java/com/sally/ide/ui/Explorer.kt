package com.sally.ide.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sally.ide.TreeRow
import com.sally.ide.editor.Lang
import java.io.File

/** Explorer sederhana (Fase 1). Versi lengkap (rename/hapus/drag-drop/icon per tipe) = Fase 2. */
@Composable
fun Explorer(
    rows: List<TreeRow>,
    selectedDir: File,
    activeFile: File?,
    onRow: (TreeRow) -> Unit,
    onNewFile: () -> Unit,
    onRefresh: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Workspace", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        IconButton(onClick = onNewFile) { Icon(Icons.Filled.Add, "File baru") }
        IconButton(onClick = onRefresh) { Icon(Icons.Filled.Refresh, "Muat ulang") }
    }
    Text(
        "Folder tujuan file baru: ${selectedDir.name}", style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp, bottom = 6.dp),
    )
    LazyColumn {
        items(rows, key = { it.file.path }) { r ->
            val selected = r.file == activeFile
            Row(
                Modifier.fillMaxWidth().height(48.dp).clickable { onRow(r) }
                    .padding(start = (12 + r.depth * 16).dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (r.file.isDirectory) {
                    Icon(Icons.Filled.ExpandMore, null, Modifier.size(18.dp).rotate(if (r.expanded) 0f else -90f))
                } else Spacer(Modifier.width(18.dp))
                Icon(
                    when {
                        r.file.isDirectory && r.expanded -> Icons.Outlined.FolderOpen
                        r.file.isDirectory -> Icons.Outlined.Folder
                        Lang.forFile(r.file.name) != Lang.PLAIN && Lang.forFile(r.file.name) != Lang.MARKDOWN -> Icons.Outlined.Code
                        else -> Icons.Outlined.Description
                    },
                    null, Modifier.padding(horizontal = 8.dp).size(20.dp),
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    r.file.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
