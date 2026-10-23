package com.sally.freshlauncher.core

import java.util.UUID

data class OfflineAccount(val name: String) {
    /** UUID offline standar Minecraft: nameUUIDFromBytes("OfflinePlayer:" + nama). */
    val uuid: String
        get() = UUID.nameUUIDFromBytes("OfflinePlayer:$name".toByteArray(Charsets.UTF_8)).toString()

    companion object {
        private val VALID = Regex("^[A-Za-z0-9_]{3,16}$")
        fun isValid(name: String): Boolean = VALID.matches(name)
    }
}
