package com.sally.freshlauncher.core

import com.oracle.dalvik.VMLauncher

/**
 * Pembungkus engine native ZalithLauncher (libpojavexec.so).
 * Hanya dipanggil dari proses :game.
 *
 * Catatan: pemanggilan ini belum menyiapkan environment JRE (LD_LIBRARY_PATH, renderer,
 * linker namespace). Langkah itu dikerjakan di tahap berikutnya.
 */
object NativeJvm {
    init {
        System.loadLibrary("pojavexec")
    }

    /** Mengembalikan kode keluar JVM, atau nilai negatif jika gagal sebelum JVM berjalan. */
    fun launch(jliPath: String, argv: Array<String>): Int = VMLauncher.launchJVM(argv)
}
