package com.sally.klip

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.ChannelMixingAudioProcessor
import androidx.media3.common.audio.ChannelMixingMatrix
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.effect.SpeedChangeEffect
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import java.io.File

/** Export lewat Media3 Transformer (hardware encoder). Tanpa watermark. Panggil dari main thread. */
@OptIn(UnstableApi::class)
class Exporter(private val ctx: Context) {
    private var transformer: Transformer? = null
    private val handler = Handler(Looper.getMainLooper())

    fun cancel() { transformer?.cancel(); transformer = null }

    fun export(project: Project, shortSide: Int, onProgress: (Int) -> Unit, onDone: (Uri?, String?) -> Unit) {
        if (project.clips.isEmpty()) { onDone(null, "Belum ada clip."); return }
        val (w, h) = project.aspect.size(shortSide)
        val items = project.clips.map { c ->
            val mi = MediaItem.Builder().setUri(c.uri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder().setStartPositionMs(c.inMs).setEndPositionMs(c.outMs).build())
                .build()
            val audio = mutableListOf<AudioProcessor>()
            val video = mutableListOf<androidx.media3.common.Effect>()
            if (c.speed != 1f) {
                audio += SonicAudioProcessor().apply { setSpeed(c.speed); setPitch(1f) }
                video += SpeedChangeEffect(c.speed)
            }
            if (c.volume != 1f) {
                audio += ChannelMixingAudioProcessor().apply {
                    for (i in 1..2) for (o in 1..2) putChannelMixingMatrix(ChannelMixingMatrix.create(i, o).scaleBy(c.volume))
                }
            }
            video += Presentation.createForWidthAndHeight(w, h, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP)
            EditedMediaItem.Builder(mi).setEffects(Effects(audio, video)).build()
        }
        val composition = Composition.Builder(EditedMediaItemSequence(items)).build()
        val out = File(ctx.cacheDir, "klip_${System.currentTimeMillis()}.mp4")

        val t = Transformer.Builder(ctx)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    transformer = null
                    onDone(saveToGallery(out), null)
                }
                override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                    transformer = null
                    onDone(null, "Export gagal: ${exportException.errorCodeName}")
                }
            }).build()
        transformer = t
        t.start(composition, out.absolutePath)

        val holder = ProgressHolder()
        val poll = object : Runnable {
            override fun run() {
                val tr = transformer ?: return
                if (tr.getProgress(holder) != Transformer.PROGRESS_STATE_NOT_STARTED) onProgress(holder.progress)
                handler.postDelayed(this, 300)
            }
        }
        handler.post(poll)
    }

    private fun saveToGallery(src: File): Uri? = runCatching {
        val name = "Klip_${System.currentTimeMillis()}.mp4"
        if (Build.VERSION.SDK_INT >= 29) {
            val cv = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/Klip")
            }
            val uri = ctx.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cv)!!
            ctx.contentResolver.openOutputStream(uri)!!.use { o -> src.inputStream().use { it.copyTo(o) } }
            src.delete(); uri
        } else {
            val dir = ctx.getExternalFilesDir(Environment.DIRECTORY_MOVIES)!!.apply { mkdirs() }
            val dst = File(dir, name); src.copyTo(dst, true); src.delete(); Uri.fromFile(dst)
        }
    }.getOrNull()
}
