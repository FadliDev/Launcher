package com.sally.freshlauncher.core

import com.oracle.dalvik.VMLauncher

/** Pembungkus engine native ZalithLauncher (libpojavexec.so). Hanya dipanggil dari proses :game. */
object NativeJvm {
    init {
        System.loadLibrary("pojavexec")
    }

    /** Memicu inisialisasi library native. Dipanggil sebelum fungsi JREUtils apa pun. */
    fun ensureLoaded() {}

    /** Mengembalikan kode keluar JVM, atau nilai negatif jika gagal sebelum JVM berjalan. */
    fun launch(argv: Array<String>): Int = VMLauncher.launchJVM(argv)
}
