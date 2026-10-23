package net.kdt.pojavlaunch.utils;

import android.content.Context;

/**
 * Shim JNI. Nama class dan signature di sini harus sama dengan fungsi
 * Java_net_kdt_pojavlaunch_utils_JREUtils_* di jni/*.c.
 * Implementasi lengkapnya ada di ZalithLauncher (GPL-3.0).
 */
public final class JREUtils {
    private JREUtils() {}

    public static native boolean dlopen(String libPath);
    public static native void setLdLibraryPath(String ldLibraryPath);
    public static native int chdir(String path);
    public static native void initializeGameExitHook();
    public static native void setupExitMethod(Context context);
}
