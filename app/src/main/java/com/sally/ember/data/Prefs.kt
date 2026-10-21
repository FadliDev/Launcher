package com.sally.ember.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("ember", Context.MODE_PRIVATE)

    var ramMb: Int
        get() = sp.getInt("ram", 1536)
        set(v) = sp.edit().putInt("ram", v).apply()
    var extraJvm: String
        get() = sp.getString("jvm", "") ?: ""
        set(v) = sp.edit().putString("jvm", v).apply()
    /** Renderer id; null means "never chosen" -> launcher default (Krypton Wrapper). */
    var renderer: String?
        get() = sp.getString("renderer", null)
        set(v) = sp.edit().putString("renderer", v).apply()
    var selectedVersion: String?
        get() = sp.getString("ver", null)
        set(v) = sp.edit().putString("ver", v).apply()
    var selectedAccount: String?
        get() = sp.getString("acc", null)
        set(v) = sp.edit().putString("acc", v).apply()

    fun accounts(): List<Account> {
        val arr = JSONArray(sp.getString("accounts", "[]"))
        return (0 until arr.length()).map { arr.getJSONObject(it).let { o -> Account(o.getString("name"), o.getString("uuid")) } }
    }

    private fun save(list: List<Account>) {
        val arr = JSONArray(); list.forEach { arr.put(JSONObject().put("name", it.name).put("uuid", it.uuid)) }
        sp.edit().putString("accounts", arr.toString()).apply()
    }

    fun addOffline(name: String): Account {
        val acc = Account(name, offlineUuid(name))
        save(accounts().filter { it.name != name } + acc)
        selectedAccount = acc.uuid
        return acc
    }

    fun removeAccount(uuid: String) {
        save(accounts().filter { it.uuid != uuid })
        if (selectedAccount == uuid) selectedAccount = accounts().firstOrNull()?.uuid
    }

    companion object {
        // Same scheme the vanilla server uses for offline players.
        fun offlineUuid(name: String): String =
            UUID.nameUUIDFromBytes("OfflinePlayer:$name".toByteArray()).toString()
    }
}
