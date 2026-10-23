package com.movtery.zalithlauncher.ui.activity;

import android.content.Context;
import android.util.Log;

/**
 * Dipanggil oleh native exit hook saat game keluar dengan kode error.
 * Nama class dan method ini wajib ada karena jni/stdio_is.c mencarinya lewat JNI.
 */
public final class ErrorActivity {
    private ErrorActivity() {}

    public static void showExitMessage(Context context, int code, boolean isSignal) {
        Log.e("FreshGame", "Game keluar dengan kode " + code + (isSignal ? " (sinyal)" : ""));
    }
}
