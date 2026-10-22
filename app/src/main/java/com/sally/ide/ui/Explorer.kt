package com.sally.ide.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sally.ide.TreeRow
import java.io.File

class ExplorerActions(
    val onOpen: (TreeRow) -> Unit,
    val onNewFile: (File) -> Unit,
    val onNewFolder: (File) -> Unit,
    val onRename: (File) -> Unit,
    val onDuplicate: (File) -> Unit,
    val onDelete: (File) -> Unit,
    val onCopyPath: (File) -> Unit,
    val onRefresh: () -> Unit,
    val onCollapse: () -> Unit,
)

/** Explorer proyek: tap = buka/lipat, tekan lama = menu aksi. */
@Composable
fun Explorer(projectName: String, rows: List<TreeRow>, activeFile: File?, targetDir: File, a: ExplorerActions) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(projectName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            IconButton(onClick = { a.onNewFile(targetDir) }) { Icon(Ic.NewFile, "File baru") }
            IconButton(onClick = { a.onNewFolder(targetDir) }) { Icon(Ic.NewFolder, "Folder baru") }
            IconButton(onClick = a.onCollapse) { Icon(Ic.CollapseAll, "Ciutkan semua") }
            IconButton(onClick = a.onRefresh) { Icon(Ic.Refresh, "Muat ulang") }
        }
        HorizontalDivider()
        if (rows.isEmpty()) {
            Text("Proyek kosong. Buat file dengan tombol di atas.", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(rows, key = { it.file.path }) { r -> ExplorerRow(r, r.file == activeFile, a) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExplorerRow(r: TreeRow, active: Boolean, a: ExplorerActions) {
    var menu by remember { mutableStateOf(false) }
    val f = r.file
    val dim = r.isDir && (f.name == "build" || f.name == ".gradle" || f.name == ".git")
    val base = MaterialTheme.colorScheme.onSurfaceVariant
    val visual = if (r.isDir) null else fileVisual(f.name)
    val iconTint = when {
        dim -> base.copy(alpha = 0.5f)
        r.isDir -> MaterialTheme.colorScheme.primary
        else -> visual!!.tintOr(base)
    }
    Box {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp).height(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(if (active) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                .combinedClickable(onClick = { a.onOpen(r) }, onLongClick = { menu = true })
                .padding(start = (6 + r.depth * 14).dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (r.isDir) Icon(Ic.ExpandMore, null, Modifier.size(18.dp).rotate(if (r.expanded) 0f else -90f), tint = base)
            else Spacer(Modifier.width(18.dp))
            Icon(
                if (r.isDir) (if (r.expanded) Ic.FolderOpen else Ic.Folder) else visual!!.icon,
                null, Modifier.padding(horizontal = 8.dp).size(20.dp), tint = iconTint,
            )
            Text(
                f.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium,
                color = if (dim) base.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
            )
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            if (r.isDir) {
                DropdownMenuItem(text = { Text("File baru") }, leadingIcon = { Icon(Ic.NewFile, null) }, onClick = { menu = false; a.onNewFile(f) })
                DropdownMenuItem(text = { Text("Folder baru") }, leadingIcon = { Icon(Ic.NewFolder, null) }, onClick = { menu = false; a.onNewFolder(f) })
            }
            DropdownMenuItem(text = { Text("Ganti nama") }, leadingIcon = { Icon(Ic.Edit, null) }, onClick = { menu = false; a.onRename(f) })
            DropdownMenuItem(text = { Text("Duplikat") }, leadingIcon = { Icon(Ic.Copy, null) }, onClick = { menu = false; a.onDuplicate(f) })
            DropdownMenuItem(text = { Text("Salin path") }, leadingIcon = { Icon(Ic.Code, null) }, onClick = { menu = false; a.onCopyPath(f) })
            DropdownMenuItem(text = { Text("Hapus") }, leadingIcon = { Icon(Ic.Delete, null) }, onClick = { menu = false; a.onDelete(f) })
        }
    }
}
