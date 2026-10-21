package net.kdt.pojavlaunch.utils;

import android.content.Context;

/** Native hooks implemented in libpojavexec.so. Names/signatures must match the JNI exports. */
public class JREUtils {
    public static native int chdir(String path);
    public static native boolean dlopen(String libPath);
    public static native void setLdLibraryPath(String ldLibraryPath);
    public static native void setupBridgeWindow(Object surface);
    public static native void releaseBridgeWindow();
    public static native void initializeGameExitHook();
    public static native void setupExitMethod(Context context);
}
