package com.sally.klip

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

private fun fmt(ms: Long): String { val s = ms / 1000; return "%02d:%02d".format(s / 60, s % 60) }

@Composable
fun KlipApp(vm: EditorViewModel) {
    var inEditor by remember { mutableStateOf(false) }
    if (inEditor) { BackHandler { inEditor = false }; EditorScreen(vm) { inEditor = false } }
    else HomeScreen(vm) { inEditor = true }
}

@Composable
fun HomeScreen(vm: EditorViewModel, open: () -> Unit) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        if (uris.isNotEmpty()) { vm.newProject(); vm.addVideos(uris); open() }
    }
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(20.dp)) {
        Text("Klip", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
            modifier = Modifier.fillMaxWidth().height(72.dp), shape = RoundedCornerShape(16.dp)
        ) { Text("＋  Proyek Baru", style = MaterialTheme.typography.titleMedium) }
        Spacer(Modifier.height(16.dp))
        if (vm.project.clips.isNotEmpty()) {
            OutlinedButton(onClick = open, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp)) {
                Text("Lanjutkan proyek terakhir (${vm.project.clips.size} clip · ${fmt(vm.project.totalMs)})")
            }
        } else {
            Text("Belum ada proyek. Yuk bikin video pertamamu.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.weight(1f))
        Text("Gratis. Tanpa watermark. Tanpa langganan.", color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.labelLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(vm: EditorViewModel, close: () -> Unit) {
    val ctx = LocalContext.current
    val project = vm.project
    val exporter = remember { Exporter(ctx) }
    val player = remember { ExoPlayer.Builder(ctx).build() }
    var playing by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    var posIndex by remember { mutableIntStateOf(0) }
    var posMs by remember { mutableLongStateOf(0L) }

    val addPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { vm.addVideos(it) }

    DisposableEffect(Unit) {
        val l = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
            override fun onMediaItemTransition(item: MediaItem?, reason: Int) { applyClipParams(player, vm) }
        }
        player.addListener(l)
        onDispose { player.removeListener(l); player.release(); exporter.cancel() }
    }

    // Bangun ulang playlist tiap project berubah, pertahankan posisi.
    LaunchedEffect(project.clips) {
        val idx = player.currentMediaItemIndex.coerceAtLeast(0)
        val pos = player.currentPosition
        val items = project.clips.map { c ->
            MediaItem.Builder().setUri(Uri.parse(c.uri)).setMediaId(c.id)
                .setClippingConfiguration(MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(c.inMs).setEndPositionMs(c.outMs).build()).build()
        }
        player.setMediaItems(items)
        player.prepare()
        if (items.isNotEmpty()) {
            val i = idx.coerceAtMost(items.size - 1)
            player.seekTo(i, if (i == idx) pos else 0L)
        }
        applyClipParams(player, vm)
    }
    LaunchedEffect(Unit) {
        while (true) { posIndex = player.currentMediaItemIndex; posMs = player.currentPosition; delay(100) }
    }
    LaunchedEffect(vm.message) { vm.message?.let { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show(); vm.message = null } }

    val sel = vm.selected
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        // Top bar
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = close) { Text("✕") }
            Spacer(Modifier.weight(1f))
            Button(onClick = { showExport = true }, enabled = project.clips.isNotEmpty() && vm.exportProgress == null) { Text("Export") }
        }
        // Preview
        Box(Modifier.fillMaxWidth().weight(1f).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
            AndroidView(
                factory = { c -> PlayerView(c).apply { this.player = player; useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT } },
                modifier = Modifier.aspectRatio(project.aspect.w.toFloat() / project.aspect.h).fillMaxHeight()
            )
        }
        // Kontrol putar
        val before = project.clips.take(posIndex).sumOf { it.lengthMs }
        val cur = project.clips.getOrNull(posIndex)
        val global = before + ((posMs) / (cur?.speed ?: 1f)).toLong()
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (playing) player.pause() else player.play() }) {
                Text(if (playing) "⏸" else "▶", style = MaterialTheme.typography.titleLarge)
            }
            Text("${fmt(global)} / ${fmt(project.totalMs)}", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = vm::undo, enabled = vm.canUndo) { Text("↶") }
            TextButton(onClick = vm::redo, enabled = vm.canRedo) { Text("↷") }
        }
        if (project.totalMs > 0) {
            Slider(
                value = global.toFloat().coerceIn(0f, project.totalMs.toFloat()),
                onValueChange = { g ->
                    var acc = 0L
                    for ((i, c) in project.clips.withIndex()) {
                        if (g < acc + c.lengthMs || i == project.clips.lastIndex) {
                            player.seekTo(i, ((g - acc) * c.speed).toLong().coerceAtLeast(0)); break
                        }
                        acc += c.lengthMs
                    }
                },
                valueRange = 0f..project.totalMs.toFloat(), modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
        // Timeline
        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).horizontalScroll(rememberScrollState()).padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            project.clips.forEachIndexed { i, c ->
                val selected = c.id == vm.selectedId
                Box(
                    Modifier.width((c.lengthMs / 1000f * 24).dp.coerceAtLeast(56.dp)).height(56.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .border(if (selected) 2.dp else 1.dp,
                            if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .clickable { vm.selectedId = c.id; player.seekTo(i, 0) }.padding(6.dp)
                ) { Text("${i + 1} · ${fmt(c.lengthMs)}${if (c.speed != 1f) " ·${c.speed}x" else ""}", style = MaterialTheme.typography.labelSmall) }
            }
            FilledTonalButton(onClick = { addPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
                modifier = Modifier.height(56.dp)) { Text("＋") }
        }
        // Alat
        Column(Modifier.fillMaxWidth().heightIn(max = 230.dp).verticalScrollCompat().padding(horizontal = 8.dp)) {
            if (sel == null) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Aspect.values().forEach { a ->
                        FilterChip(selected = project.aspect == a, onClick = { vm.setAspect(a) }, label = { Text(a.label) })
                    }
                }
                Text("Pilih clip di timeline untuk mengedit.", color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
            } else {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AssistChip(onClick = { vm.split(sel.id, player.currentPosition) }, label = { Text("Split") })
                    AssistChip(onClick = { vm.duplicate(sel.id) }, label = { Text("Duplikat") })
                    AssistChip(onClick = { vm.move(sel.id, -1) }, label = { Text("◀ Geser") })
                    AssistChip(onClick = { vm.move(sel.id, 1) }, label = { Text("Geser ▶") })
                    AssistChip(onClick = { vm.delete(sel.id) }, label = { Text("Hapus", color = MaterialTheme.colorScheme.error) })
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Speed", style = MaterialTheme.typography.labelMedium)
                    listOf(0.25f, 0.5f, 1f, 1.5f, 2f, 4f).forEach { s ->
                        FilterChip(selected = sel.speed == s, onClick = { vm.setSpeed(sel.id, s) }, label = { Text("${s}x") })
                    }
                }
                var vol by remember(sel.id, sel.volume) { mutableFloatStateOf(sel.volume) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Volume ${(vol * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(96.dp))
                    Slider(value = vol, onValueChange = { vol = it }, onValueChangeFinished = { vm.setVolume(sel.id, vol) }, valueRange = 0f..2f)
                }
                var trim by remember(sel.id, sel.inMs, sel.outMs) { mutableStateOf(sel.inMs.toFloat()..sel.outMs.toFloat()) }
                Text("Trim ${fmt(trim.start.toLong())} – ${fmt(trim.endInclusive.toLong())}", style = MaterialTheme.typography.labelMedium)
                RangeSlider(value = trim, onValueChange = { trim = it }, valueRange = 0f..sel.durationMs.toFloat(),
                    onValueChangeFinished = {
                        if (trim.endInclusive - trim.start >= 200) vm.setTrim(sel.id, trim.start.toLong(), trim.endInclusive.toLong())
                    })
            }
        }
    }

    if (showExport) ExportSheet(
        onDismiss = { showExport = false },
        onStart = { shortSide ->
            showExport = false; player.pause(); vm.exportProgress = 0
            exporter.export(vm.project, shortSide, { vm.exportProgress = it }) { uri, err ->
                vm.exportProgress = null
                vm.message = if (uri != null) "Tersimpan di Movies/Klip (tanpa watermark)" else err
            }
        })
    vm.exportProgress?.let { p ->
        AlertDialog(onDismissRequest = {}, title = { Text("Mengekspor…") },
            text = { Column { LinearProgressIndicator(progress = { p / 100f }, modifier = Modifier.fillMaxWidth()); Text("$p%") } },
            confirmButton = { TextButton(onClick = { exporter.cancel(); vm.exportProgress = null }) { Text("Batal") } })
    }
}

private fun applyClipParams(player: ExoPlayer, vm: EditorViewModel) {
    val c = vm.project.clips.getOrNull(player.currentMediaItemIndex) ?: return
    player.playbackParameters = PlaybackParameters(c.speed)
    player.volume = c.volume.coerceAtMost(1f)
}

@Composable
private fun Modifier.verticalScrollCompat(): Modifier =
    this.then(Modifier.verticalScroll(rememberScrollState()))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportSheet(onDismiss: () -> Unit, onStart: (Int) -> Unit) {
    var res by remember { mutableIntStateOf(1080) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(20.dp).navigationBarsPadding()) {
            Text("Export", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(720 to "720p", 1080 to "1080p", 2160 to "4K").forEach { (v, l) ->
                    FilterChip(selected = res == v, onClick = { res = v }, label = { Text(l) })
                }
            }
            Text("Tanpa watermark", color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(16.dp))
            Button(onClick = { onStart(res) }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Export") }
        }
    }
}
