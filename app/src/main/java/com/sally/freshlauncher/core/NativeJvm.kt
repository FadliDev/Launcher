package com.sally.freshlauncher.core

/** Pembungkus JNI ke jre_launcher.c. Hanya dipanggil dari proses :game. */
object NativeJvm {
    init {
        System.loadLibrary("freshjvm")
    }

    /** Mengembalikan kode keluar JVM, atau nilai negatif jika gagal sebelum JVM berjalan. */
    @JvmStatic
    external fun launch(jliPath: String, argv: Array<String>): Int
}
