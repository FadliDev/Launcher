package net.kdt.pojavlaunch;

import android.content.ClipboardManager;

/**
 * Not an Activity: the native bridge looks these static members up by this exact class name
 * (clipboard / open-link callbacks used by CallbackBridge and the AWT bridge).
 */
public class MainActivity {
    public static ClipboardManager GLOBAL_CLIPBOARD;

    public static void openLink(String link) { }
    public static void querySystemClipboard() { }
    public static void putClipboardData(String data, String mimeType) { }
}
