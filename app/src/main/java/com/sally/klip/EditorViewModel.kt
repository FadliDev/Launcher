package com.sally.klip

import android.app.Application
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import java.io.File
import java.util.UUID

class EditorViewModel(app: Application) : AndroidViewModel(app) {
    private val saveFile = File(app.filesDir, "project.json")
    private val undo = ArrayDeque<Project>()
    private val redo = ArrayDeque<Project>()

    var project by mutableStateOf(Project()); private set
    var selectedId by mutableStateOf<String?>(null)
    var exportProgress by mutableStateOf<Int?>(null)
    var message by mutableStateOf<String?>(null)

    val selected: Clip? get() = project.clips.firstOrNull { it.id == selectedId }
    val canUndo get() = undo.isNotEmpty()
    val canRedo get() = redo.isNotEmpty()

    init {
        runCatching { if (saveFile.exists()) project = Project.fromJson(saveFile.readText()) }
    }

    /** Semua perubahan lewat sini: simpan snapshot untuk undo (maks 50) + auto-save. */
    private fun commit(new: Project) {
        undo.addLast(project); if (undo.size > 50) undo.removeFirst()
        redo.clear()
        project = new
        runCatching { saveFile.writeText(new.toJson()) }
    }

    fun undo() { if (undo.isEmpty()) return; redo.addLast(project); project = undo.removeLast(); persist() }
    fun redo() { if (redo.isEmpty()) return; undo.addLast(project); project = redo.removeLast(); persist() }
    private fun persist() { runCatching { saveFile.writeText(project.toJson()) } }

    fun newProject() { undo.clear(); redo.clear(); selectedId = null; project = Project(aspect = project.aspect); persist() }

    fun addVideos(uris: List<Uri>) {
        val ctx = getApplication<Application>()
        val added = uris.mapNotNull { u ->
            runCatching {
                val r = MediaMetadataRetriever()
                r.setDataSource(ctx, u)
                val d = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
                r.release()
                if (d > 0) Clip(uri = u.toString(), durationMs = d) else null
            }.getOrNull()
        }
        if (added.isEmpty()) { message = "Video tidak bisa dibaca."; return }
        commit(project.copy(clips = project.clips + added))
        selectedId = added.last().id
    }

    private fun update(id: String, f: (Clip) -> Clip) =
        commit(project.copy(clips = project.clips.map { if (it.id == id) f(it) else it }))

    fun setAspect(a: Aspect) = commit(project.copy(aspect = a))
    fun setSpeed(id: String, s: Float) = update(id) { it.copy(speed = s) }
    fun setVolume(id: String, v: Float) = update(id) { it.copy(volume = v) }
    fun setTrim(id: String, inMs: Long, outMs: Long) = update(id) { it.copy(inMs = inMs, outMs = outMs) }

    fun delete(id: String) {
        commit(project.copy(clips = project.clips.filterNot { it.id == id })); selectedId = null
    }

    fun duplicate(id: String) {
        val i = project.clips.indexOfFirst { it.id == id }.takeIf { it >= 0 } ?: return
        val copy = project.clips[i].copy(id = UUID.randomUUID().toString())
        commit(project.copy(clips = project.clips.toMutableList().apply { add(i + 1, copy) }))
        selectedId = copy.id
    }

    fun move(id: String, dir: Int) {
        val l = project.clips.toMutableList()
        val i = l.indexOfFirst { it.id == id }
        val j = i + dir
        if (i < 0 || j !in l.indices) return
        l[i] = l[j].also { l[j] = l[i] }
        commit(project.copy(clips = l))
    }

    /** [localMs] = posisi dalam sumber relatif ke awal trim (sama dengan player.currentPosition pada item ter-clip). */
    fun split(id: String, localMs: Long) {
        val i = project.clips.indexOfFirst { it.id == id }.takeIf { it >= 0 } ?: return
        val c = project.clips[i]
        val at = c.inMs + localMs
        if (at - c.inMs < 200 || c.outMs - at < 200) { message = "Posisi terlalu dekat ke ujung clip."; return }
        val a = c.copy(outMs = at)
        val b = c.copy(id = UUID.randomUUID().toString(), inMs = at)
        commit(project.copy(clips = project.clips.toMutableList().apply { removeAt(i); add(i, b); add(i, a) }))
        selectedId = b.id
    }
}
