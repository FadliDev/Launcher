package com.sally.ide.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.UnfoldLess
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * SEMUA ikon app dirujuk lewat objek ini. Kalau compiler bilang sebuah ikon tidak ada di
 * material-icons-extended, cukup ganti satu baris di sini.
 */
object Ic {
    val Add: ImageVector = Icons.Outlined.Add
    val Android: ImageVector = Icons.Outlined.Android
    val Back: ImageVector = Icons.AutoMirrored.Filled.ArrowBack
    val Build: ImageVector = Icons.Outlined.Build
    val Chip: ImageVector = Icons.Outlined.Memory
    val Close: ImageVector = Icons.Outlined.Close
    val CollapseAll: ImageVector = Icons.Outlined.UnfoldLess
    val Code: ImageVector = Icons.Outlined.Code
    val Copy: ImageVector = Icons.Outlined.ContentCopy
    val Delete: ImageVector = Icons.Outlined.Delete
    val Down: ImageVector = Icons.Outlined.KeyboardArrowDown
    val Download: ImageVector = Icons.Outlined.FileDownload
    val Edit: ImageVector = Icons.Outlined.Edit
    val ExpandMore: ImageVector = Icons.Outlined.ExpandMore
    val Extension: ImageVector = Icons.Outlined.Extension
    val File: ImageVector = Icons.Outlined.Description
    val Folder: ImageVector = Icons.Outlined.Folder
    val FolderOpen: ImageVector = Icons.Outlined.FolderOpen
    val Home: ImageVector = Icons.Outlined.Home
    val Image: ImageVector = Icons.Outlined.Image
    val Json: ImageVector = Icons.Outlined.DataObject
    val Menu: ImageVector = Icons.Outlined.Menu
    val More: ImageVector = Icons.Outlined.MoreVert
    val NewFile: ImageVector = Icons.Outlined.NoteAdd
    val NewFolder: ImageVector = Icons.Outlined.CreateNewFolder
    val Redo: ImageVector = Icons.AutoMirrored.Filled.Redo
    val Refresh: ImageVector = Icons.Outlined.Refresh
    val Save: ImageVector = Icons.Outlined.Save
    val Search: ImageVector = Icons.Outlined.Search
    val Settings: ImageVector = Icons.Outlined.Settings
    val Terminal: ImageVector = Icons.Outlined.Terminal
    val Undo: ImageVector = Icons.AutoMirrored.Filled.Undo
    val Up: ImageVector = Icons.Outlined.KeyboardArrowUp
    val Upload: ImageVector = Icons.Outlined.FileUpload
    val Zip: ImageVector = Icons.Outlined.FolderZip
}

/** Ikon + warna aksen per tipe file. [tint] = Unspecified berarti pakai warna default tema. */
class FileVisual(val icon: ImageVector, val tint: Color = Color.Unspecified) {
    fun tintOr(default: Color): Color = if (tint == Color.Unspecified) default else tint
}

fun fileVisual(name: String): FileVisual {
    val n = name.lowercase()
    return when {
        n.endsWith(".kt") -> FileVisual(Ic.Code, Color(0xFF9B7BEA))
        n.endsWith(".kts") || n.endsWith(".gradle") -> FileVisual(Ic.Build, Color(0xFF3FB6A8))
        n.endsWith(".java") -> FileVisual(Ic.Code, Color(0xFFE8833A))
        n.endsWith(".c") || n.endsWith(".cc") || n.endsWith(".cpp") || n.endsWith(".cxx") ||
            n.endsWith(".h") || n.endsWith(".hpp") -> FileVisual(Ic.Code, Color(0xFF4C8DDB))
        n.endsWith(".xml") || n.endsWith(".html") -> FileVisual(Ic.Code, Color(0xFF5BAE5B))
        n.endsWith(".json") || n.endsWith(".mcmeta") -> FileVisual(Ic.Json, Color(0xFFD4B13A))
        n.endsWith(".toml") || n.endsWith(".properties") || n.endsWith(".yml") || n.endsWith(".yaml") ->
            FileVisual(Ic.Settings, Color(0xFF8A94A6))
        n.endsWith(".md") || n.endsWith(".markdown") -> FileVisual(Ic.File, Color(0xFF5C9DD6))
        n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".webp") || n.endsWith(".gif") ->
            FileVisual(Ic.Image, Color(0xFFD16B9A))
        n.endsWith(".zip") || n.endsWith(".jar") || n.endsWith(".aar") -> FileVisual(Ic.Zip, Color(0xFFB08968))
        n.endsWith(".apk") -> FileVisual(Ic.Android, Color(0xFF3DDC84))
        n.endsWith(".so") || n.endsWith(".dll") || n.endsWith(".bin") -> FileVisual(Ic.Chip)
        n.endsWith(".sh") -> FileVisual(Ic.Terminal, Color(0xFF8A94A6))
        else -> FileVisual(Ic.File)
    }
}
