package com.sally.ide.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sally.ide.IdeViewModel
import java.io.File

/** Dua layar saja: Beranda (daftar proyek) dan Editor (proyek terbuka). */
@Composable
fun SallyRoot(vm: IdeViewModel) {
    val snackbar = remember { SnackbarHostState() }
    vm.message?.let { msg -> LaunchedEffect(msg) { snackbar.showSnackbar(msg); vm.message = null } }
    val p = vm.project
    if (p == null) HomeScreen(vm, snackbar) else EditorScreen(vm, p, snackbar)
}

/** Aksi impor/ekspor zip lewat Storage Access Framework (tanpa izin storage). */
class ZipActions(val onImport: () -> Unit, val onExport: (File) -> Unit)

@Composable
fun rememberZipActions(vm: IdeViewModel): ZipActions {
    var pending by remember { mutableStateOf<File?>(null) }
    val imp = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importZip(uri)
    }
    val exp = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        val d = pending
        pending = null
        if (uri != null && d != null) vm.exportProject(d, uri)
    }
    return remember(imp, exp) {
        ZipActions(
            onImport = { imp.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) },
            onExport = { d -> pending = d; exp.launch(d.name + ".zip") },
        )
    }
}
