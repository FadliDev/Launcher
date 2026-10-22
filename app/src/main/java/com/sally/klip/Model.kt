package com.sally.klip

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class Aspect(val label: String, val w: Int, val h: Int) {
    R9_16("9:16", 9, 16), R16_9("16:9", 16, 9), R1_1("1:1", 1, 1), R4_5("4:5", 4, 5), R4_3("4:3", 4, 3);

    /** Ukuran output dengan sisi terpendek = [short], selalu genap. */
    fun size(short: Int): Pair<Int, Int> {
        val (ow, oh) = if (w < h) short to short * h / w else short * w / h to short
        return (ow / 2 * 2) to (oh / 2 * 2)
    }
}

data class Clip(
    val id: String = UUID.randomUUID().toString(),
    val uri: String,
    val durationMs: Long,
    val inMs: Long = 0,
    val outMs: Long = durationMs,
    val speed: Float = 1f,
    val volume: Float = 1f,
) {
    val lengthMs: Long get() = ((outMs - inMs) / speed).toLong()
}

data class Project(val clips: List<Clip> = emptyList(), val aspect: Aspect = Aspect.R9_16) {
    val totalMs: Long get() = clips.sumOf { it.lengthMs }

    fun toJson(): String = JSONObject().apply {
        put("aspect", aspect.name)
        put("clips", JSONArray().also { arr ->
            clips.forEach {
                arr.put(JSONObject().put("id", it.id).put("uri", it.uri).put("dur", it.durationMs)
                    .put("in", it.inMs).put("out", it.outMs).put("speed", it.speed.toDouble()).put("vol", it.volume.toDouble()))
            }
        })
    }.toString()

    companion object {
        fun fromJson(s: String): Project {
            val o = JSONObject(s)
            val arr = o.getJSONArray("clips")
            val clips = (0 until arr.length()).map {
                val c = arr.getJSONObject(it)
                Clip(c.getString("id"), c.getString("uri"), c.getLong("dur"), c.getLong("in"), c.getLong("out"),
                    c.getDouble("speed").toFloat(), c.getDouble("vol").toFloat())
            }
            return Project(clips, runCatching { Aspect.valueOf(o.getString("aspect")) }.getOrDefault(Aspect.R9_16))
        }
    }
}
