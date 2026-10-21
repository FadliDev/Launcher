package com.sally.ember.game

object JvmBridge {
    init { System.loadLibrary("ember") }

    @JvmStatic
    external fun nativeLaunch(
        jliPath: String, workDir: String, logPath: String,
        ldPath: String, javaHome: String, args: Array<String>
    ): Int
}
