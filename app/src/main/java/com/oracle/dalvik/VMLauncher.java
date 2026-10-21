package com.oracle.dalvik;

/** Entry point into the native JLI launcher (jre_launcher.c). */
public final class VMLauncher {
    public static native int launchJVM(String[] args);
}
